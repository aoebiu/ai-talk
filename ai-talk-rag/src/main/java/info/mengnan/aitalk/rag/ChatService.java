package info.mengnan.aitalk.rag;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.handler.StreamingResponseHandler;
import info.mengnan.aitalk.rag.container.assemble.AssembledModels;
import info.mengnan.aitalk.rag.container.assemble.DynamicEmbeddingStoreRegistry;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.rag.service.ModelConfigProvider;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.rag.query.router.LanguageModelQueryRouter.FallbackStrategy.DO_NOT_ROUTE;
import static info.mengnan.aitalk.common.param.ModelType.*;
import static info.mengnan.aitalk.rag.constant.promptTemplate.PromptTemplateConstant.*;


@Slf4j
public class ChatService {
    private final ChatMemoryStore chatMemoryStore;
    private final ModelRegistry modelRegistry;
    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final ModelConfigProvider modelConfigProvider;

    public ChatService(ChatMemoryStore chatMemoryStore,
                       ModelRegistry modelRegistry,
                       DynamicEmbeddingStoreRegistry embeddingStoreRegistry,
                       ModelConfigProvider modelConfigProvider) {
        this.chatMemoryStore = chatMemoryStore;
        this.modelRegistry = modelRegistry;
        this.embeddingStoreRegistry = embeddingStoreRegistry;
        this.modelConfigProvider = modelConfigProvider;
    }

    private final static ThreadPoolExecutor POOL_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2, 2,
            TimeUnit.MINUTES, new LinkedBlockingDeque<>(256),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 流式RAG对话 - 使用回调处理器
     *
     * @param sessionId       会话id
     * @param message         消息
     * @param handler         流式响应处理器
     * @param assembledModels 已组装好的模型配置
     * @param toolMap         工具map
     *
     */
    public void chatStreaming(Long memberId,
                              String sessionId,
                              String message,
                              StreamingResponseHandler handler,
                              AssembledModels assembledModels,
                              Map<ToolSpecification, ToolExecutor> toolMap) {
        if (assembledModels == null) {
            handler.onError(new IllegalArgumentException("AssembledModels cannot be null"));
            return;
        }

        AssistantUnique assistantUnique = buildAssistantUnique(memberId,assembledModels,toolMap);

        try {
            TokenStream tokenStream = assistantUnique.chatStreaming(sessionId, message);

            tokenStream.onPartialResponse(token -> {
                        if (!handler.isCancelled()) {
                            handler.onToken(token);
                        }
                    })
                    .onCompleteResponse(response -> {
                        String completeText = response.aiMessage().text();
                        if (!handler.isCancelled()) {
                            handler.onComplete(completeText);
                        }
                    })
                    .onError(error -> {
                        if (!handler.isCancelled()) {
                            handler.onError(error);
                        }
                    })
                    .start();
        } catch (Exception e) {
            handler.onError(e);
        }
    }

    /**
     * 根据配置动态构建 AssistantUnique
     */
    private AssistantUnique buildAssistantUnique(Long memberId, AssembledModels assembledModels, Map<ToolSpecification, ToolExecutor> toolMap) {
        AiServices<AssistantUnique> builder = AiServices.builder(AssistantUnique.class);

        if (assembledModels.rag()) {
            DefaultRetrievalAugmentor.DefaultRetrievalAugmentorBuilder ragBuilder = DefaultRetrievalAugmentor.builder();
            ragBuilder.executor(POOL_EXECUTOR);

            if (assembledModels.contentAggregator()) {
                ContentAggregator contentAggregator = null;

                if (assembledModels.scoringModel() != null) {
                    ModelConfig scoringConfig = modelConfigProvider.findModel(
                            memberId,
                            assembledModels.scoringModel().getModelName(),
                            SCORING);

                    if (scoringConfig != null) {
                        ScoringModel scoringModel = modelRegistry.createScoringModel(scoringConfig);
                        contentAggregator = ReRankingContentAggregator.builder()
                                .scoringModel(scoringModel)
                                .querySelector(queryToContents -> queryToContents.entrySet().iterator().next().getKey())
                                .build();
                    }
                } else {
                    contentAggregator = new DefaultContentAggregator();
                }
                ragBuilder.contentAggregator(contentAggregator);
            }

            if (assembledModels.transform() != null) {
                ragBuilder.queryTransformer(new DefaultQueryTransformer());
            }

            // 动态创建 EmbeddingStore 和 ContentRetriever
            List<String> indexNames = embeddingStoreRegistry.queryAllIndexNames();
            Map<ContentRetriever, String> contentRetrieverMap = buildContentRetrieverMap(
                    memberId,
                    indexNames,
                    assembledModels);

            // 配置创建合适的 QueryRouter
            QueryRouter queryRouter;
            if (contentRetrieverMap.isEmpty()) {
                queryRouter = new DefaultQueryRouter();
            } else if (assembledModels.chatModel() == null) {
                queryRouter = new DefaultQueryRouter(contentRetrieverMap.keySet());
            } else {
                ModelConfig chatConfig = modelConfigProvider.findModel(
                        memberId,
                        assembledModels.chatModel().getModelName(),
                        CHAT);

                if (chatConfig != null) {
                    ChatModel chatModel = modelRegistry.createChatModel(chatConfig);
                    queryRouter = new LanguageModelQueryRouter(chatModel, contentRetrieverMap,
                            QUERY_ROUTER_PROMPT_TEMPLATE, DO_NOT_ROUTE);
                } else {
                    queryRouter = new DefaultQueryRouter(contentRetrieverMap.keySet());
                }
            }

            ragBuilder.queryRouter(queryRouter);
            builder.retrievalAugmentor(ragBuilder.build());
        }

        if (assembledModels.streamingChatModel() != null) {
            ModelConfig streamingChatConfig = modelConfigProvider.findModel(
                    memberId,
                    assembledModels.streamingChatModel().getModelName(),
                    STREAMING_CHAT);

            if (streamingChatConfig != null) {
                StreamingChatModel streamingChatModel = modelRegistry.createStreamingChatModel(streamingChatConfig);
                builder.streamingChatModel(streamingChatModel);
            }
        }

        if (assembledModels.chatModel() != null) {
            ModelConfig chatConfig = modelConfigProvider.findModel(
                    memberId,
                    assembledModels.chatModel().getModelName(),
                    CHAT);

            if (chatConfig != null) {
                ChatModel chatModel = modelRegistry.createChatModel(chatConfig);
                builder.chatModel(chatModel);
            }
        }

        if (assembledModels.moderateModel() != null) {
            ModelConfig moderateConfig = modelConfigProvider.findModel(
                    memberId,
                    assembledModels.moderateModel().getModelName(),
                    MODERATE);

            if (moderateConfig != null) {
                ModerationModel moderationModel = modelRegistry.createModerationModel(moderateConfig);
                builder.moderationModel(moderationModel);
            } else {
                builder.moderationModel(new DisabledModerationModel());
            }
        } else {
            builder.moderationModel(new DisabledModerationModel());
        }

        return builder
                .tools(toolMap)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(assembledModels.maxMessages())
                        .chatMemoryStore(assembledModels.inDB() ? chatMemoryStore : new ThreadLocalChatMemoryStore())
                        .build())
                .build();
    }


    /**
     * 构建 ContentRetriever Map
     * 为每个 ES 索引动态创建 EmbeddingStore 和 ContentRetriever
     */
    public Map<ContentRetriever, String> buildContentRetrieverMap(Long memberId,
                                                                  List<String> indexNames,
                                                                  AssembledModels assembledModels) {
        Map<ContentRetriever, String> map = new HashMap<>();

        // 从数据库查询 EmbeddingModel 配置
        if (assembledModels.embeddingModel() == null) {
            log.warn("No embedding model configured");
            return map;
        }

        ModelConfig embeddingConfig = modelConfigProvider.findModel(
                memberId,
                assembledModels.embeddingModel().getModelName(),
                EMBEDDING);

        if (embeddingConfig == null) {
            log.warn("Embedding model config not found in database: {}",
                    assembledModels.embeddingModel().getModelName());
            return map;
        }

        // 动态创建 EmbeddingModel
        EmbeddingModel embeddingModel = modelRegistry.createEmbeddingModel(embeddingConfig);

        // 为每个索引创建 EmbeddingStore 和 ContentRetriever
        for (String indexName : indexNames) {
            try {
                // 动态创建 EmbeddingStore
                EmbeddingStore<TextSegment> embeddingStore = embeddingStoreRegistry.createEmbeddingStore(indexName);

                // 创建 ContentRetriever
                ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .maxResults(assembledModels.maxResults())
                        .minScore(assembledModels.minScore())
                        .build();

                map.put(contentRetriever, indexName);
            } catch (Exception e) {
                log.error("Failed to create ContentRetriever for index: {}", indexName, e);
            }
        }

        return map;
    }

    /**
     * 使用第三方客户端没有必要存储在内存中,但是langchain4j又需要通过ChatMemoryStore获取数据,所以可以通过ThreadLocal实现
     */
    private static class ThreadLocalChatMemoryStore implements ChatMemoryStore {

        private static final ThreadLocal<Map<Object, List<ChatMessage>>> THREAD_LOCAL_MEMORY =
            ThreadLocal.withInitial(HashMap::new);

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return THREAD_LOCAL_MEMORY.get().getOrDefault(memoryId, List.of());
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            THREAD_LOCAL_MEMORY.get().put(memoryId, messages);
        }

        @Override
        public void deleteMessages(Object memoryId) {
            THREAD_LOCAL_MEMORY.get().remove(memoryId);
            if (THREAD_LOCAL_MEMORY.get().isEmpty()) {
                THREAD_LOCAL_MEMORY.remove();
            }
        }
    }
}
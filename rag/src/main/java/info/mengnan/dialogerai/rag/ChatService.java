package info.mengnan.dialogerai.rag;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import info.mengnan.dialogerai.rag.injector.CapturingContentInjector;
import info.mengnan.dialogerai.rag.injector.RagSourceStore;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.dialogerai.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver;
import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver.KbIndexRef;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.handler.StreamingResponseHandler;
import info.mengnan.dialogerai.rag.container.assemble.AssembledModels;
import info.mengnan.dialogerai.rag.container.factory.UniversalModelFactory;
import info.mengnan.dialogerai.rag.service.ModelConfigProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.rag.query.router.LanguageModelQueryRouter.FallbackStrategy.DO_NOT_ROUTE;
import static info.mengnan.dialogerai.common.param.ModelType.*;
import static info.mengnan.dialogerai.rag.constant.promptTemplate.PromptTemplateConstant.*;


@Slf4j
public class ChatService {
    private final ChatMemoryStore chatMemoryStore;
    private final UniversalModelFactory modelFactory;
    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final ModelConfigProvider modelConfigProvider;
    private final KnowledgeBaseIndexResolver knowledgeBaseIndexResolver;
    private final RagSourceStore ragSourceStore;

    public ChatService(ChatMemoryStore chatMemoryStore,
                       UniversalModelFactory modelFactory,
                       DynamicEmbeddingStoreRegistry embeddingStoreRegistry,
                       ModelConfigProvider modelConfigProvider,
                       KnowledgeBaseIndexResolver knowledgeBaseIndexResolver,
                       RagSourceStore ragSourceStore) {
        this.chatMemoryStore = chatMemoryStore;
        this.modelFactory = modelFactory;
        this.embeddingStoreRegistry = embeddingStoreRegistry;
        this.modelConfigProvider = modelConfigProvider;
        this.knowledgeBaseIndexResolver = knowledgeBaseIndexResolver;
        this.ragSourceStore = ragSourceStore;
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
     * @param kbIndexRefs     知识库索引list
     */
    public void chatStreaming(Long memberId,
                              String sessionId,
                              String message,
                              StreamingResponseHandler handler,
                              AssembledModels assembledModels,
                              Map<ToolSpecification, ToolExecutor> toolMap,
                              List<KnowledgeBaseIndexResolver.KbIndexRef> kbIndexRefs) {
        if (assembledModels == null) {
            handler.onError(new IllegalArgumentException("AssembledModels cannot be null"));
            return;
        }

        AssistantUnique assistantUnique = buildAssistantUnique(memberId, sessionId, assembledModels, toolMap, kbIndexRefs);

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
    private AssistantUnique buildAssistantUnique(Long memberId, String sessionId, AssembledModels assembledModels,
                                                 Map<ToolSpecification, ToolExecutor> toolMap,
                                                 List<KnowledgeBaseIndexResolver.KbIndexRef> kbIndexRefs) {
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
                        ScoringModel scoringModel = modelFactory.createScoringModel(scoringConfig);
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

            // 配置创建合适的 QueryRouter
            Map<ContentRetriever, String> contentRetrieverMap = buildContentRetrieverMap(memberId, kbIndexRefs, assembledModels);
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
                    ChatModel chatModel = modelFactory.createChatModel(chatConfig);
                    queryRouter = new LanguageModelQueryRouter(chatModel, contentRetrieverMap,
                            QUERY_ROUTER_PROMPT_TEMPLATE, DO_NOT_ROUTE);
                } else {
                    queryRouter = new DefaultQueryRouter(contentRetrieverMap.keySet());
                }
            }

            ragBuilder.queryRouter(queryRouter);
            ragBuilder.contentInjector(new CapturingContentInjector(sessionId, ragSourceStore));
            builder.retrievalAugmentor(ragBuilder.build());
        }

        if (assembledModels.streamingChatModel() != null) {
            ModelConfig streamingChatConfig = modelConfigProvider.findModel(
                    memberId,
                    assembledModels.streamingChatModel().getModelName(),
                    STREAMING_CHAT);

            if (streamingChatConfig != null) {
                StreamingChatModel streamingChatModel = modelFactory.createStreamingChatModel(streamingChatConfig);
                builder.streamingChatModel(streamingChatModel);
            }
        }

        if (assembledModels.chatModel() != null) {
            ModelConfig chatConfig = modelConfigProvider.findModel(
                    memberId,
                    assembledModels.chatModel().getModelName(),
                    CHAT);

            if (chatConfig != null) {
                ChatModel chatModel = modelFactory.createChatModel(chatConfig);
                builder.chatModel(chatModel);
            }
        }

        if (assembledModels.moderateModel() != null) {
            ModelConfig moderateConfig = modelConfigProvider.findModel(
                    memberId,
                    assembledModels.moderateModel().getModelName(),
                    MODERATE);

            if (moderateConfig != null) {
                ModerationModel moderationModel = modelFactory.createModerationModel(moderateConfig);
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
                        .chatMemoryStore(assembledModels.inDB() ? chatMemoryStore : null)
                        .build())
                .build();
    }

    /**
     * 构建 ContentRetriever Map
     * 为每个 ES 索引动态创建 EmbeddingStore 和 ContentRetriever
     */
    public Map<ContentRetriever, String> buildContentRetrieverMap(Long memberId,
                                                                  List<KbIndexRef> kbIndexes,
                                                                  AssembledModels assembledModels) {
        Map<ContentRetriever, String> map = new HashMap<>();

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

        EmbeddingModel embeddingModel = modelFactory.createEmbeddingModel(embeddingConfig);

        for (KbIndexRef kbIndex : kbIndexes) {
            String indexName = kbIndex.indexName();
            String kbName = kbIndex.displayName();
            try {
                EmbeddingStore<TextSegment> embeddingStore = embeddingStoreRegistry.createEmbeddingStore(indexName);

                ContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .maxResults(assembledModels.maxResults())
                        .minScore(assembledModels.minScore())
                        .build();

                ContentRetriever enrichedRetriever = query -> {
                    List<Content> contents = baseRetriever.retrieve(query);
                    contents.forEach(c -> {
                        c.textSegment().metadata().put("indexName", indexName);
                        c.textSegment().metadata().put("kbName", kbName);
                    });
                    return contents;
                };

                map.put(enrichedRetriever, kbName);
            } catch (Exception e) {
                log.error("Failed to create ContentRetriever for kb: {}", kbName, e);
            }
        }
        return map;
    }

}
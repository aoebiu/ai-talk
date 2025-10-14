package info.mengnan.aitalk.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
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
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import info.mengnan.aitalk.rag.container.RagContainer;
import info.mengnan.aitalk.rag.handler.StreamingResponseHandler;
import info.mengnan.aitalk.rag.container.AssembledModels;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry.*;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ChatService {
    private final ChatMemoryStore chatMemoryStore;
    private final RagContainer ragContainer;

    public ChatService(ChatMemoryStore chatMemoryStore,
                      RagContainer ragContainer) {
        this.chatMemoryStore = chatMemoryStore;
        this.ragContainer = ragContainer;
    }

    private final static ThreadPoolExecutor POOL_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2, 2,
            TimeUnit.MINUTES, new LinkedBlockingDeque<>(256),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 流式RAG对话 - 使用回调处理器
     * @param sessionId 会话id
     * @param message 消息
     * @param handler 流式响应处理器
     * @param assembledModels 已组装好的模型配置
     */
    public void chatStreaming(String sessionId,
                              String message,
                              StreamingResponseHandler handler,
                              AssembledModels assembledModels) {
        if (assembledModels == null) {
            handler.onError(new IllegalArgumentException("AssembledModels cannot be null"));
            return;
        }

        AssistantUnique assistantUnique = buildAssistantUnique(assembledModels);

        try {
            TokenStream tokenStream = assistantUnique.chatStreaming(sessionId, message);

            tokenStream.onPartialResponse(token -> {
                        if (!handler.isCancelled()) {
                            handler.onToken(token);
                        }
                    })
                    .onCompleteResponse(response -> {
                        String completeText = response.aiMessage().text();
                        log.info("流式响应完成,sessionId: {}, 响应长度: {}", sessionId, completeText.length());
                        if (!handler.isCancelled()) {
                            handler.onComplete(completeText);
                        }
                    })
                    .onError(error -> {
                        log.error("生成响应失败,sessionId: {}", sessionId, error);
                        if (!handler.isCancelled()) {
                            handler.onError(error);
                        }
                    })
                    .start();
        } catch (Exception e) {
            log.error("启动流式响应失败,sessionId: {}", sessionId, e);
            handler.onError(e);
        }
    }

    /**
     * 根据配置动态构建 AssistantUnique
     */
    private AssistantUnique buildAssistantUnique(AssembledModels assembledModels) {
        AiServices<AssistantUnique> builder = AiServices.builder(AssistantUnique.class);

        if (assembledModels.rag()) {
            DefaultRetrievalAugmentor.DefaultRetrievalAugmentorBuilder ragBuilder = DefaultRetrievalAugmentor.builder();
            ragBuilder.executor(POOL_EXECUTOR);

            if (assembledModels.contentAggregator()) {
                ContentAggregator contentAggregator = null;

                if (assembledModels.scoringModel() != null) {
                    ScoringModel scoringModel = ragContainer.getScoringModel(assembledModels.scoringModel().getModelName());
                    if (scoringModel != null) {
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
                // QueryTransformer queryTransformer = queryTransformerMap.getOrDefault(assembledModels.transform(), new DefaultQueryTransformer());
                ragBuilder.queryTransformer(new DefaultQueryTransformer());
            }


            Map<String, EmbeddingStore<TextSegment>> embeddingStore = ragContainer.getAllEmbeddingStore();
            // TODO 通过es的多个indexName 匹配多个ContentRetriever 和对应的 名称
            // TODO 最后再通过模型
            // TODO 或者反过来
            Map<ContentRetriever, String> contentRetrieverMap = buildContentRetrieverMap(embeddingStore,
                    assembledModels,
                    ragContainer.getEmbeddingModel(assembledModels.embeddingModel().getModelName()));
            QueryRouter queryRouter;
            if (assembledModels.chatModel() != null) {
                ChatModel chatModel = ragContainer.getChatModel(assembledModels.chatModel().getModelName());
                queryRouter = new LanguageModelQueryRouter(chatModel, contentRetrieverMap);
            } else {
                queryRouter = new DefaultQueryRouter(contentRetrieverMap.keySet());
            }
            ragBuilder.queryRouter(queryRouter);


//             if (assembledModels.contentInjectorPrompt() != null) {
//                 new DefaultContentInjector(assembledModels.contentInjectorPrompt())
//                 ragBuilder.contentInjector(contentInjector);
//             }
            builder.retrievalAugmentor(ragBuilder.build());
        }

        if (assembledModels.streamingChatModel() != null) {
            StreamingChatModel streamingChatModel = ragContainer.getStreamingChatModel(assembledModels.streamingChatModel().getModelName());
            builder.streamingChatModel(streamingChatModel);
        }

        if (assembledModels.chatModel() != null) {
            ChatModel chatModel = ragContainer.getChatModel(assembledModels.chatModel().getModelName());
            builder.chatModel(chatModel);
        }

        return builder
                // .tools()
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(assembledModels.maxMessages())
                        .chatMemoryStore(assembledModels.inDB() ? chatMemoryStore : new InMemoryChatMemoryStore())
                        .build())
                .build();
    }


    public Map<ContentRetriever, String> buildContentRetrieverMap(Map<String, EmbeddingStore<TextSegment>> embeddingStore,
                                                                  AssembledModels assembledModels,
                                                                  EmbeddingModel embeddingModel) {
        Map<ContentRetriever, String> map = new HashMap<>();

        for (Map.Entry<String, EmbeddingStore<TextSegment>> e : embeddingStore.entrySet()) {
            ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(e.getValue())
                    .embeddingModel(embeddingModel)
                    .maxResults(assembledModels.maxResults())
                    .minScore(assembledModels.minScore())
                    .build();

            map.put(contentRetriever, e.getKey());
        }

        return map;
    }
}
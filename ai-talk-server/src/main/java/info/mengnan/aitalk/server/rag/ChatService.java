package info.mengnan.aitalk.server.rag;

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
import info.mengnan.aitalk.server.common.ChatRequest;
import info.mengnan.aitalk.server.rag.container.RagContainer;
import info.mengnan.aitalk.repository.entity.ChatOption;
import info.mengnan.aitalk.server.rag.container.AssembledModels;
import info.mengnan.aitalk.server.rag.container.assemble.AssembledModelsConstruct;
import info.mengnan.aitalk.server.rag.container.assemble.ModelRegistry.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMemoryStore chatMemoryStore;

    private final AssembledModelsConstruct assembledChatModelsService;
    private final RagContainer ragContainer;

    private final static ThreadPoolExecutor POOL_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2, 2,
            TimeUnit.MINUTES, new LinkedBlockingDeque<>(256),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 流式RAG对话 - 使用Flux返回响应流
     */
    public Flux<String> chatStreaming(ChatRequest chatRequest) {
        Long optionId = chatRequest.getOptionId();
        ChatOption chatOption = assembledChatModelsService.findChatOption(optionId);
        if (chatOption == null || !chatOption.getEnabled()) {
            return Flux.error(new IllegalArgumentException("找不到对应的聊天配置，optionId: " + optionId));
        }

        AssembledModels assembledModels = assembledChatModelsService.assemble(chatOption);
        AssistantUnique assistantUnique = buildAssistantUnique(assembledModels);

        String sessionId = chatRequest.getSessionId();

        return Flux.create(sink -> {

            TokenStream tokenStream = assistantUnique.chatStreaming(sessionId, chatRequest.getMessage());

            tokenStream.onPartialResponse(token -> {
                        if (!sink.isCancelled()) {
                            sink.next(token);
                        }
                    })
                    .onCompleteResponse(response -> {
                        log.info("流式响应完成,sessionId: {}, 完整响应: {}", sessionId, response);
                        if (!sink.isCancelled()) {
                            sink.complete();
                        }
                    })
                    .onError(error -> {
                        log.error("生成响应失败", error);
                        if (!sink.isCancelled()) {
                            sink.error(error);
                        }
                    })
                    .start();
        });
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
                        .chatMemoryStore(chatMemoryStore)
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
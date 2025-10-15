package info.mengnan.aitalk.rag.container;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.DisabledStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import info.mengnan.aitalk.common.param.ModelType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG模型容器 - 管理所有RAG相关的模型实例
 * 通过容器的设计思想，提供模型的注册、获取、生命周期管理
 */
@Slf4j
public class RagContainer {

    private final Map<String, QueryTransformer> queryTransformerMap = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatModel> streamingChatModelMap = new ConcurrentHashMap<>();
    private final Map<String, ChatModel> chatModelMap = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModelMap = new ConcurrentHashMap<>();
    private final Map<String, ScoringModel> scoringModelMap = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingStore<TextSegment>> embeddingStoreMap = new ConcurrentHashMap<>();

    // ==================== 注册API ====================

    /**
     * 注册流式聊天模型
     *
     * @param modelName 模型名称
     * @param model     模型实例
     * @return 容器本身，支持链式调用
     */
    public void registerStreamingChatModel(String modelName, StreamingChatModel model) {
        String key = buildKey(ModelType.STREAMING_CHAT, modelName);
        streamingChatModelMap.put(key, model);
    }

    /**
     * 注册聊天模型
     *
     * @param modelName 模型名称
     * @param model     模型实例
     * @return 容器本身，支持链式调用
     */
    public void registerChatModel(String modelName, ChatModel model) {
        String key = buildKey(ModelType.CHAT, modelName);
        chatModelMap.put(key, model);
    }

    /**
     * 注册向量模型
     *
     * @param modelName 模型名称
     * @param model     模型实例
     * @return 容器本身，支持链式调用
     */
    public void registerEmbeddingModel(String modelName, EmbeddingModel model) {
        String key = buildKey(ModelType.EMBEDDING, modelName);
        embeddingModelMap.put(key, model);
    }

    public void registerScoringModel(String modelName, ScoringModel model) {
        String key = buildKey(ModelType.SCORING, modelName);
        scoringModelMap.put(key, model);
    }



    /**
     * 注册查询转换器
     *
     * @param transformerName 转换器名称
     * @param transformer     转换器实例
     * @return 容器本身，支持链式调用
     */
    public RagContainer registerQueryTransformer(String transformerName, QueryTransformer transformer) {
        queryTransformerMap.put(transformerName, transformer);
        log.debug("Registered QueryTransformer: {}", transformerName);
        return this;
    }

    // ==================== 获取API ====================

    /**
     * 获取流式聊天模型
     *
     * @param modelName 模型名称
     * @return Optional包装的模型实例
     */
    public StreamingChatModel getStreamingChatModel(String modelName) {
        String key = buildKey(ModelType.STREAMING_CHAT, modelName);
        return streamingChatModelMap.getOrDefault(key,new DisabledStreamingChatModel());
    }

    /**
     * 获取聊天模型
     *
     * @param modelName 模型名称
     * @return Optional包装的模型实例
     */
    public ChatModel getChatModel(String modelName) {
        String key = buildKey(ModelType.CHAT, modelName);
        return chatModelMap.getOrDefault(key, new DisabledChatModel());
    }


    /**
     * 获取向量模型
     *
     * @param modelName 模型名称
     * @return Optional包装的模型实例
     */
    public EmbeddingModel getEmbeddingModel(String modelName) {
        String key = buildKey(ModelType.EMBEDDING, modelName);
        return embeddingModelMap.get(key);
    }

    /**
     * 获取评分模型
     *
     * @param modelName 模型名称
     * @return Optional包装的模型实例
     */
    public ScoringModel getScoringModel(String modelName) {
        String key = buildKey(ModelType.SCORING, modelName);
        return scoringModelMap.get(key);
    }

    public void registerEmbeddingStore(String beanName, EmbeddingStore<TextSegment> embeddingStore) {
        embeddingStoreMap.put(beanName,embeddingStore);
    }

    public Map<String, EmbeddingStore<TextSegment>> getAllEmbeddingStore() {
        return Map.copyOf(embeddingStoreMap);
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore(String name) {
        return embeddingStoreMap.get(name);
    }



    public boolean containsEmbeddingStore(String beanName) {
        return embeddingStoreMap.get(beanName) != null;
    }



    // ==================== 批量获取API ====================

    /**
     * 获取所有流式聊天模型
     *
     * @return 所有流式聊天模型的不可变集合
     */
    public Collection<StreamingChatModel> getAllStreamingChatModels() {
        return Map.copyOf(streamingChatModelMap).values();
    }

    /**
     * 获取所有聊天模型
     *
     * @return 所有聊天模型的不可变集合
     */
    public Collection<ChatModel> getAllChatModels() {
        return Map.copyOf(chatModelMap).values();
    }

    /**
     * 获取所有向量模型
     *
     * @return 所有向量模型的不可变集合
     */
    public Collection<EmbeddingModel> getAllEmbeddingModels() {
        return Map.copyOf(embeddingModelMap).values();
    }

    /**
     * 获取所有评分模型
     *
     * @return 所有评分模型的不可变集合
     */
    public Collection<ScoringModel> getAllScoringModels() {
        return Map.copyOf(scoringModelMap).values();
    }

    /**
     * 获取所有查询转换器
     *
     * @return 所有查询转换器的不可变集合
     */
    public Collection<QueryTransformer> getAllQueryTransformers() {
        return Map.copyOf(queryTransformerMap).values();
    }




    /**
     * 构建模型的唯一键
     *
     * @param modelType 模型类型
     * @param modelName 模型名称
     * @return 唯一键
     */
    private String buildKey(ModelType modelType, String modelName) {
        return modelType.n() + ":" + modelName;
    }
}


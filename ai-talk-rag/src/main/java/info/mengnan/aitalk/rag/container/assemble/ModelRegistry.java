package info.mengnan.aitalk.rag.container.assemble;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.service.*;
import info.mengnan.aitalk.rag.container.RagContainer;
import info.mengnan.aitalk.rag.container.factory.ModelFactory;
import info.mengnan.aitalk.rag.config.ModelConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 模型注册器
 * 接收外部传入的模型配置列表，创建模型实例并注册到容器中
 * 不进行任何数据库查询操作
 */
@Slf4j
public class ModelRegistry {

    private final RagContainer ragContainer;

    public ModelRegistry(RagContainer ragContainer) {
        this.ragContainer = ragContainer;
    }

    /**
     * 初始化方法 - 根据传入的模型配置列表注册模型到容器
     *
     * @param modelConfigs 模型配置列表
     */
    public void initialize(List<ModelConfig> modelConfigs) {
        if (modelConfigs == null || modelConfigs.isEmpty()) {
            log.warn("No model configurations provided for initialization");
            return;
        }

        // 按类型分组
        Map<String, List<ModelConfig>> configsByType = new HashMap<>();
        for (ModelConfig config : modelConfigs) {
            String keyType = config.getKeyType();
            if (keyType != null) {
                configsByType.computeIfAbsent(keyType.toLowerCase(), k -> new ArrayList<>()).add(config);
            }
        }

        // 注册各类型的模型
        registerModels(configsByType, "chat", this::registerChatModel);
        registerModels(configsByType, "streaming_chat", this::registerStreamingChatModel);
        registerModels(configsByType, "embedding", this::registerEmbeddingModel);
        registerModels(configsByType, "scoring", this::registerScoringModel);
    }

    /**
     * 通用的模型注册方法
     */
    private void registerModels(Map<String, List<ModelConfig>> configsByType,
                               String modelType,
                               ModelRegistrationHandler handler) {
        List<ModelConfig> configs = configsByType.get(modelType);
        if (configs != null) {
            for (ModelConfig config : configs) {
                try {
                    handler.register(config);
                    log.info("Successfully registered {} model: {} (provider: {})",
                            modelType, config.getModelName(), config.getModelProvider());
                } catch (Exception e) {
                    log.error("Failed to register {} model: {} - {}",
                            modelType, config.getModelName(), e.getMessage(), e);
                }
            }
        }
    }

    private void registerChatModel(ModelConfig config) {
        ChatModel chatModel = ModelFactory.createChatModel(config);
        ragContainer.registerChatModel(config.getModelName(), chatModel);
    }

    private void registerStreamingChatModel(ModelConfig config) {
        StreamingChatModel streamingChatModel = ModelFactory.createStreamingChatModel(config);
        ragContainer.registerStreamingChatModel(config.getModelName(), streamingChatModel);
    }

    private void registerEmbeddingModel(ModelConfig config) {
        EmbeddingModel embeddingModel = ModelFactory.createEmbeddingModel(config);
        ragContainer.registerEmbeddingModel(config.getModelName(), embeddingModel);
    }

    private void registerScoringModel(ModelConfig config) {
        ScoringModel scoringModel = ModelFactory.createScoringModel(config);
        ragContainer.registerScoringModel(config.getModelName(), scoringModel);
    }

    @FunctionalInterface
    private interface ModelRegistrationHandler {
        void register(ModelConfig config);
    }

    public interface AssistantUnique {
        @SystemMessage(fromResource = "rag/customer_message.txt")
        TokenStream chatStreaming(@MemoryId String memoryId, @UserMessage String userMessage);
    }

}
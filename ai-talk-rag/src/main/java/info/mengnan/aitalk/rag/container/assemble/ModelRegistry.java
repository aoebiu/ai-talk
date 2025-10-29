package info.mengnan.aitalk.rag.container.assemble;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.scoring.ScoringModel;
import info.mengnan.aitalk.rag.container.factory.CapableModelFactory;
import info.mengnan.aitalk.rag.config.ModelConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 负责动态创建模型实例,每次调用都会创建新的模型实例
 */
@Slf4j
public class ModelRegistry {

    private final CapableModelFactory modelFactory;

    public ModelRegistry(CapableModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }


    /**
     * 动态创建 ChatModel
     */
    public ChatModel createChatModel(ModelConfig config) {
        log.debug("Creating ChatModel: {} (provider: {})", config.getModelName(), config.getModelProvider());
        return modelFactory.createChatModel(config);
    }

    /**
     * 动态创建 StreamingChatModel
     */
    public StreamingChatModel createStreamingChatModel(ModelConfig config) {
        log.debug("Creating StreamingChatModel: {} (provider: {})", config.getModelName(), config.getModelProvider());
        return modelFactory.createStreamingChatModel(config);
    }

    /**
     * 动态创建 ModerationModel
     */
    public ModerationModel createModerationModel(ModelConfig config) {
        log.debug("Creating ModerationModel: {} (provider: {})", config.getModelName(), config.getModelProvider());
        return modelFactory.createModerationModel(config);
    }

    /**
     * 动态创建 EmbeddingModel
     */
    public EmbeddingModel createEmbeddingModel(ModelConfig config) {
        log.debug("Creating EmbeddingModel: {} (provider: {})", config.getModelName(), config.getModelProvider());
        return modelFactory.createEmbeddingModel(config);
    }

    /**
     * 动态创建 ScoringModel
     */
    public ScoringModel createScoringModel(ModelConfig config) {
        log.debug("Creating ScoringModel: {} (provider: {})", config.getModelName(), config.getModelProvider());
        return modelFactory.createScoringModel(config);
    }
}
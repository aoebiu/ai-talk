package info.mengnan.aitalk.rag.container.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.scoring.ScoringModel;
import info.mengnan.aitalk.common.util.Cast;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;

public enum ModelTypeMapper {
    CHAT(ChatModel.class, ModelRegistry::createChatModel),
    STREAMING_CHAT(StreamingChatModel.class, ModelRegistry::createStreamingChatModel),
    EMBEDDING(EmbeddingModel.class, ModelRegistry::createEmbeddingModel),
    MODERATION(ModerationModel.class, ModelRegistry::createModerationModel),
    SCORING(ScoringModel.class, ModelRegistry::createScoringModel);

    private final Class<?> modelClass;
    private final ModelCreator creator;

    ModelTypeMapper(Class<?> modelClass, ModelCreator creator) {
        this.modelClass = modelClass;
        this.creator = creator;
    }

    /**
     * 根据模型类型查找对应的映射器
     */
    public static ModelTypeMapper findByClass(Class<?> modelClass) {
        for (ModelTypeMapper mapper : values()) {
            if (mapper.modelClass == modelClass) {
                return mapper;
            }
        }
        throw new IllegalArgumentException("Unsupported model class: " + modelClass);
    }

    /**
     * 创建模型实例
     */
    public <T> T create(ModelRegistry registry, ModelConfig config) {
        return Cast.cast(creator.create(registry, config));
    }

    @FunctionalInterface
    interface ModelCreator {
        Object create(ModelRegistry registry, ModelConfig config);
    }
}

package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.scoring.ScoringModel;
import info.mengnan.dialogerai.common.util.Cast;
import info.mengnan.dialogerai.rag.config.ModelConfig;

public enum ModelTypeMapper {
    CHAT(ChatModel.class, UniversalModelFactory::createChatModel),
    STREAMING_CHAT(StreamingChatModel.class, UniversalModelFactory::createStreamingChatModel),
    EMBEDDING(EmbeddingModel.class, UniversalModelFactory::createEmbeddingModel),
    MODERATION(ModerationModel.class, UniversalModelFactory::createModerationModel),
    SCORING(ScoringModel.class, UniversalModelFactory::createScoringModel),
    IMAGE(ImageModel.class, UniversalModelFactory::createImageModel);


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
    public <T> T create(UniversalModelFactory factory, ModelConfig config) {
        return Cast.cast(creator.create(factory, config));
    }

    @FunctionalInterface
    interface ModelCreator {
        Object create(UniversalModelFactory factory, ModelConfig config);
    }
}

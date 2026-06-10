package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.container.factory.applier.OpenAiParamApplier;

/**
 * OpenAI 模型工厂。
 */
public class OpenAiParamApplierModelFactory extends OpenAiParamApplier
        implements ChatModelFactory, EmbeddingModelFactory, ImageModelFactory, ModerationModelFactory {

    @Override
    public Object createModel(ModelConfig config, ModelType modelType) {
        return switch (modelType) {
            case CHAT           -> buildChatModel(config).build();
            case STREAMING_CHAT -> buildStreamingChatModel(config).build();
            case EMBEDDING      -> buildEmbeddingModel(config).build();
            case MODERATE       -> buildModerationModel(config).build();
            case IMAGE          -> buildImageModel(config).build();
            default -> throw new UnsupportedOperationException(notSupported(modelType));
        };
    }

    @Override
    public OpenAiStreamingChatModel createStreamingChatModel(ModelConfig modelConfig) {
        return buildStreamingChatModel(modelConfig).build();
    }

    @Override
    public OpenAiChatModel createChatModel(ModelConfig modelConfig) {
        return buildChatModel(modelConfig).build();
    }

    @Override
    public OpenAiEmbeddingModel createEmbeddingModel(ModelConfig modelConfig) {
        return buildEmbeddingModel(modelConfig).build();
    }

    @Override
    public OpenAiImageModel createImageModel(ModelConfig modelConfig) {
        return buildImageModel(modelConfig).build();
    }
}

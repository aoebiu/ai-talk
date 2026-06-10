package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.container.factory.applier.OllamaParamApplier;

/**
 * Ollama 模型工厂
 */
public class OllamaParamApplierModelFactory extends OllamaParamApplier
        implements ChatModelFactory, EmbeddingModelFactory {

    @Override
    public Object createModel(ModelConfig config, ModelType modelType) {
        return switch (modelType) {
            case CHAT           -> buildChatModel(config).build();
            case STREAMING_CHAT -> buildStreamingChatModel(config).build();
            case EMBEDDING      -> buildEmbeddingModel(config).build();
            default -> throw new UnsupportedOperationException(notSupported(modelType));
        };
    }

    @Override
    public OllamaStreamingChatModel createStreamingChatModel(ModelConfig modelConfig) {
        return buildStreamingChatModel(modelConfig).build();
    }

    @Override
    public OllamaChatModel createChatModel(ModelConfig modelConfig) {
        return buildChatModel(modelConfig).build();
    }

    @Override
    public OllamaEmbeddingModel createEmbeddingModel(ModelConfig modelConfig) {
        return buildEmbeddingModel(modelConfig).build();
    }
}

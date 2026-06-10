package info.mengnan.dialogerai.rag.container.factory.applier;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;

public abstract class ParamApplier {

    protected Object buildChatModel(ModelConfig config) { return null; }

    protected Object buildStreamingChatModel(ModelConfig config) { return null; }

    protected Object buildEmbeddingModel(ModelConfig config) { return null; }

    protected Object buildScoringModel(ModelConfig config) { return null; }

    protected Object buildModerationModel(ModelConfig config) { return null; }

    protected Object buildImageModel(ModelConfig config) { return null; }

    protected String notSupported(ModelType type) {
        return getClass().getSimpleName() + " does not support model type: " + type.n();
    }
}

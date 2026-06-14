package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.model.moderation.ModerationModel;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import static info.mengnan.dialogerai.common.param.ModelType.MODERATION;

public interface ModerationModelFactory extends ModelFactory{

    default ModerationModel createModerationModel(ModelConfig modelConfig) {
        return (ModerationModel) createModel(modelConfig, MODERATION);
    }
}

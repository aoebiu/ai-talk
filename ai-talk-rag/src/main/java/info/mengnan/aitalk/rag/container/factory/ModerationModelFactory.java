package info.mengnan.aitalk.rag.container.factory;

import dev.langchain4j.model.moderation.ModerationModel;
import info.mengnan.aitalk.rag.config.ModelConfig;

import static info.mengnan.aitalk.common.param.ModelType.MODERATE;

public interface ModerationModelFactory extends ModelFactory{

    default ModerationModel createModerationModel(ModelConfig modelConfig) {
        return (ModerationModel) createModel(modelConfig, MODERATE);
    }
}

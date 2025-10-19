package info.mengnan.aitalk.rag.container.factory;

import dev.langchain4j.model.scoring.ScoringModel;
import info.mengnan.aitalk.rag.config.ModelConfig;

import static info.mengnan.aitalk.common.param.ModelType.SCORING;

/**
 * 评分模型工厂接口
 * 提供创建评分模型的能力
 */
public interface ScoringModelFactory extends ModelFactory {

    /**
     * 创建评分模型
     *
     * @param modelConfig 模型配置
     * @return ScoringModel 实例
     * @throws UnsupportedOperationException 如果不支持该提供商或模型类型
     * @throws RuntimeException 如果创建失败
     */
    default ScoringModel createScoringModel(ModelConfig modelConfig) {
        return (ScoringModel) createModel(modelConfig, SCORING);
    }
}

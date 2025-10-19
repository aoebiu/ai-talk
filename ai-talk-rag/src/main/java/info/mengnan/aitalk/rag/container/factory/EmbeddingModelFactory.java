package info.mengnan.aitalk.rag.container.factory;

import dev.langchain4j.model.embedding.EmbeddingModel;
import info.mengnan.aitalk.rag.config.ModelConfig;

import static info.mengnan.aitalk.common.param.ModelType.EMBEDDING;

/**
 * 嵌入模型工厂接口
 * 提供创建嵌入模型的能力
 */
public interface EmbeddingModelFactory extends ModelFactory {

    /**
     * 创建嵌入模型
     *
     * @param modelConfig 模型配置
     * @return EmbeddingModel 实例
     * @throws UnsupportedOperationException 如果不支持该提供商或模型类型
     * @throws RuntimeException 如果创建失败
     */
    default EmbeddingModel createEmbeddingModel(ModelConfig modelConfig) {
        return (EmbeddingModel) createModel(modelConfig, EMBEDDING);
    }
}

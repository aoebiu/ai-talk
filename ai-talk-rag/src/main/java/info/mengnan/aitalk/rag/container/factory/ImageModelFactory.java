package info.mengnan.aitalk.rag.container.factory;

import dev.langchain4j.model.image.ImageModel;
import info.mengnan.aitalk.rag.config.ModelConfig;

import static info.mengnan.aitalk.common.param.ModelType.IMAGE;

/**
 * 图生文模型工厂接口
 * 提供创建图生文模型的能力
 *
 * 支持的提供商：
 * - OpenAI: 通过 OpenAiImageModel 支持
 * - DashScope: 通过 QwenImageModel 支持
 */
public interface ImageModelFactory extends ModelFactory {

    /**
     * 创建图生文模型
     *
     * @param modelConfig 模型配置 (需要包含 apiKey 和 modelName)
     * @return ImageModel 实例
     * @throws UnsupportedOperationException 如果不支持该提供商的图生文功能
     * @throws RuntimeException 如果创建失败
     */
    default ImageModel createImageModel(ModelConfig modelConfig) {
        return (ImageModel) createModel(modelConfig, IMAGE);
    }
}

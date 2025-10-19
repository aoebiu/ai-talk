package info.mengnan.aitalk.rag.container.factory;

import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;


/**
 * 模型工厂接口 - 提供通用的模型创建逻辑
 * 包含模型映射配置和核心创建方法
 */
public interface ModelFactory {

    /**
     * 通用的模型创建方法 - 由具体实现类提供
     *
     * @param modelConfig 模型配置
     * @param modelType   模型类型
     * @return 创建的模型实例
     * @throws UnsupportedOperationException 如果不支持该提供商或模型类型
     * @throws RuntimeException              如果创建失败
     */
    Object createModel(ModelConfig modelConfig, ModelType modelType);

}
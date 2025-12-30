package info.mengnan.aitalk.rag.service;

import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;

/**
 * 模型配置提供者接口
 * 用于从外部（如数据库）查询模型配置
 */
@FunctionalInterface
public interface ModelConfigProvider {

    /**
     * 根据用户Id、模型名称和类型查询模型配置
     * @param memberId 用户Id
     * @param modelName 模型名称
     * @param modelType 模型类型
     * @return ModelConfig
     */
    ModelConfig findModel(Long memberId,String modelName, ModelType modelType);
}
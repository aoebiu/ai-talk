package info.mengnan.dialogerai.rag.service;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;

/**
 * 单一模型配置提供者接口
 * 用于根据模型名称和类型查找单个模型配置
 */
@FunctionalInterface
public interface SingleModelConfigProvider {

    /**
     * 根据用户 Id、模型名称和类型查找单个模型配置
     * @param memberId 用户 Id
     * @param modelName 模型名称
     * @param modelType 模型类型
     * @return 模型配置，找不到返回 null
     */
    ModelConfig findModel(Long memberId, String modelName, ModelType modelType);

}
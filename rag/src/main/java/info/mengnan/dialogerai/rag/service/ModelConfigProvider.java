package info.mengnan.dialogerai.rag.service;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import java.util.Map;

/**
 * 模型配置提供者接口
 * 用于从外部（如数据库）查询模型配置
 */
@FunctionalInterface
public interface ModelConfigProvider {

    /**
     * 根据用户 Id 加载该用户的所有模型配置
     * @param memberId 用户 Id
     * @return ModelType 到 ModelConfig 的映射
     */
    Map<ModelType, ModelConfig> loadModelConfigs(Long memberId);

}
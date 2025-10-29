package info.mengnan.aitalk.server.service;

import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.service.ChatApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型配置服务
 * 负责从数据库动态查询模型配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ChatApiKeyService chatApiKeyService;

    /**
     * 根据模型名称和类型从数据库查询模型配置
     *
     * @param modelName   模型名称
     * @param modelType   模型类型
     * @return ModelConfig
     */
    public ModelConfig findModel(String modelName, ModelType modelType) {
        if (modelName == null || modelType == null) {
            log.warn("Model name or model type is null");
            return null;
        }

        try {
            List<ChatApiKey> apiKeys = chatApiKeyService.findAll();

            ChatApiKey matchedKey = apiKeys.stream()
                    .filter(key -> modelType.n().equals(key.getKeyType()) && modelName.equals(key.getModelName()))
                    .findFirst()
                    .orElse(null);

            if (matchedKey == null) {
                log.warn("Model not found in database: name={}, type={}", modelName, modelType);
                return null;
            }

            return buildModelConfig(matchedKey);
        } catch (Exception e) {
            log.error("Failed to query model config from database: name={}, type={}", modelName, modelType, e);
            return null;
        }
    }

    private ModelConfig buildModelConfig(ChatApiKey apiKey) {
        ModelConfig config = new ModelConfig();
        config.setModelName(apiKey.getModelName());
        config.setApiKey(apiKey.getApiKey());
        config.setModelProvider(apiKey.getModelProvider());
        config.setKeyType(apiKey.getKeyType());
        return config;
    }
}
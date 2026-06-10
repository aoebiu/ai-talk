package info.mengnan.dialogerai.rag.config;

import info.mengnan.dialogerai.common.json.JSONObject;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型配置
 */
@Data
public class ModelConfig {
    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * 模型提供商 (例如: dashscope, ollama, onnx)
     */
    private String modelProvider;

    /**
     * 模型类型 (例如: chat, streaming_chat, embedding, scoring)
     */
    private String keyType;

    /**
     * 模型调参参数
     */
    private JSONObject params = new JSONObject();

    public ModelConfig() {
    }

    public ModelConfig(String modelName, String apiKey, String modelProvider, String keyType) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.modelProvider = modelProvider;
        this.keyType = keyType;
    }
}
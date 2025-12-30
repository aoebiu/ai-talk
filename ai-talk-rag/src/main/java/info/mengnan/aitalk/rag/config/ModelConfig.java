package info.mengnan.aitalk.rag.config;

import lombok.Data;

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
     * 模型提供商 (例如: dashscope, ollama, onnx)
     */
    private String modelProvider;

    /**
     * 模型类型 (例如: chat, streaming_chat, embedding, scoring)
     */
    private String keyType;

    public ModelConfig() {
    }

    public ModelConfig(String modelName, String apiKey, String modelProvider, String keyType) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.modelProvider = modelProvider;
        this.keyType = keyType;
    }
}
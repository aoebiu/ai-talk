package info.mengnan.aitalk.server.rag.container.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM模型工厂 - 使用反射动态创建模型实例，避免硬编码依赖所有提供商
 */
@Slf4j
public class ModelFactory {

    /**
     * 模型类名映射配置
     * 格式: provider -> (modelType -> className)
     */
    private static final Map<ModelProvider, Map<String, String>> MODEL_CLASS_MAPPING = new HashMap<>();

    static {
        // DashScope (通义千问)
        Map<String, String> dashscopeModels = new HashMap<>();
        dashscopeModels.put("chat", "dev.langchain4j.community.model.dashscope.QwenChatModel");
        dashscopeModels.put("streaming_chat", "dev.langchain4j.community.model.dashscope.QwenStreamingChatModel");
        dashscopeModels.put("embedding", "dev.langchain4j.community.model.dashscope.QwenEmbeddingModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.DASHSCOPE, dashscopeModels);

        // Ollama
        Map<String, String> ollamaModels = new HashMap<>();
        ollamaModels.put("chat", "dev.langchain4j.model.ollama.OllamaChatModel");
        ollamaModels.put("streaming_chat", "dev.langchain4j.model.ollama.OllamaStreamingChatModel");
        ollamaModels.put("embedding", "dev.langchain4j.model.ollama.OllamaEmbeddingModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.OLLAMA, ollamaModels);

        // ONNX Scoring Models
        Map<String, String> onnxModels = new HashMap<>();
        onnxModels.put("scoring", "dev.langchain4j.model.scoring.onnx.OnnxScoringModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.ONNX, onnxModels);

    }

    /**
     * 创建聊天模型
     */
    public static ChatModel createChatModel(ChatApiKey apiKey) {
        return (ChatModel) createModel(apiKey, "chat");
    }

    /**
     * 创建流式聊天模型
     */
    public static StreamingChatModel createStreamingChatModel(ChatApiKey apiKey) {
        return (StreamingChatModel) createModel(apiKey, "streaming_chat");
    }

    /**
     * 创建向量模型
     */
    public static EmbeddingModel createEmbeddingModel(ChatApiKey apiKey) {
        return (EmbeddingModel) createModel(apiKey, "embedding");
    }

    /**
     * 创建评分模型
     */
    public static ScoringModel createScoringModel(ChatApiKey apiKey) {
        return (ScoringModel) createModel(apiKey, "scoring");
    }

    /**
     * 通用的模型创建方法，使用反射动态创建
     */
    private static Object createModel(ChatApiKey apiKey, String modelType) {
        try {
            // 获取提供商
            ModelProvider provider = ModelProvider.fromCode(apiKey.getModelProvider());

            // 获取对应的模型类名
            Map<String, String> providerModels = MODEL_CLASS_MAPPING.get(provider);
            if (providerModels == null) {
                throw new UnsupportedOperationException(
                        "Unsupported model provider: " + provider);
            }

            String className = providerModels.get(modelType);
            if (className == null) {
                throw new UnsupportedOperationException(
                        "Model type '" + modelType + "' is not supported for provider: " + provider);
            }

            // 动态加载类
            Class<?> modelClass = Class.forName(className);

            // 获取 builder() 方法
            Method builderMethod = modelClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);

            Class<?> builderClass = builder.getClass();

            // 设置 apiKey（如果该方法存在）
            try {
                Method apiKeyMethod = builderClass.getMethod("apiKey", String.class);
                builder = apiKeyMethod.invoke(builder, apiKey.getApiKey());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have apiKey method, skipping", className);
            }

            // 设置 modelName（如果该方法存在）
            try {
                Method modelNameMethod = builderClass.getMethod("modelName", String.class);
                builder = modelNameMethod.invoke(builder, apiKey.getModelName());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have modelName method, skipping", className);
            }

            // 调用 build() 方法
            Method buildMethod = builderClass.getMethod("build");
            Object model = buildMethod.invoke(builder);

            log.info("Successfully created {} model: {} (provider: {})",
                    modelType, className, provider);

            return model;

        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Model class not found in classpath. Please add the corresponding Maven dependency for provider: "
                            + ModelProvider.fromCode(apiKey.getModelProvider()), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create model for provider: " + ModelProvider.fromCode(apiKey.getModelProvider())
                            + ", type: " + modelType, e);
        }
    }
}
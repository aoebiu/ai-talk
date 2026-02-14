package info.mengnan.aitalk.rag.container.factory;

import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static info.mengnan.aitalk.common.param.ModelType.*;

/**
 * 通用模型工厂实现
 * 继承 RagContainer，同时实现多种模型工厂接口
 * 集成了容器管理和模型创建的能力
 *
 * 支持的提供商和模型类型：
 * - DashScope (阿里云通义千问): CHAT, STREAMING_CHAT, EMBEDDING, IMAGE
 * - Ollama: CHAT, STREAMING_CHAT, EMBEDDING
 * - Cohere: SCORING
 * - OpenAI: MODERATE, IMAGE
 */
@Slf4j
public class CapableModelFactory implements ChatModelFactory,
                                            EmbeddingModelFactory,
                                            ScoringModelFactory,
                                            ModerationModelFactory,
                                            ImageModelFactory {

    private final Map<ModelProvider, Map<ModelType, String>> MODEL_CLASS_MAPPING = new HashMap<>();


    public CapableModelFactory() {
        // DashScope (通义千问) - 支持聊天、流式聊天、嵌入、图生文
        Map<ModelType, String> dashscopeModels = new HashMap<>();
        dashscopeModels.put(CHAT, "dev.langchain4j.community.model.dashscope.QwenChatModel");
        dashscopeModels.put(STREAMING_CHAT, "dev.langchain4j.community.model.dashscope.QwenStreamingChatModel");
        dashscopeModels.put(EMBEDDING, "dev.langchain4j.community.model.dashscope.QwenEmbeddingModel");
        dashscopeModels.put(IMAGE, "dev.langchain4j.community.model.dashscope.QwenImageModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.DASHSCOPE, dashscopeModels);

        // Ollama - 支持聊天、流式聊天、嵌入
        Map<ModelType, String> ollamaModels = new HashMap<>();
        ollamaModels.put(CHAT, "dev.langchain4j.model.ollama.OllamaChatModel");
        ollamaModels.put(STREAMING_CHAT, "dev.langchain4j.model.ollama.OllamaStreamingChatModel");
        ollamaModels.put(EMBEDDING, "dev.langchain4j.model.ollama.OllamaEmbeddingModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.OLLAMA, ollamaModels);

        // Cohere - 支持评分
        Map<ModelType, String> cohereModels = new HashMap<>();
        cohereModels.put(SCORING, "dev.langchain4j.model.cohere.CohereScoringModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.COHERE, cohereModels);

        // OpenAI - 支持审核、图生文
        Map<ModelType, String> openaiModels = new HashMap<>();
        openaiModels.put(MODERATE, "dev.langchain4j.model.openai.OpenAiModerationModel");
        openaiModels.put(IMAGE, "dev.langchain4j.model.openai.OpenAiImageModel");
        MODEL_CLASS_MAPPING.put(ModelProvider.OPENAI, openaiModels);
    }

    @Override
    public Object createModel(ModelConfig modelConfig, ModelType modelType) {
        try {
            // 获取提供商
            ModelProvider provider = ModelProvider.fromCode(modelConfig.getModelProvider());

            // 获取对应的模型类名
            Map<ModelType, String> providerModels = MODEL_CLASS_MAPPING.get(provider);
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
                builder = apiKeyMethod.invoke(builder, modelConfig.getApiKey());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have apiKey method, skipping", className);
            }

            // 设置 modelName（如果该方法存在）
            try {
                Method modelNameMethod = builderClass.getMethod("modelName", String.class);
                builder = modelNameMethod.invoke(builder, modelConfig.getModelName());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have modelName method, skipping", className);
            }

            // 调用 build() 方法
            Method buildMethod = builderClass.getMethod("build");
            Object model = buildMethod.invoke(builder);

            log.info("Successfully created {} model: {} (provider: {}, modelName: {})",
                    modelType, className, provider, modelConfig.getModelName());

            return model;
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Model class not found in classpath. Please add the corresponding Maven dependency for provider: "
                            + ModelProvider.fromCode(modelConfig.getModelProvider()), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create model for provider: " + ModelProvider.fromCode(modelConfig.getModelProvider())
                            + ", type: " + modelType, e);
        }
    }
}

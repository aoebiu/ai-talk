package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiModerationModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import java.util.Map;

/**
 * OpenAI 模型参数注入器。
 * 仅负责配置 Builder 参数，不负责创建模型。
 * baseUrl 可通过 {@link info.mengnan.dialogerai.rag.config.ModelConfig#getBaseUrl()} 配置，
 * 用于对接 DeepSeek、Together AI 等 OpenAI 兼容 API。
 */
public class OpenAiParamApplier extends ParamApplier {

    @Override
    protected OpenAiChatModel.OpenAiChatModelBuilder buildChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.baseUrl(config.getBaseUrl());
        builder.temperature(p.getDouble("temperature"));
        builder.topP(p.getDouble("topP"));
        builder.maxTokens(p.getInt("maxTokens"));
        builder.frequencyPenalty(p.getDouble("frequencyPenalty"));
        builder.presencePenalty(p.getDouble("presencePenalty"));
        builder.seed(p.getInt("seed"));
        return builder;

    }

    @Override
    protected OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.baseUrl(config.getBaseUrl());
        builder.temperature(p.getDouble("temperature"));
        builder.topP(p.getDouble("topP"));
        builder.maxTokens(p.getInt("maxTokens"));
        builder.frequencyPenalty(p.getDouble("frequencyPenalty"));
        builder.presencePenalty(p.getDouble("presencePenalty"));
        builder.seed(p.getInt("seed"));
        return builder;
    }

    @Override
    protected OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) builder.baseUrl(config.getBaseUrl());
        return builder;
    }

    @Override
    protected OpenAiModerationModel.OpenAiModerationModelBuilder buildModerationModel(ModelConfig config) {
        OpenAiModerationModel.OpenAiModerationModelBuilder builder = OpenAiModerationModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) builder.baseUrl(config.getBaseUrl());
        return builder;
    }

    @Override
    protected OpenAiImageModel.OpenAiImageModelBuilder buildImageModel(ModelConfig config) {
        OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) builder.baseUrl(config.getBaseUrl());
        return builder;
    }
}

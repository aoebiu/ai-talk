package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import java.util.Map;

/**
 * Ollama 模型参数注入器。
 * 仅负责配置 Builder 参数，不负责创建模型。
 */
public class OllamaParamApplier extends ParamApplier {

    @Override
    protected OllamaChatModel.OllamaChatModelBuilder buildChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                .modelName(config.getModelName());
        builder.baseUrl(config.getBaseUrl());
        builder.temperature(p.getDouble("temperature"));
        builder.topP(p.getDouble("tpP"));
        builder.topK(p.getInt("topK"));
        builder.numPredict(p.getInt("numPredict"));
        builder.repeatPenalty(p.getDouble("repeatPenalty"));
        return builder;
    }

    @Override
    protected OllamaStreamingChatModel.OllamaStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
                .modelName(config.getModelName());
        builder.baseUrl(config.getBaseUrl());
        builder.temperature(p.getDouble("temperature"));
        builder.topP(p.getDouble("tpP"));
        builder.topK(p.getInt("topK"));
        builder.numPredict(p.getInt("numPredict"));
        builder.repeatPenalty(p.getDouble("repeatPenalty"));
        return builder;
    }

    @Override
    protected OllamaEmbeddingModel.OllamaEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) builder.baseUrl(config.getBaseUrl());
        return builder;
    }
}

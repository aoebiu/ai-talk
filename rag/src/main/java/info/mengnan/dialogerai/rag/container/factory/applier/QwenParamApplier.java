package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import java.util.Map;

/**
 * 通义千问（DashScope）模型参数注入器
 */
public class QwenParamApplier extends ParamApplier {

    @Override
    protected QwenChatModel.QwenChatModelBuilder buildChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
         builder.temperature(p.getFloat("temperature"));
         builder.topP(p.getDouble("topP"));
         builder.topK(p.getInt("topK"));
         builder.maxTokens(p.getInt("maxTokens"));
         builder.seed(p.getInt("seed"));
         builder.enableSearch(p.getBool("enableSearch"));
         builder.repetitionPenalty(p.getFloat("repetitionPenalty"));
        return builder;
    }

    @Override
    protected QwenStreamingChatModel.QwenStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.temperature(p.getFloat("temperature"));
        builder.topP(p.getDouble("topP"));
        builder.topK(p.getInt("topK"));
        builder.maxTokens(p.getInt("maxTokens"));
        builder.seed(p.getInt("seed"));
        builder.enableSearch(p.getBool("enableSearch"));
        builder.repetitionPenalty(p.getFloat("repetitionPenalty"));
        return builder;
    }

    @Override
    protected QwenEmbeddingModel.QwenEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        return QwenEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
    }

    @Override
    protected WanxImageModel.WanxImageModelBuilder buildImageModel(ModelConfig config) {
        return WanxImageModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
    }
}

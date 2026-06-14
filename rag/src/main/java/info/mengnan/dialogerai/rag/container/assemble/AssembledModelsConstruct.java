package info.mengnan.dialogerai.rag.container.assemble;

import info.mengnan.dialogerai.rag.config.ChatOptionConfig;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.common.param.ModelType;

import java.util.List;

/**
 * 模型组装器
 * 根据传入的配置数据组装 AssembledModels
 */
public class AssembledModelsConstruct {

    public AssembledModelsConstruct() {
    }

    /**
     * 根据聊天选项配置和模型配置列表组装 AssembledModels
     *
     * @param chatOptionConfig 聊天选项配置
     * @param modelConfigs 该选项关联的所有模型配置
     * @return AssembledModels 对象
     */
    public AssembledModels assemble(ChatOptionConfig chatOptionConfig, List<ModelConfig> modelConfigs) {
        if (chatOptionConfig == null) {
            throw new IllegalArgumentException("Invalid chat option config");
        }

        ModelConfig chatModel = null;
        ModelConfig streamingChatModel = null;
        ModelConfig embeddingModel = null;
        ModelConfig scoringModel = null;
        ModelConfig moderationModel = null;

        // 根据 keyType 分类模型
        for (ModelConfig modelConfig : modelConfigs) {
            if (modelConfig.getKeyType() == null) {
                continue;
            }

            ModelType keyType = ModelType.valueOf(modelConfig.getKeyType().toUpperCase());
            switch (keyType) {
                case CHAT -> chatModel = modelConfig;
                case STREAMING_CHAT -> streamingChatModel = modelConfig;
                case EMBEDDING -> embeddingModel = modelConfig;
                case SCORING -> scoringModel = modelConfig;
                case MODERATION -> moderationModel = modelConfig;
                default -> {}
            }
        }

        return new AssembledModels(
                chatOptionConfig.getName(),
                chatOptionConfig.getTools(),
                chatOptionConfig.getRag(),
                chatOptionConfig.getMaxMessages(),
                chatOptionConfig.getTransform(),
                chatOptionConfig.getContentAggregator(),
                chatOptionConfig.getContentInjectorPrompt(),
                chatOptionConfig.getMaxResults(),
                chatOptionConfig.getMinScore(),
                chatOptionConfig.getInDB(),
                chatModel,
                moderationModel,
                streamingChatModel,
                embeddingModel,
                scoringModel
        );
    }
}


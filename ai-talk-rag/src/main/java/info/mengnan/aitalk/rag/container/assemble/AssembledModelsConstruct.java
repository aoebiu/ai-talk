package info.mengnan.aitalk.rag.container.assemble;

import info.mengnan.aitalk.rag.container.AssembledModels;
import info.mengnan.aitalk.rag.config.ChatOptionConfig;
import info.mengnan.aitalk.rag.config.ModelConfig;

import java.util.List;
import java.util.Map;

/**
 * 模型组装器
 * 根据传入的配置数据组装 AssembledModels
 * 不进行任何数据库查询操作
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

        // 根据 keyType 分类模型
        for (ModelConfig modelConfig : modelConfigs) {
            if (modelConfig.getKeyType() == null) {
                continue;
            }

            switch (modelConfig.getKeyType().toLowerCase()) {
                case "chat":
                    chatModel = modelConfig;
                    break;
                case "streaming_chat":
                    streamingChatModel = modelConfig;
                    break;
                case "embedding":
                    embeddingModel = modelConfig;
                    break;
                case "scoring":
                    scoringModel = modelConfig;
                    break;
                default:
                    break;
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
                streamingChatModel,
                embeddingModel,
                scoringModel
        );
    }
}


package info.mengnan.aitalk.server.rag.container.assemble;

import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.entity.ChatOption;
import info.mengnan.aitalk.repository.entity.ChatOptionApiKeyRel;
import info.mengnan.aitalk.repository.service.ChatApiKeyService;
import info.mengnan.aitalk.repository.service.ChatOptionApiKeyRelService;
import info.mengnan.aitalk.repository.service.ChatOptionService;
import info.mengnan.aitalk.server.common.ModelType;
import info.mengnan.aitalk.server.rag.container.AssembledModels;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AssembledModelsConstruct {
    private final ChatOptionApiKeyRelService chatOptionApiKeyRelService;
    private final ChatOptionService chatOptionService;
    private final ChatApiKeyService chatApiKeyService;

    public ChatOption findChatOption(Long optionId) {
        return chatOptionService.findById(optionId);
    }

    /**
     * 根据ChatOption配置，组装该配置使用的所有模型
     *
     * @param chatOption 聊天配置
     * @return 包含按类型分类的模型配置对象
     */
    public AssembledModels assemble(ChatOption chatOption) {
        if (chatOption == null || chatOption.getId() == null) {
            throw new IllegalArgumentException("Invalid chat option");
        }

        List<ChatOptionApiKeyRel> relations = chatOptionApiKeyRelService.findByChatOptionId(chatOption.getId());

        ChatApiKey chatModel = null;
        ChatApiKey streamingChatModel = null;
        ChatApiKey embeddingModel = null;
        ChatApiKey scoringModel = null;

        if (relations != null && !relations.isEmpty()) {
            for (ChatOptionApiKeyRel rel : relations) {
                ChatApiKey apiKey = chatApiKeyService.findById(rel.getChatApiKeyId());
                if (apiKey != null && apiKey.getKeyType() != null) {
                    ModelType modelType = ModelType.valueOf(apiKey.getKeyType().toUpperCase(Locale.ROOT));
                    switch (modelType) {
                        case CHAT:
                            chatModel = apiKey;
                            break;
                        case STREAMING_CHAT:
                            streamingChatModel = apiKey;
                            break;
                        case EMBEDDING:
                            embeddingModel = apiKey;
                            break;
                        case SCORING:
                            scoringModel = apiKey;
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return new AssembledModels(
                chatOption.getName(),
                chatOption.getTools(),
                chatOption.getRag(),
                chatOption.getMaxMessages(),
                chatOption.getTransform(),
                chatOption.getContentAggregator(),
                chatOption.getContentInjectorPrompt(),
                chatOption.getMaxResults(),
                chatOption.getMinScore(),
                chatModel,
                streamingChatModel,
                embeddingModel,
                scoringModel
        );
    }
}

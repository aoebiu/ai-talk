package info.mengnan.aitalk.server.service;

import info.mengnan.aitalk.rag.container.assemble.AssembledModels;
import info.mengnan.aitalk.rag.container.assemble.AssembledModelsConstruct;
import info.mengnan.aitalk.rag.config.ChatOptionConfig;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.entity.ChatOption;
import info.mengnan.aitalk.repository.entity.ChatOptionApiKeyRel;
import info.mengnan.aitalk.repository.service.ChatApiKeyService;
import info.mengnan.aitalk.repository.service.ChatOptionApiKeyRelService;
import info.mengnan.aitalk.repository.service.ChatOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG适配服务
 * 负责从数据库查询数据并组装成 AssembledModels
 * 作为 server 项目和 rag 项目之间的适配层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAdapterService {

    private final ChatOptionService chatOptionService;
    private final ChatApiKeyService chatApiKeyService;
    private final ChatOptionApiKeyRelService chatOptionApiKeyRelService;
    private final AssembledModelsConstruct assembledModelsConstruct;

    /**
     * 根据 optionId 查询并组装 AssembledModels
     *
     * @param optionId 聊天选项ID
     * @return AssembledModels 对象
     * @throws IllegalArgumentException 如果找不到对应的聊天配置或配置未启用
     */
    public AssembledModels assembleModels(Long optionId) {
        // 1. 查询聊天选项
        ChatOption chatOption = chatOptionService.findById(optionId);
        if (chatOption == null || !chatOption.getEnabled()) {
            throw new IllegalArgumentException("找不到对应的聊天配置或配置未启用,optionId: " + optionId);
        }

        // 2. 转换为 ChatOptionConfig
        ChatOptionConfig chatOptionConfig = convertToChatOptionConfig(chatOption);

        // 3. 查询该选项关联的所有模型
        List<ChatOptionApiKeyRel> relations = chatOptionApiKeyRelService.findByChatOptionId(optionId);
        List<ModelConfig> modelConfigs = new ArrayList<>();

        if (relations != null && !relations.isEmpty()) {
            for (ChatOptionApiKeyRel rel : relations) {
                ChatApiKey apiKey = chatApiKeyService.findById(rel.getChatApiKeyId());
                if (apiKey != null) {
                    modelConfigs.add(convertToModelConfig(apiKey));
                }
            }
        }

        // 4. 组装并返回
        return assembledModelsConstruct.assemble(chatOptionConfig, modelConfigs);
    }

    private ChatOptionConfig convertToChatOptionConfig(ChatOption chatOption) {
        ChatOptionConfig config = new ChatOptionConfig();
        config.setName(chatOption.getName());
        config.setTools(chatOption.getTools());
        config.setRag(chatOption.getRag());
        config.setMaxMessages(chatOption.getMaxMessages());
        config.setTransform(chatOption.getTransform());
        config.setContentAggregator(chatOption.getContentAggregator());
        config.setContentInjectorPrompt(chatOption.getContentInjectorPrompt());
        config.setMaxResults(chatOption.getMaxResults());
        config.setMinScore(chatOption.getMinScore());
        config.setInDB(chatOption.getInDB());
        return config;
    }

    private ModelConfig convertToModelConfig(ChatApiKey apiKey) {
        ModelConfig config = new ModelConfig();
        config.setModelName(apiKey.getModelName());
        config.setApiKey(apiKey.getApiKey());
        config.setModelProvider(apiKey.getModelProvider());
        config.setKeyType(apiKey.getKeyType());
        return config;
    }
}
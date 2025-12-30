package info.mengnan.aitalk.server.content;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.constant.promptTemplate.PromptTemplateConstant;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.entity.ChatSession;
import info.mengnan.aitalk.repository.service.ChatSessionService;
import info.mengnan.aitalk.server.service.ModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static info.mengnan.aitalk.common.param.MessageRole.USER;

/**
 * 聊天历史压缩服务
 * 使用 ChatModel 对历史对话进行总结压缩
 */
@Slf4j
@Component
public class ChatHistoryCompressing {

    private final PromptTemplate promptTemplate;
    private final ModelRegistry modelRegistry;
    private final ModelConfigService modelConfigService;
    private final ChatSessionService chatSessionService;

    @Value("${chat.compress.model-name:gpt-3.5-turbo}")
    private String compressModelName;

    public ChatHistoryCompressing(@Qualifier("compressingPrompt")
                                  @Autowired(required = false)
                                  PromptTemplate promptTemplate,
                                  ModelRegistry modelRegistry,
                                  ModelConfigService modelConfigService,
                                  ChatSessionService chatSessionService) {
        this.promptTemplate = promptTemplate != null ? promptTemplate : PromptTemplateConstant.COMPRESSION_PROMPT_TEMPLATE;
        this.modelRegistry = modelRegistry;
        this.modelConfigService = modelConfigService;
        this.chatSessionService = chatSessionService;
    }

    private ChatModel getChatModel(Long memberId) {
        ModelConfig chatConfig = modelConfigService.findModel(memberId, compressModelName, ModelType.CHAT);
        if (chatConfig == null) {
            throw new RuntimeException("压缩模型配置不存在: " + compressModelName);
        }
        return modelRegistry.createChatModel(chatConfig);
    }

    /**
     * 压缩会话历史
     *
     * @param messagesToCompress 需要压缩的消息列表
     * @return 压缩后的摘要文本
     */
    public String compressHistory(List<ChatMessage> messagesToCompress) {
        if (messagesToCompress == null || messagesToCompress.isEmpty()) {
            log.warn("Compressed message list is empty");
            return "";
        }

        log.info("Start compressing {} historical messages", messagesToCompress.size());

        try {
            // 构建压缩提示词
            String conversationText = buildConversationText(messagesToCompress);
            Prompt prompt = createPrompt(Query.from(conversationText));

            ChatSession chatSession = chatSessionService.findBySessionId(messagesToCompress.get(0).getSessionId());
            // 动态创建 ChatModel 并调用 LLM 进行总结
            ChatModel chatModel = getChatModel(chatSession.getMemberId());
            String compressedText = chatModel.chat(prompt.text());

            log.info("Message compression completed, original message number: {}, compressed length: {}",
                    messagesToCompress.size(), compressedText.length());

            return compressedText;
        } catch (Exception e) {
            log.error("Failed to compress historical messages", e);
            throw new RuntimeException("压缩历史消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建对话文本
     */
    private String buildConversationText(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            String roleLabel = USER.equals(msg.getRole()) ? "用户" : "助手";
            sb.append(roleLabel).append(": ").append(msg.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 创建压缩提示词
     */
    protected Prompt createPrompt(Query query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query.text());
        return promptTemplate.apply(variables);
    }
}
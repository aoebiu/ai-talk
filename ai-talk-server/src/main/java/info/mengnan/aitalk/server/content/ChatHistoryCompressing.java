package info.mengnan.aitalk.server.content;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.repository.entity.ChatMessage;
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

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
            请将以下对话内容进行总结和压缩，保留关键信息和上下文。总结应该简洁但要包含重要的讨论点和结论。

            对话内容:
            {{query}}

            请提供一个简明的总结:
            """
    );

    private final PromptTemplate promptTemplate;
    private final ModelRegistry modelRegistry;
    private final ModelConfigService modelConfigService;

    @Value("${chat.compress.model-name:gpt-3.5-turbo}")
    private String compressModelName;

    public ChatHistoryCompressing(@Qualifier("compressingPrompt")
                                  @Autowired(required = false)
                                  PromptTemplate promptTemplate,
                                  ModelRegistry modelRegistry,
                                  ModelConfigService modelConfigService) {
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.modelRegistry = modelRegistry;
        this.modelConfigService = modelConfigService;
    }

    private ChatModel getChatModel() {
        ModelConfig chatConfig = modelConfigService.findModel(compressModelName, ModelType.CHAT);
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
            log.warn("压缩消息列表为空");
            return "";
        }

        log.info("开始压缩 {} 条历史消息", messagesToCompress.size());

        try {
            // 构建压缩提示词
            String conversationText = buildConversationText(messagesToCompress);
            Prompt prompt = createPrompt(Query.from(conversationText));

            // 动态创建 ChatModel 并调用 LLM 进行总结
            ChatModel chatModel = getChatModel();
            String compressedText = chatModel.chat(prompt.text());

            log.info("消息压缩完成，原始消息数: {}, 压缩后长度: {}",
                    messagesToCompress.size(), compressedText.length());

            return compressedText;
        } catch (Exception e) {
            log.error("压缩历史消息失败", e);
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
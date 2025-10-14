package info.mengnan.aitalk.server.content;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.rag.container.RagContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
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

    private final RagContainer ragContainer;
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                    请将以下对话内容进行总结和压缩，保留关键信息和上下文。总结应该简洁但要包含重要的讨论点和结论。
                                    "对话内容:{{query}}" +
                                    "请提供一个简明的总结:";
                    """
    );
    protected final PromptTemplate promptTemplate;

    public ChatHistoryCompressing(@Qualifier("compressingPrompt")
                                  @Autowired(required = false)
                                  PromptTemplate promptTemplate,
                                  @Lazy RagContainer ragContainer) {
        if (promptTemplate == null) {
            promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        }
        this.promptTemplate = promptTemplate;
        this.ragContainer = ragContainer;
    }

    /**
     * 懒加载获取ChatModel，避免循环依赖
     */
    private ChatModel getChatModel() {
        return ragContainer.getChatModel("gpt-3.5-turbo");
    }

    /**
     * 压缩会话历史
     *
     * @param messagesToCompress 需要压缩的消息列表
     * @return 压缩后的摘要文本
     */
    public String compressHistory(List<ChatMessage> messagesToCompress) {
        if (messagesToCompress.isEmpty()) {
            return "";
        }
        // 构建压缩提示词
        String conversationText = buildConversationText(messagesToCompress);
        Prompt prompt = createPrompt(Query.from(conversationText));

        // 调用 LLM 进行总结
        return getChatModel().chat(prompt.text());

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

    protected Prompt createPrompt(Query query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query.text());
        return promptTemplate.apply(variables);
    }
}
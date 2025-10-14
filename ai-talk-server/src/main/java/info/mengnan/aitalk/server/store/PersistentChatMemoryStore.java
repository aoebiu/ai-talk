package info.mengnan.aitalk.server.store;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.service.ChatMessageService;
import info.mengnan.aitalk.server.content.ChatHistoryCompressing;
import info.mengnan.aitalk.server.content.TokenCounting;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Component;

import java.util.List;

import static info.mengnan.aitalk.server.param.common.MessageRole.*;

@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ChatMessageService chatMessageService;
    private final ChatHistoryCompressing compressing;
    private final TokenCounting tokenCounting;

    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        // 查询所有消息类型
        List<ChatMessage> dbMessages = chatMessageService.findChat(sessionId);

        // 找到最后一个compress消息的索引
        int lastCompressIndex = -1;
        for (int i = dbMessages.size() - 1; i >= 0; i--) {
            if (COMPRESS.equals(dbMessages.get(i).getRole())) {
                lastCompressIndex = i;
                break;
            }
        }

        List<dev.langchain4j.data.message.ChatMessage> list = Lists.newArrayList();
        for (int i = 0; i < dbMessages.size(); i++) {
            ChatMessage dbMessage = dbMessages.get(i);
            String role = dbMessage.getRole();

            // 如果存在compress消息，过滤掉compress之前的user和assistant消息
            if (lastCompressIndex != -1 && i < lastCompressIndex) {
                if (USER.equals(role) || ASSISTANT.equals(role)) {
                    continue;
                }
            }

            list.add(convertToChatMessage(dbMessage));
        }
        return list;
    }

    @Override
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String sessionId = memoryId.toString();

        dev.langchain4j.data.message.ChatMessage chatMessage = messages.get(messages.size() - 1);
        ChatMessage dbMessage = new ChatMessage();
        dbMessage.setSessionId(sessionId);

        if (chatMessage instanceof UserMessage msg) {
            dbMessage.setRole(USER.n());
            dbMessage.setContent(msg.singleText());

        } else if (chatMessage instanceof AiMessage msg) {
            dbMessage.setRole(ASSISTANT.n());
            dbMessage.setContent(msg.text());

        } else if (chatMessage instanceof SystemMessage msg) {
            dbMessage.setRole(SYSTEM.n());
            dbMessage.setContent(msg.text());

        }
        convertToChatMessage(dbMessage);
        chatMessageService.insert(dbMessage);

        List<ChatMessage> chats = chatMessageService.findChatByRole(sessionId,
                List.of(USER.n(), ASSISTANT.n(), COMPRESS.n()));
        if (messages.size() > 1) {
            int summary = 0;
            for (ChatMessage chat : chats) {
                summary += tokenCounting.estimateTokenCount(chat.getContent());
            }
            if (summary > 1500) {
                String compressRes = compressing.compressHistory(chats);
                saveCompressedSummary(sessionId, compressRes, chats);
            }
        }
    }

    private void saveCompressedSummary(String sessionId, String summary,
                                       List<ChatMessage> originalMessages) {
        ChatMessage lastMsg = originalMessages.get(originalMessages.size() - 1);

        // 创建压缩摘要消息
        ChatMessage summaryMessage = new ChatMessage();
        summaryMessage.setSessionId(sessionId);
        summaryMessage.setRole("compress");

        // 在摘要前添加说明
        String summaryWithMeta = String.format("[历史对话摘要 - 压缩了 %d 条消息]\n%s",
                originalMessages.size(), summary);
        summaryMessage.setContent(summaryWithMeta);

        summaryMessage.setCreatedAt(lastMsg.getCreatedAt()); // 使用最后一条消息的时间保持顺序
        chatMessageService.insert(summaryMessage);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        chatMessageService.deleteBySessionId(sessionId);
    }

    /**
     * 转换数据库消息转换成对应类型
     */
    private dev.langchain4j.data.message.ChatMessage convertToChatMessage(ChatMessage dbMessage) {
        dev.langchain4j.data.message.ChatMessage chatMessage;
        if (USER.equals(dbMessage.getRole())) {
            chatMessage = UserMessage.from(dbMessage.getContent());

        } else if (ASSISTANT.equals(dbMessage.getRole())) {
            chatMessage = AiMessage.from(dbMessage.getContent());

        } else if (SYSTEM.equals(dbMessage.getRole())) {
            chatMessage = SystemMessage.from(dbMessage.getContent());

        } else if (COMPRESS.equals(dbMessage.getRole())) {
            chatMessage = UserMessage.from(dbMessage.getContent());

        } else {
            throw new IllegalArgumentException("不支持的消息类型：" + dbMessage.getRole());
        }
        return chatMessage;
    }
}

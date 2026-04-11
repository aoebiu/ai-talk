package info.mengnan.aitalk.server.store;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.aitalk.common.json.JSONObject;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.entity.ChatMessageExtras;
import info.mengnan.aitalk.repository.entity.ToolExecutionRequestSnapshot;
import info.mengnan.aitalk.repository.service.ChatMessageService;
import info.mengnan.aitalk.server.content.ChatHistoryCompressing;
import info.mengnan.aitalk.server.content.TokenCounting;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static info.mengnan.aitalk.common.param.MessageRole.*;
import static info.mengnan.aitalk.rag.config.DefaultModelConfig.DEFAULT_SESSION;

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
        if (DEFAULT_SESSION.equals(sessionId)) return;

        dev.langchain4j.data.message.ChatMessage chatMessage = messages.get(messages.size() - 1);
        ChatMessage dbMessage = new ChatMessage();
        dbMessage.setSessionId(sessionId);

        if (chatMessage instanceof UserMessage msg) {
            dbMessage.setRole(USER.n());
            dbMessage.setContent(msg.singleText());
            dbMessage.setExtras(buildUserExtras(msg));

        } else if (chatMessage instanceof AiMessage msg) {
            dbMessage.setRole(ASSISTANT.n());
            String aiText = msg.text();
            dbMessage.setContent(aiText != null ? aiText : "");
            dbMessage.setExtras(buildAiExtras(msg));

        } else if (chatMessage instanceof SystemMessage msg) {
            dbMessage.setRole(SYSTEM.n());
            dbMessage.setContent(msg.text());

        } else if (chatMessage instanceof ToolExecutionResultMessage msg) {
            dbMessage.setRole(TOOL.n());
            dbMessage.setContent(msg.text());
            dbMessage.setExtras(buildToolExtras(msg));
        }
        chatMessageService.insert(dbMessage);

        List<ChatMessage> chats = chatMessageService.findChatByRole(sessionId, List.of(USER.n(), ASSISTANT.n(), COMPRESS.n(), TOOL.n()));
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

    private static ChatMessageExtras buildUserExtras(UserMessage msg) {
        if (msg.name() == null || msg.name().isBlank()) {
            return null;
        }
        ChatMessageExtras ex = new ChatMessageExtras();
        ex.setUserName(msg.name());
        return ex;
    }

    private static ChatMessageExtras buildAiExtras(AiMessage msg) {
        boolean hasThinking = msg.thinking() != null && !msg.thinking().isBlank();
        boolean hasTools = msg.hasToolExecutionRequests();
        Map<String, Object> attrs = msg.attributes();
        boolean hasAttrs = attrs != null && !attrs.isEmpty();
        if (!hasThinking && !hasTools && !hasAttrs) {
            return null;
        }
        ChatMessageExtras ex = new ChatMessageExtras();
        if (hasThinking) {
            ex.setThinking(msg.thinking());
        }
        if (hasTools) {
            List<ToolExecutionRequestSnapshot> snapshots = new ArrayList<>();
            for (ToolExecutionRequest r : msg.toolExecutionRequests()) {
                ToolExecutionRequestSnapshot s = new ToolExecutionRequestSnapshot();
                s.setId(r.id());
                s.setName(r.name());
                s.setArguments(r.arguments());
                snapshots.add(s);
            }
            ex.setToolExecutionRequests(snapshots);
        }
        if (hasAttrs) {
            ex.setAttributes(new HashMap<>(attrs));
        }
        return ex;
    }

    private static ChatMessageExtras buildToolExtras(ToolExecutionResultMessage msg) {
        boolean hasId = msg.id() != null && !msg.id().isBlank();
        boolean hasName = msg.toolName() != null && !msg.toolName().isBlank();
        if (!hasId && !hasName) {
            return null;
        }
        ChatMessageExtras ex = new ChatMessageExtras();
        if (hasId) {
            ex.setToolCallId(msg.id());
        }
        if (hasName) {
            ex.setToolName(msg.toolName());
        }
        return ex;
    }

    private ChatMessageExtras readExtras(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return new JSONObject(json).toBean(ChatMessageExtras.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveCompressedSummary(String sessionId, String summary, List<ChatMessage> originalMessages) {
        ChatMessage lastMsg = originalMessages.get(originalMessages.size() - 1);

        // 创建压缩摘要消息
        ChatMessage summaryMessage = new ChatMessage();
        summaryMessage.setSessionId(sessionId);
        summaryMessage.setRole(COMPRESS.n());

        // 在摘要前添加说明
        String summaryWithMeta = String.format("[历史对话摘要 - 压缩了 %d 条消息]\n%s", originalMessages.size(), summary);
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
        String content = dbMessage.getContent() != null ? dbMessage.getContent() : "";
        ChatMessageExtras ex = dbMessage.getExtras();

        if (USER.equals(dbMessage.getRole())) {
            if (ex != null && ex.getUserName() != null && !ex.getUserName().isBlank()) {
                chatMessage = UserMessage.from(ex.getUserName(), content);
            } else {
                chatMessage = UserMessage.from(content);
            }

        } else if (ASSISTANT.equals(dbMessage.getRole())) {
            if (ex == null || !hasAiExtras(ex)) {
                chatMessage = AiMessage.from(content);
            } else {
                AiMessage.Builder b = AiMessage.builder().text(content);
                if (ex.getThinking() != null && !ex.getThinking().isBlank()) {
                    b.thinking(ex.getThinking());
                }
                if (ex.getToolExecutionRequests() != null && !ex.getToolExecutionRequests().isEmpty()) {
                    List<ToolExecutionRequest> reqs = new ArrayList<>();
                    for (ToolExecutionRequestSnapshot s : ex.getToolExecutionRequests()) {
                        reqs.add(ToolExecutionRequest.builder()
                                .id(s.getId() != null ? s.getId() : "")
                                .name(s.getName() != null ? s.getName() : "")
                                .arguments(s.getArguments() != null ? s.getArguments() : "")
                                .build());
                    }
                    b.toolExecutionRequests(reqs);
                }
                if (ex.getAttributes() != null && !ex.getAttributes().isEmpty()) {
                    b.attributes(new HashMap<>(ex.getAttributes()));
                }
                chatMessage = b.build();
            }

        } else if (SYSTEM.equals(dbMessage.getRole())) {
            chatMessage = SystemMessage.from(content);

        } else if (COMPRESS.equals(dbMessage.getRole())) {
            chatMessage = UserMessage.from(content);

        } else if (TOOL.equals(dbMessage.getRole())) {
            String toolId = ex != null && ex.getToolCallId() != null ? ex.getToolCallId() : "";
            String toolName = ex != null && ex.getToolName() != null ? ex.getToolName() : "";
            chatMessage = ToolExecutionResultMessage.from(toolId, toolName, content);
        } else {
            throw new IllegalArgumentException("不支持的消息类型：" + dbMessage.getRole());
        }
        return chatMessage;
    }

    private static boolean hasAiExtras(ChatMessageExtras ex) {
        return (ex.getThinking() != null && !ex.getThinking().isBlank())
                || (ex.getToolExecutionRequests() != null && !ex.getToolExecutionRequests().isEmpty())
                || (ex.getAttributes() != null && !ex.getAttributes().isEmpty());
    }

}

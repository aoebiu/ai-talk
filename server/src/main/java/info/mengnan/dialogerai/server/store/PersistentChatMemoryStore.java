package info.mengnan.dialogerai.server.store;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.dialogerai.repository.entity.ChatMessage;
import info.mengnan.dialogerai.repository.entity.ChatMessageExtras;
import info.mengnan.dialogerai.repository.entity.ChatMessageRagSource;
import info.mengnan.dialogerai.repository.repo.ChatMessageRagSourceRepository;
import info.mengnan.dialogerai.repository.repo.ChatMessageRepository;
import info.mengnan.dialogerai.rag.injector.RagSourceStore;
import info.mengnan.dialogerai.server.core.ChatHistoryCompressing;
import info.mengnan.dialogerai.server.core.TokenCounting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static info.mengnan.dialogerai.common.param.MessageRole.*;
import static info.mengnan.dialogerai.rag.config.DefaultModelConfig.DEFAULT_SESSION;
import static info.mengnan.dialogerai.rag.constant.promptTemplate.PromptTemplateConstant.CONTENT_INJECTION_SEPARATOR;
import static info.mengnan.dialogerai.rag.constant.promptTemplate.PromptTemplateConstant.CONTENT_INJECTOR_PROMPT_TEMPLATE;

@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private static final ContentInjector CONTENT_INJECTOR =
            new DefaultContentInjector(CONTENT_INJECTOR_PROMPT_TEMPLATE);

    private final ChatMessageRepository chatMessageService;
    private final ChatHistoryCompressing compressing;
    private final TokenCounting tokenCounting;
    private final RagSourceStore ragSourceStore;
    private final ChatMessageRagSourceRepository chatMessageRagSourceRepository;

    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        // 查询所有消息类型
        List<ChatMessage> dbMessages = chatMessageService.findChat(sessionId);

        // 找到最后一个 compress 消息的索引
        int lastCompressIndex = -1;
        for (int i = dbMessages.size() - 1; i >= 0; i--) {
            if (COMPRESS.equals(dbMessages.get(i).getRole())) {
                lastCompressIndex = i;
                break;
            }
        }

        // 过滤出要发送给 LLM 的消息（从 compress 之后开始收集）
        List<ChatMessage> filtered = new ArrayList<>();
        if (lastCompressIndex != -1) {
            // 从 compress 消息之后开始添加
            for (int i = lastCompressIndex + 1; i < dbMessages.size(); i++) {
                filtered.add(dbMessages.get(i));
            }
        } else {
            // 没有 compress 消息，添加所有
            filtered.addAll(dbMessages);
        }

        // 已关联消息 ID 的（历史对话的 RAG 内容，关联的是 ASSISTANT 消息 ID）
        Map<Long, List<ChatMessageRagSource>> ragSourceByAssistantId = chatMessageRagSourceRepository.findGroupedByMessage(sessionId);
        // message_id 为 null 的 pending 记录（当前最新一轮的 RAG 内容）
        List<ChatMessageRagSource> pendingRagSources = chatMessageRagSourceRepository.findPending(sessionId);

        boolean pendingRagUsed = false;
        List<dev.langchain4j.data.message.ChatMessage> result = new ArrayList<>();
        // 先收集所有ASSISTANT消息的RAG来源映射（按消息ID）
        Map<Long, List<ChatMessageRagSource>> ragSourceByMessageId = new HashMap<>();
        if (ragSourceByAssistantId != null) {
            ragSourceByMessageId.putAll(ragSourceByAssistantId);
        }

        for (int i = 0; i < filtered.size(); i++) {
            ChatMessage chatMessage = filtered.get(i);
            String role = chatMessage.getRole();

            if (USER.equals(role)) {
                // 获取当前USER消息的RAG来源
                List<ChatMessageRagSource> ragSources = null;

                // 检查下一条消息是否是ASSISTANT
                if (i + 1 < filtered.size() && ASSISTANT.equals(filtered.get(i + 1).getRole())) {
                    // 如果下一条是ASSISTANT，获取该ASSISTANT对应的RAG来源
                    ChatMessage nextAssistant = filtered.get(i + 1);
                    ragSources = ragSourceByMessageId.get(nextAssistant.getId());
                }
                else if (i == filtered.size() - 1 ||
                        (i + 1 < filtered.size() && !ASSISTANT.equals(filtered.get(i + 1).getRole()))) {
                    // 如果这是最新的USER消息,使用pendingRagSources
                    if (pendingRagSources != null && !pendingRagSources.isEmpty() && !pendingRagUsed) {
                        ragSources = pendingRagSources;
                        pendingRagUsed = true;
                    }
                }

                // 如果有RAG来源,注入到USER消息中
                dev.langchain4j.data.message.ChatMessage userMessage;
                if (ragSources != null && !ragSources.isEmpty()) {
                    ChatMessage enrichedUserMessage = withRagContent(chatMessage, ragSources);
                    userMessage = convertToChatMessage(enrichedUserMessage);
                } else {
                    userMessage = convertToChatMessage(chatMessage);
                }
                result.add(userMessage);
            }
            else {
                // 其他消息直接转换添加
                dev.langchain4j.data.message.ChatMessage converted = convertToChatMessage(chatMessage);
                result.add(converted);
            }
        }

        return result;
    }

    @Override
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String sessionId = memoryId.toString();
        if (DEFAULT_SESSION.equals(sessionId)) {
            return;
        }

        dev.langchain4j.data.message.ChatMessage chatMessage = messages.get(messages.size() - 1);
        ChatMessage dbMessage = new ChatMessage();
        dbMessage.setSessionId(sessionId);

        if (chatMessage instanceof UserMessage msg) {
            dbMessage.setRole(USER.n());
            String rawContent = msg.singleText();

            int sepIdx = rawContent.indexOf(CONTENT_INJECTION_SEPARATOR);
            dbMessage.setContent(sepIdx != -1 ? rawContent.substring(0, sepIdx) : rawContent);

            dbMessage.setExtras(buildUserExtras(msg));
            chatMessageService.insert(dbMessage);

        } else if (chatMessage instanceof AiMessage msg) {
            dbMessage.setRole(ASSISTANT.n());
            String aiText = msg.text();
            dbMessage.setContent(aiText != null ? aiText : "");
            dbMessage.setExtras(buildAiExtras(msg));
            chatMessageService.insert(dbMessage);

            ragSourceStore.linkToMessage(sessionId, dbMessage.getId());

        } else if (chatMessage instanceof SystemMessage msg) {
            dbMessage.setRole(SYSTEM.n());
            dbMessage.setContent(msg.text());
            chatMessageService.insert(dbMessage);

        } else if (chatMessage instanceof ToolExecutionResultMessage msg) {
            dbMessage.setRole(TOOL.n());
            dbMessage.setContent(msg.text());
            dbMessage.setExtras(buildToolExtras(msg));
            chatMessageService.insert(dbMessage);
        }

        List<ChatMessage> chats = chatMessageService.findChat(sessionId, List.of(USER.n(), ASSISTANT.n(), COMPRESS.n(), TOOL.n()));
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
        boolean hasName = msg.name() != null && !msg.name().isBlank();
        if (!hasName) return null;
        ChatMessageExtras ex = new ChatMessageExtras();
        ex.setUserName(msg.name());
        return ex;
    }

    private static ChatMessageExtras buildAiExtras(AiMessage msg) {
        Map<String, Object> attrs = msg.attributes();
        boolean hasAttrs = attrs != null && !attrs.isEmpty();
        boolean hasThinking = msg.thinking() != null && !msg.thinking().isBlank();
        boolean hasTools = msg.hasToolExecutionRequests();
        if (!hasThinking && !hasTools && !hasAttrs) return null;

        ChatMessageExtras ex = new ChatMessageExtras();
        if (hasThinking) ex.setThinking(msg.thinking());

        if (hasTools) {
            List<ChatMessageExtras.ToolExecutionRequestSnapshot> snapshots = new ArrayList<>();
            for (ToolExecutionRequest r : msg.toolExecutionRequests()) {
                ChatMessageExtras.ToolExecutionRequestSnapshot s = new ChatMessageExtras.ToolExecutionRequestSnapshot();
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

    private static ChatMessage withRagContent(ChatMessage original, List<ChatMessageRagSource> sources) {
        List<Content> contents = sources.stream()
                .map(s -> Content.from(TextSegment.from(s.getContent())))
                .toList();
        String text = original.getContent() != null ? original.getContent() : "";
        ChatMessageExtras ex = original.getExtras();
        UserMessage base = (ex != null && ex.getUserName() != null && !ex.getUserName().isBlank())
                ? UserMessage.from(ex.getUserName(), text)
                : UserMessage.from(text);
        // 走与 CapturingContentInjector 完全相同的注入路径，格式由库保证一致
        UserMessage injected = (UserMessage) CONTENT_INJECTOR.inject(contents, base);
        ChatMessage copy = new ChatMessage();
        copy.setId(original.getId());
        copy.setSessionId(original.getSessionId());
        copy.setRole(original.getRole());
        copy.setContent(injected.singleText());
        copy.setExtras(original.getExtras());
        copy.setCreatedAt(original.getCreatedAt());
        return copy;
    }

    private void saveCompressedSummary(String sessionId, String summary, List<ChatMessage> originalMessages) {
        ChatMessage lastMsg = originalMessages.get(originalMessages.size() - 1);

        ChatMessage summaryMessage = new ChatMessage();
        summaryMessage.setSessionId(sessionId);
        summaryMessage.setRole(COMPRESS.n());

        String summaryWithMeta = String.format("[历史对话摘要 - 压缩了 %d 条消息]\n%s", originalMessages.size(), summary);
        summaryMessage.setContent(summaryWithMeta);

        summaryMessage.setCreatedAt(lastMsg.getCreatedAt());
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
                    List<ToolExecutionRequest> results = new ArrayList<>();
                    for (ChatMessageExtras.ToolExecutionRequestSnapshot s : ex.getToolExecutionRequests()) {
                        results.add(ToolExecutionRequest.builder()
                                .id(s.getId() != null ? s.getId() : "")
                                .name(s.getName() != null ? s.getName() : "")
                                .arguments(s.getArguments() != null ? s.getArguments() : "")
                                .build());
                    }
                    b.toolExecutionRequests(results);
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

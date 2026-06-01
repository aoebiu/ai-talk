package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.ChatMessage;
import info.mengnan.dialogerai.repository.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {

    private final ChatMessageMapper mapper;

    public List<ChatMessage> findChat(String sessionId, List<String> roles) {
        return mapper.findChatByRole(sessionId, roles);
    }

    public List<ChatMessage> findChat(String sessionId) {
        return mapper.findChatByRole(sessionId, null);
    }

    public void insert(ChatMessage entity) {
        mapper.insert(entity);
    }

    public void deleteBySessionId(String sessionId) {
        mapper.deleteBySessionId(sessionId);
    }

    public void deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        mapper.deleteByMessageIdGreaterThanOrEqual(sessionId, messageId);
    }

    public ChatMessage findLatest(String sessionId, String role) {
        return mapper.findLatestByRole(sessionId, role);
    }
}

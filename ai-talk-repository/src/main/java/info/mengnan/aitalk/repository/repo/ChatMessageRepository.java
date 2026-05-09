package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {

    private final ChatMessageMapper mapper;

    public List<ChatMessage> findChatByRole(String sessionId, List<String> roles) {
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

    public void truncateMessagesFrom(String sessionId, Long messageId) {
        mapper.truncateMessagesFrom(sessionId, messageId);
    }
}

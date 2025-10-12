package info.mengnan.aitalk.repository.service;

import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

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
}

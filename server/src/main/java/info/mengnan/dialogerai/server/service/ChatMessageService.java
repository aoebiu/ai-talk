package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatMessage;
import info.mengnan.dialogerai.repository.repo.ChatMessageRagSourceRepository;
import info.mengnan.dialogerai.repository.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageRagSourceRepository chatMessageRagSourceRepository;

    public List<ChatMessage> findBySessionId(String sessionId) {
        return chatMessageRepository.findChat(sessionId);
    }


    public List<ChatMessage> findChat(String sessionId, List<String> roles) {
        return chatMessageRepository.findChat(sessionId, roles);
    }

    public ChatMessage findLatest(String sessionId, String role) {
        return chatMessageRepository.findLatest(sessionId, role);
    }

    public void truncateMessages(String sessionId, Long messageId) {
        chatMessageRepository.deleteByMessageIdGreaterThanOrEqual(sessionId, messageId);
        chatMessageRagSourceRepository.deleteByMessageIdGreaterThanOrEqual(sessionId, messageId);
    }

    public void deleteBySessionId(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }

    public ChatMessage save(ChatMessage chatMessage) {
        chatMessageRepository.insert(chatMessage);
        return chatMessage;
    }
}
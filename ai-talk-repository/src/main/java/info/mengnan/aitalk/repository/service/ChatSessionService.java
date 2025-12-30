package info.mengnan.aitalk.repository.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import info.mengnan.aitalk.repository.entity.ChatSession;
import info.mengnan.aitalk.repository.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper mapper;

    public ChatSession findBySessionId(String sessionId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getChatSessionId, sessionId));
    }

    public void createChat(ChatSession chatSession) {
         mapper.insert(chatSession);
    }

    public void updateChatTitle(String sessionId, String title) {
        ChatSession chatSession = new ChatSession();
        chatSession.setTitle(title);
        mapper.update(chatSession, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getChatSessionId, sessionId));
    }

    public ChatSession findLastByMemberId(Long memberId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getMemberId, memberId).last("limit 1"));
    }
}

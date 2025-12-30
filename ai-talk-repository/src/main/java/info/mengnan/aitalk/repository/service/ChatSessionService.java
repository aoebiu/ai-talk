package info.mengnan.aitalk.repository.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.mengnan.aitalk.repository.entity.ChatSession;
import info.mengnan.aitalk.repository.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper mapper;

    public ChatSession findLastBySessionId(String sessionId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getChatSessionId, sessionId).last("limit 1"));
    }

    public ChatSession createChat(ChatSession chatSession) {
         mapper.insert(chatSession);
         return chatSession;
    }

    public ChatSession findLastByMemberId(Long memberId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getMemberId, memberId).last("limit 1"));
    }
}

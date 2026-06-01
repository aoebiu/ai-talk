package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    default int deleteBySessionId(String sessionId) {
        return delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
    }

    default int deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        return delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .ge(ChatMessage::getId, messageId));
    }

    default List<ChatMessage> findChatByRole(String sessionId, List<String> role) {
        LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .in(role != null, ChatMessage::getRole, role)
                .orderByAsc(ChatMessage::getId);
        return selectList(qw);
    }

    default ChatMessage findLatestByRole(String sessionId, String role) {
        return selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getRole, role)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
    }
}
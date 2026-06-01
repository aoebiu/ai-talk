package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatMessageRagSource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageRagSourceMapper extends BaseMapper<ChatMessageRagSource> {

    default int linkToMessage(String sessionId, Long messageId) {
        return update(null, new LambdaUpdateWrapper<ChatMessageRagSource>()
                .set(ChatMessageRagSource::getMessageId, messageId)
                .eq(ChatMessageRagSource::getSessionId, sessionId)
                .isNull(ChatMessageRagSource::getMessageId));
    }

    default List<ChatMessageRagSource> findForMessage(Long messageId) {
        return selectList(new LambdaQueryWrapper<ChatMessageRagSource>()
                .eq(ChatMessageRagSource::getMessageId, messageId));
    }

    default List<ChatMessageRagSource> find(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<ChatMessageRagSource>()
                .in(ChatMessageRagSource::getId, ids));
    }

    default List<ChatMessageRagSource> findIdAndMessageId(String sessionId) {
        return selectList(new LambdaQueryWrapper<ChatMessageRagSource>()
                .select(ChatMessageRagSource::getId, ChatMessageRagSource::getMessageId)
                .eq(ChatMessageRagSource::getSessionId, sessionId)
                .isNotNull(ChatMessageRagSource::getMessageId));
    }

    default List<ChatMessageRagSource> find(String sessionId) {
        return selectList(new LambdaQueryWrapper<ChatMessageRagSource>()
                .eq(ChatMessageRagSource::getSessionId, sessionId)
                .isNotNull(ChatMessageRagSource::getMessageId));
    }

    default List<ChatMessageRagSource> findPending(String sessionId) {
        return selectList(new LambdaQueryWrapper<ChatMessageRagSource>()
                .eq(ChatMessageRagSource::getSessionId, sessionId)
                .isNull(ChatMessageRagSource::getMessageId));
    }

    default void deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        update(null, new LambdaUpdateWrapper<ChatMessageRagSource>()
                .set(ChatMessageRagSource::getMessageId, null)
                .eq(ChatMessageRagSource::getSessionId, sessionId)
                .lt(ChatMessageRagSource::getMessageId, messageId));
    }
}

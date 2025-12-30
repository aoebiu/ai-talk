package info.mengnan.aitalk.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatApiKeyMapper extends BaseMapper<ChatApiKey> {

    @Delete("DELETE FROM chat_api_key WHERE id = #{id}")
    int deleteById(Long id);

    default ChatApiKey findById(Long id) {
        return selectById(id);
    }

    default List<ChatApiKey> findALl(Long memberId) {
        if (memberId == null) {
            memberId = 0L;
        }
        LambdaQueryWrapper<ChatApiKey> qw = new LambdaQueryWrapper<ChatApiKey>()
                .eq(ChatApiKey::getMemberId, memberId);
        return selectList(qw);
    }

    default List<ChatApiKey> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        LambdaQueryWrapper<ChatApiKey> qw = new LambdaQueryWrapper<ChatApiKey>()
                .in(ChatApiKey::getId, ids);
        return selectList(qw);
    }
}
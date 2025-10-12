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

    default List<ChatApiKey> findByKeyType(String keyType) {
        LambdaQueryWrapper<ChatApiKey> qw = new LambdaQueryWrapper<ChatApiKey>()
                .eq(ChatApiKey::getKeyType, keyType);
        return selectList(qw);
    }

    default ChatApiKey findById(Long id) {
        return selectById(id);
    }

    default List<ChatApiKey> findALl() {
        return selectList(null);
    }
}
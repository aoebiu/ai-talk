package info.mengnan.aitalk.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.aitalk.repository.entity.ChatOptionApiKeyRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatOptionApiKeyRelMapper extends BaseMapper<ChatOptionApiKeyRel> {

    @Delete("DELETE FROM chat_option_api_key_rel WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM chat_option_api_key_rel WHERE chat_option_id = #{chatOptionId}")
    int deleteByChatOptionId(Long chatOptionId);

    @Delete("DELETE FROM chat_option_api_key_rel WHERE chat_api_key_id = #{chatApiKeyId}")
    int deleteByChatApiKeyId(Long chatApiKeyId);

    default List<ChatOptionApiKeyRel> findByChatOptionId(Long chatOptionId) {
        LambdaQueryWrapper<ChatOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatOptionApiKeyRel>()
                .eq(ChatOptionApiKeyRel::getChatOptionId, chatOptionId);
        return selectList(qw);
    }

    default List<ChatOptionApiKeyRel> findByChatApiKeyId(Long chatApiKeyId) {
        LambdaQueryWrapper<ChatOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatOptionApiKeyRel>()
                .eq(ChatOptionApiKeyRel::getChatApiKeyId, chatApiKeyId);
        return selectList(qw);
    }

    default ChatOptionApiKeyRel findByRelation(Long chatOptionId, Long chatApiKeyId) {
        LambdaQueryWrapper<ChatOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatOptionApiKeyRel>()
                .eq(ChatOptionApiKeyRel::getChatOptionId, chatOptionId)
                .eq(ChatOptionApiKeyRel::getChatApiKeyId, chatApiKeyId);
        return selectOne(qw);
    }
}
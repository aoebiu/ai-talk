package info.mengnan.aitalk.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.aitalk.repository.entity.ChatOption;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatOptionMapper extends BaseMapper<ChatOption> {

    @Delete("DELETE FROM chat_option WHERE id = #{id}")
    int deleteById(Long id);

    default ChatOption findById(Long id) {
        return selectById(id);
    }

    default List<ChatOption> findByEnabled(Boolean enabled) {
        LambdaQueryWrapper<ChatOption> qw = new LambdaQueryWrapper<ChatOption>()
                .eq(ChatOption::getEnabled, enabled);
        return selectList(qw);
    }

    default List<ChatOption> findByName(String name) {
        LambdaQueryWrapper<ChatOption> qw = new LambdaQueryWrapper<ChatOption>()
                .like(ChatOption::getName, name);
        return selectList(qw);
    }

    default ChatOption findByNameExact(String name) {
        LambdaQueryWrapper<ChatOption> qw = new LambdaQueryWrapper<ChatOption>()
                .eq(ChatOption::getName, name);
        return selectOne(qw);
    }
}
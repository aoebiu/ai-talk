package info.mengnan.aitalk.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.aitalk.repository.entity.ChatToolDescription;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ToolDescriptionMapper extends BaseMapper<ChatToolDescription> {

    @Delete("DELETE FROM tool_description WHERE id = #{id}")
    int deleteById(Long id);

    default ChatToolDescription findById(Long id) {
        return selectById(id);
    }

    default ChatToolDescription findByName(String name) {
        LambdaQueryWrapper<ChatToolDescription> qw = new LambdaQueryWrapper<ChatToolDescription>()
                .eq(ChatToolDescription::getName, name);
        return selectOne(qw);
    }

    default List<ChatToolDescription> findAll() {
        return selectList(null);
    }
}
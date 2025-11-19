package info.mengnan.aitalk.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.aitalk.repository.entity.ChatProjectApiKey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ChatProjectApiKey> {
}

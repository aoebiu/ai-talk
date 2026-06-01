package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.BizConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BizConfigMapper extends BaseMapper<BizConfig> {

    default List<BizConfig> listByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<BizConfig>()
                .eq(BizConfig::getMemberId, memberId)
                .orderByDesc(BizConfig::getUpdatedAt));
    }

    default BizConfig findByMemberAndKey(Long memberId, String configKey) {
        return selectOne(new LambdaQueryWrapper<BizConfig>()
                .eq(BizConfig::getMemberId, memberId)
                .eq(BizConfig::getConfigKey, configKey));
    }
}

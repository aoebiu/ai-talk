package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.BizConfig;
import info.mengnan.aitalk.repository.mapper.BizConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BizConfigRepository {

    private final BizConfigMapper mapper;

    public List<BizConfig> listByMember(Long memberId) {
        return mapper.listByMemberId(memberId);
    }

    public BizConfig findByMemberAndKey(Long memberId, String configKey) {
        return mapper.findByMemberAndKey(memberId, configKey);
    }

    public void insert(BizConfig entity) {
        mapper.insert(entity);
    }

    public void update(BizConfig entity) {
        mapper.updateById(entity);
    }

    public void deleteByMemberAndKey(Long memberId, String configKey) {
        BizConfig row = mapper.findByMemberAndKey(memberId, configKey);
        if (row != null) {
            mapper.deleteById(row.getId());
        }
    }
}

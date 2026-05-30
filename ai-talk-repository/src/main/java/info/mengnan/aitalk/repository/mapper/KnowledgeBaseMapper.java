package info.mengnan.aitalk.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.aitalk.repository.entity.KnowledgeBase;
import info.mengnan.aitalk.repository.enums.KnowledgeBaseStatus;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    default List<KnowledgeBase> findByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0)
                .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    default List<KnowledgeBase> findActiveByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0)
                .eq(KnowledgeBase::getStatus, KnowledgeBaseStatus.ACTIVE)
                .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    default List<KnowledgeBase> findDraftsOlderThan(Long memberId, LocalDateTime createdBefore) {
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0)
                .eq(KnowledgeBase::getStatus, KnowledgeBaseStatus.DRAFT)
                .lt(KnowledgeBase::getCreatedAt, createdBefore));
    }

    default KnowledgeBase findByIdAndMemberId(Long id, Long memberId) {
        return selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, id)
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0));
    }

    default KnowledgeBase findByIndexName(String indexName) {
        return selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getIndexName, indexName)
                .eq(KnowledgeBase::getDeleted, 0));
    }
}

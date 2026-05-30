package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.KnowledgeBase;
import info.mengnan.aitalk.repository.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeBaseRepository {

    private final KnowledgeBaseMapper mapper;

    public void insert(KnowledgeBase entity) {
        mapper.insert(entity);
    }

    public void updateById(KnowledgeBase entity) {
        mapper.updateById(entity);
    }

    public KnowledgeBase findById(Long id) {
        return mapper.selectById(id);
    }

    public KnowledgeBase findByIdAndMemberId(Long id, Long memberId) {
        return mapper.findByIdAndMemberId(id, memberId);
    }

    public List<KnowledgeBase> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public List<KnowledgeBase> findActiveByMemberId(Long memberId) {
        return mapper.findActiveByMemberId(memberId);
    }

    public List<KnowledgeBase> findDraftsOlderThan(Long memberId, LocalDateTime createdBefore) {
        return mapper.findDraftsOlderThan(memberId, createdBefore);
    }

    public KnowledgeBase findByIndexName(String indexName) {
        return mapper.findByIndexName(indexName);
    }

    public void deleteById(Long id) {
        KnowledgeBase entity = new KnowledgeBase();
        entity.setId(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }
}

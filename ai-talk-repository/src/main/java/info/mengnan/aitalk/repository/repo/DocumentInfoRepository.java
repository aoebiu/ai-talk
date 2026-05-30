package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.DocumentInfo;
import info.mengnan.aitalk.repository.mapper.DocumentInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DocumentInfoRepository {

    private final DocumentInfoMapper mapper;

    public void insert(DocumentInfo entity) {
        mapper.insert(entity);
    }

    public void updateById(DocumentInfo entity) {
        mapper.updateById(entity);
    }

    public DocumentInfo findById(Long id) {
        return mapper.selectById(id);
    }

    public DocumentInfo findByIndexName(String indexName) {
        return mapper.findByIndexName(indexName);
    }

    public List<DocumentInfo> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public List<DocumentInfo> findByKbId(Long kbId) {
        return mapper.findByKbId(kbId);
    }

    public DocumentInfo findByKbIdAndOriginalName(Long kbId, String originalName) {
        return mapper.findByKbIdAndOriginalName(kbId, originalName);
    }

    public long countByKbId(Long kbId) {
        return mapper.countByKbId(kbId);
    }

    public Map<Long, Long> countDocsGroupedByKbIds(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return Map.of();
        }
        return mapper.countDocsByKbIds(kbIds);
    }

    public DocumentInfo findByIdAndMemberId(Long id, Long memberId) {
        return mapper.findByIdAndMemberId(id, memberId);
    }

    public void deleteById(Long id) {
        DocumentInfo entity = new DocumentInfo();
        entity.setId(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }
}

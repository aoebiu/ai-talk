package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.DocumentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface DocumentInfoMapper extends BaseMapper<DocumentInfo> {

    default DocumentInfo findByIndexName(String indexName) {
        return selectOne(new LambdaQueryWrapper<DocumentInfo>()
                .eq(DocumentInfo::getIndexName, indexName)
                .eq(DocumentInfo::getDeleted, 0));
    }

    default List<DocumentInfo> findByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<DocumentInfo>()
                .eq(DocumentInfo::getMemberId, memberId)
                .eq(DocumentInfo::getDeleted, 0)
                .orderByDesc(DocumentInfo::getCreatedAt));
    }

    default List<DocumentInfo> findByKbId(Long kbId) {
        return selectList(new LambdaQueryWrapper<DocumentInfo>()
                .eq(DocumentInfo::getKbId, kbId)
                .eq(DocumentInfo::getDeleted, 0)
                .orderByDesc(DocumentInfo::getCreatedAt));
    }

    default DocumentInfo findByKbIdAndOriginalName(Long kbId, String originalName) {
        return selectOne(new LambdaQueryWrapper<DocumentInfo>()
                .eq(DocumentInfo::getKbId, kbId)
                .eq(DocumentInfo::getOriginalName, originalName)
                .eq(DocumentInfo::getDeleted, 0));
    }

    default long countByKbId(Long kbId) {
        return selectCount(new LambdaQueryWrapper<DocumentInfo>()
                .eq(DocumentInfo::getKbId, kbId)
                .eq(DocumentInfo::getDeleted, 0));
    }

    default DocumentInfo findByIdAndMemberId(Long id, Long memberId) {
        return selectOne(new LambdaQueryWrapper<DocumentInfo>()
                .eq(DocumentInfo::getId, id)
                .eq(DocumentInfo::getMemberId, memberId)
                .eq(DocumentInfo::getDeleted, 0));
    }

    default Map<Long, Long> countDocsByKbIds(List<Long> kbIds) {
        QueryWrapper<DocumentInfo> wrapper = new QueryWrapper<>();
        wrapper.in("kb_id", kbIds)
                .eq("deleted", 0)
                .groupBy("kb_id")
                .select("kb_id", "COUNT(1) as count");
        List<Map<String, Object>> maps = selectMaps(wrapper);

        return maps.stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("kb_id"),
                        m -> (Long) m.get("count")
                ));
    }
}

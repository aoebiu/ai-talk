package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.ChatToolDescription;
import info.mengnan.aitalk.repository.mapper.ToolDescriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ToolDescriptionRepository {

    private final ToolDescriptionMapper mapper;

    public ChatToolDescription findById(Long id) {
        return mapper.findById(id);
    }

    public ChatToolDescription findByNameAndMemberId(String name, Long memberId) {
        return mapper.findByNameAndMemberId(name, memberId);
    }

    public List<ChatToolDescription> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public void insert(ChatToolDescription entity) {
        mapper.insert(entity);
    }

    public void update(ChatToolDescription entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

}
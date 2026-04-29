package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.mapper.ChatApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatApiKeyRepository {

    private final ChatApiKeyMapper mapper;

    public ChatApiKey findById(Long id) {
        return mapper.findById(id);
    }

    public List<ChatApiKey> findByIds(List<Long> id) {
        return mapper.findByIds(id);
    }

    public List<ChatApiKey> findAll(Long memberId) {
        return mapper.findALl(memberId);
    }


    public void insert(ChatApiKey entity) {
        mapper.insert(entity);
    }

    public void update(ChatApiKey entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

}
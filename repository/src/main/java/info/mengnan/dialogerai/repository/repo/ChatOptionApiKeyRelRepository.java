package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.ChatOptionApiKeyRel;
import info.mengnan.dialogerai.repository.mapper.ChatOptionApiKeyRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatOptionApiKeyRelRepository {

    private final ChatOptionApiKeyRelMapper mapper;

    public List<ChatOptionApiKeyRel> findByChatOptionId(Long chatOptionId) {
        return mapper.findByChatOptionId(chatOptionId);
    }

    public List<ChatOptionApiKeyRel> findByChatApiKeyId(Long chatApiKeyId) {
        return mapper.findByChatApiKeyId(chatApiKeyId);
    }

    public ChatOptionApiKeyRel findByRelation(Long chatOptionId, Long chatApiKeyId) {
        return mapper.findByRelation(chatOptionId, chatApiKeyId);
    }

    public void insert(ChatOptionApiKeyRel entity) {
        mapper.insert(entity);
    }

    public void update(ChatOptionApiKeyRel entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public void deleteByChatOptionId(Long chatOptionId) {
        mapper.deleteByChatOptionId(chatOptionId);
    }

    public void deleteByChatApiKeyId(Long chatApiKeyId) {
        mapper.deleteByChatApiKeyId(chatApiKeyId);
    }
}
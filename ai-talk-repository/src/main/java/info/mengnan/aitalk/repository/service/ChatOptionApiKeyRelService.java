package info.mengnan.aitalk.repository.service;

import info.mengnan.aitalk.repository.entity.ChatOptionApiKeyRel;
import info.mengnan.aitalk.repository.mapper.ChatOptionApiKeyRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatOptionApiKeyRelService {

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
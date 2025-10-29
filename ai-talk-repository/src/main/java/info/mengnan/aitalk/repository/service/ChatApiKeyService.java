package info.mengnan.aitalk.repository.service;

import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.mapper.ChatApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatApiKeyService {

    private final ChatApiKeyMapper mapper;

    public ChatApiKey findById(Long id) {
        return mapper.findById(id);
    }

    public List<ChatApiKey> findByIds(List<Long> id) {
        return mapper.findByIds(id);
    }

    public List<ChatApiKey> findAll() {
        return mapper.findALl();
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
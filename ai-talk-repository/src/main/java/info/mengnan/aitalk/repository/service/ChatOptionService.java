package info.mengnan.aitalk.repository.service;

import info.mengnan.aitalk.repository.entity.ChatOption;
import info.mengnan.aitalk.repository.mapper.ChatOptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatOptionService {

    private final ChatOptionMapper mapper;

    public ChatOption findById(Long id) {
        return mapper.findById(id);
    }

    public ChatOption findByNameExact(String name) {
        return mapper.findByNameExact(name);
    }

    public List<ChatOption> findByName(String name) {
        return mapper.findByName(name);
    }

    public List<ChatOption> findByEnabled(Boolean enabled) {
        return mapper.findByEnabled(enabled);
    }

    public List<ChatOption> findAll() {
        return mapper.selectList(null);
    }

    public void insert(ChatOption entity) {
        mapper.insert(entity);
    }

    public void update(ChatOption entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
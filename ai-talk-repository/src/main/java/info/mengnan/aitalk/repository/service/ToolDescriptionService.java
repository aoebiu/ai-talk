package info.mengnan.aitalk.repository.service;

import info.mengnan.aitalk.repository.entity.ChatToolDescription;
import info.mengnan.aitalk.repository.mapper.ToolDescriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolDescriptionService {

    private final ToolDescriptionMapper mapper;

    public ChatToolDescription findById(Long id) {
        return mapper.findById(id);
    }

    public ChatToolDescription findByName(String name) {
        return mapper.findByName(name);
    }

    public List<ChatToolDescription> findAll() {
        return mapper.findAll();
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
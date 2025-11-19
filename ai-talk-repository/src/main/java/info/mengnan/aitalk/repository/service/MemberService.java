package info.mengnan.aitalk.repository.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.mengnan.aitalk.repository.entity.ChatMember;
import info.mengnan.aitalk.repository.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper mapper;

    public ChatMember findById(Long id) {
        return mapper.selectById(id);
    }

    public ChatMember findByUsername(String username) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatMember>()
                .eq(ChatMember::getUsername, username));
    }

    public ChatMember findByPhone(String phone) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatMember>()
                .eq(ChatMember::getPhone, phone));
    }

    public void insert(ChatMember entity) {
        mapper.insert(entity);
    }

    public void updateById(ChatMember entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
package info.mengnan.aitalk.repository.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.mengnan.aitalk.repository.entity.ChatProjectApiKey;
import info.mengnan.aitalk.repository.mapper.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyMapper mapper;

    /**
     * 根据 API Key 查找
     */
    public ChatProjectApiKey findByApiKey(String apiKey) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatProjectApiKey>()
                .eq(ChatProjectApiKey::getApiKey, apiKey));
    }

    /**
     * 验证 API Key 是否有效
     * @param apiKey API Key 字符串
     * @return 验证通过返回 ChatProjectApiKey 对象，否则返回 null
     */
    public ChatProjectApiKey validateApiKey(String apiKey) {
        ChatProjectApiKey key = findByApiKey(apiKey);

        if (key == null) {
            return null;
        }

        // 检查状态
        if (key.getStatus() != 1) {
            return null;
        }

        // 检查是否过期
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        // 更新最后使用时间
        key.setLastUsedAt(LocalDateTime.now());
        mapper.updateById(key);

        return key;
    }

    /**
     * 创建新的 API Key
     */
    public void insert(ChatProjectApiKey chatProjectApiKey) {
        mapper.insert(chatProjectApiKey);
    }

    /**
     * 更新 API Key
     */
    public void updateById(ChatProjectApiKey chatProjectApiKey) {
        mapper.updateById(chatProjectApiKey);
    }

    /**
     * 删除 API Key
     */
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}

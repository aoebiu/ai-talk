package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatProjectApiKey;
import info.mengnan.aitalk.repository.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API Key 管理控制器
 * 用户可以通过此接口创建和管理自己的 OpenAI API Key
 */
@Slf4j
@RestController
@RequestMapping("/api/apikey")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * 创建新的 API Key
     * @param name API Key 的名称/描述
     * @param expiresInDays 过期天数（可选，不传则永不过期）
     */
    @PostMapping("/create")
    public Map<String, Object> createApiKey(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer expiresInDays) {

        Long memberId = StpUtil.getLoginIdAsLong();

        String apiKey = "sk-" + UUID.randomUUID().toString().replace("-", "");

        ChatProjectApiKey entity = new ChatProjectApiKey();
        entity.setApiKey(apiKey);
        entity.setMemberId(memberId);
        entity.setName(name);
        entity.setStatus(1);

        if (expiresInDays != null && expiresInDays > 0) {
            entity.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }

        apiKeyService.insert(entity);

        Map<String, Object> result = new HashMap<>();
        result.put("id", entity.getId());
        result.put("apiKey", apiKey);
        result.put("name", name);
        result.put("expiresAt", entity.getExpiresAt());
        result.put("createdAt", entity.getCreatedAt());

        log.info("User {} created API Key: {}", memberId, entity.getId());
        return result;
    }

    /**
     * 禁用 API Key
     */
    @PostMapping("/disable/{id}")
    public Map<String, String> disableApiKey(@PathVariable Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey chatProjectApiKey = apiKeyService.findByApiKey(
                apiKeyService.findByApiKey(null) != null ? null : ""
        );
        // TODO: 添加权限检查，确保只能禁用自己的 API Key

        if (chatProjectApiKey != null) {
            chatProjectApiKey.setStatus(0);
            apiKeyService.updateById(chatProjectApiKey);
        }

        Map<String, String> result = new HashMap<>();
        result.put("message", "API Key 已禁用");
        return result;
    }

    /**
     * 删除 API Key（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Map<String, String> deleteApiKey(@PathVariable Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        // TODO: 添加权限检查，确保只能删除自己的 API Key

        apiKeyService.deleteById(id);

        Map<String, String> result = new HashMap<>();
        result.put("message", "API Key 已删除");
        return result;
    }
}

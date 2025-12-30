package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatProjectApiKey;
import info.mengnan.aitalk.repository.service.ApiKeyService;
import info.mengnan.aitalk.server.param.R;
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
    public R createApiKey(
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
        log.info("User {} created API Key: {}", memberId, entity.getId());
        return R.ok();
    }

    /**
     * 禁用 API Key
     */
    @PostMapping("/disable/{id}")
    public R disableApiKey(@PathVariable Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = apiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }
        // TODO: 添加权限检查，确保只能禁用自己的 API Key

        projectApiKey.setStatus(0);
        apiKeyService.updateById(projectApiKey);
        return R.ok();
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = apiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }
        apiKeyService.deleteById(id);
        return R.ok();
    }
}

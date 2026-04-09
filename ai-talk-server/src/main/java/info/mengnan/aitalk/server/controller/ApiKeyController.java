package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatProjectApiKey;
import info.mengnan.aitalk.repository.service.ProjectApiKeyService;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.server.vo.ProjectApiKeyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API Key 管理控制器
 * 管理本项目创建的 API Key（chat_project_api_key 表）
 * 区别于 chat_model_api_key（外部模型的 API Key）
 */
@Slf4j
@RestController
@RequestMapping("/api/apikey")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ProjectApiKeyService projectApiKeyService;

    /**
     * 获取当前用户的 API Key 列表（列表中 key 脱敏显示）
     */
    @GetMapping("/list")
    public R listApiKeys() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatProjectApiKey> keys = projectApiKeyService.listByMemberId(memberId);
        List<ProjectApiKeyVO> list = keys.stream().map(k -> {
            ProjectApiKeyVO vo = new ProjectApiKeyVO();
            vo.setId(k.getId());
            vo.setName(k.getName());
            vo.setStatus(k.getStatus());
            vo.setExpiresAt(k.getExpiresAt());
            vo.setLastUsedAt(k.getLastUsedAt());
            vo.setCreatedAt(k.getCreatedAt());
            vo.setApiKey(maskKey(k.getApiKey()));
            return vo;
        }).toList();
        return R.ok(list);
    }

    private static String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "sk-****";
        return "sk-****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 创建新的 API Key
     * @param name API Key 的名称/描述
     * @param expiresInDays 过期天数（可选，不传则永不过期）
     */
    @PostMapping("/create")
    public R createApiKey(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "expiresInDays", required = false) Integer expiresInDays) {

        Long memberId = StpUtil.getLoginIdAsLong();

        String apiKey = "sk-" + UUID.randomUUID().toString().replace("-", "");

        ChatProjectApiKey entity = new ChatProjectApiKey();
        entity.setApiKey(apiKey);
        entity.setMemberId(memberId);
        entity.setName(StringUtils.hasText(name) ? name.trim() : null);
        entity.setStatus(1);

        if (expiresInDays != null && expiresInDays > 0) {
            entity.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }
        projectApiKeyService.insert(entity);
        log.info("User {} created API Key: {}", memberId, entity.getId());

        ProjectApiKeyVO vo = new ProjectApiKeyVO();
        vo.setId(entity.getId());
        vo.setApiKey(apiKey);
        vo.setName(entity.getName());
        vo.setExpiresAt(entity.getExpiresAt());
        return R.ok(vo);
    }

    /**
     * 禁用 API Key
     */
    @PostMapping("/disable/{id}")
    public R disableApiKey(@PathVariable("id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = projectApiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }

        projectApiKey.setStatus(0);
        projectApiKeyService.updateById(projectApiKey);
        return R.ok();
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable("id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = projectApiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }
        projectApiKeyService.deleteById(id);
        return R.ok();
    }
}
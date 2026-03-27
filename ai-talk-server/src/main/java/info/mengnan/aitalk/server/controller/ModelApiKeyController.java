package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.service.ChatApiKeyService;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.server.vo.ModelApiKeyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型 API Key 控制器
 * 用于管理外部模型的 API Key（chat_api_key 表）
 * 区别于 chat_project_api_key（本项目创建的 API Key）
 */
@Slf4j
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelApiKeyController {

    private final ChatApiKeyService chatApiKeyService;

    /**
     * 获取当前用户的模型列表
     */
    @GetMapping("/list")
    public R listModels() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatApiKey> keys = chatApiKeyService.findAll(memberId);
        List<ModelApiKeyVO> list = keys.stream().map(k -> {
            ModelApiKeyVO vo = new ModelApiKeyVO();
            vo.setId(k.getId());
            vo.setModelName(k.getModelName());
            vo.setModelProvider(k.getModelProvider());
            vo.setKeyType(k.getKeyType());
            vo.setMaskedApiKey(maskApiKey(k.getApiKey()));
            vo.setCreatedAt(k.getCreatedAt());
            return vo;
        }).toList();
        return R.ok(list);
    }

    /**
     * 创建新的模型 API Key
     */
    @PostMapping("/create")
    public R createApiKey(@RequestParam(name = "modelName") String modelName,
                          @RequestParam(name = "modelProvider") String modelProvider,
                          @RequestParam(name = "keyType") String keyType,
                          @RequestParam(name = "apiKey") String apiKey) {

        Long memberId = StpUtil.getLoginIdAsLong();

        ChatApiKey entity = new ChatApiKey();
        entity.setMemberId(memberId);
        entity.setModelName(modelName);
        entity.setModelProvider(modelProvider);
        entity.setKeyType(keyType);
        entity.setApiKey(apiKey);

        chatApiKeyService.insert(entity);
        log.info("User {} created Model API Key: {} for model {}", memberId, entity.getId(), modelName);

        ModelApiKeyVO vo = new ModelApiKeyVO();
        vo.setId(entity.getId());
        vo.setModelName(entity.getModelName());
        vo.setModelProvider(entity.getModelProvider());
        vo.setKeyType(entity.getKeyType());
        vo.setCreatedAt(entity.getCreatedAt());
        return R.ok(vo);
    }

    /**
     * 删除模型 API Key
     */
    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatApiKey chatApiKey = chatApiKeyService.findById(id);
        if (chatApiKey == null || !memberId.equals(chatApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }

        chatApiKeyService.deleteById(id);
        return R.ok();
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
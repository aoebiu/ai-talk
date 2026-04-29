package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.common.crypto.ConfigValueCrypto;
import info.mengnan.aitalk.repository.entity.BizConfig;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.server.param.config.AppConfigItemResponse;
import info.mengnan.aitalk.server.param.config.ConfigSaveRequest;
import info.mengnan.aitalk.server.service.BizConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 应用配置
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final BizConfigService bizConfigService;

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<AppConfigItemResponse> list = bizConfigService.listByMember(memberId).stream()
                .map(ConfigController::toMaskedResponse)
                .toList();
        return R.ok(list);
    }

    @GetMapping("/{key}")
    public R getByKey(@PathVariable("key") String key) {
        Long memberId = StpUtil.getLoginIdAsLong();
        Optional<BizConfig> item = bizConfigService.find(memberId, key);
        return item.map(row -> R.ok(toDetailResponse(row))).orElseGet(() -> R.error("配置不存在"));
    }

    @PutMapping("/{key}")
    public R save(@PathVariable("key") String key, @Valid @RequestBody ConfigSaveRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        request.setConfigKey(key);
        try {
            BizConfig saved = bizConfigService.save(memberId, request);
            log.info("Member {} saved app config key={} encryptStorage={}", memberId, key,
                    request.isEncryptStorage());
            return R.ok(toMaskedResponse(saved));
        } catch (IllegalStateException e) {
            log.warn("Save app config failed: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    @DeleteMapping("/{key}")
    public R delete(@PathVariable("key") String key) {
        Long memberId = StpUtil.getLoginIdAsLong();
        bizConfigService.delete(memberId, key);
        return R.ok();
    }

    private static AppConfigItemResponse toMaskedResponse(BizConfig row) {
        AppConfigItemResponse r = new AppConfigItemResponse();
        r.setId(row.getId());
        r.setConfigKey(row.getConfigKey());
        r.setEncryptStorage(Boolean.TRUE.equals(row.getEncryptStorage()));
        r.setRemark(row.getRemark());
        r.setCreatedAt(row.getCreatedAt());
        r.setUpdatedAt(row.getUpdatedAt());
        String plainForMask = row.getConfigValue();
        if (Boolean.TRUE.equals(row.getEncryptStorage())) {
            try {
                plainForMask = ConfigValueCrypto.decrypt(row.getConfigValue());
            } catch (Exception ignored) {
                plainForMask = "****";
            }
        }
        r.setDisplayValue(maskSecret(plainForMask));
        return r;
    }

    private static AppConfigItemResponse toDetailResponse(BizConfig row) {
        AppConfigItemResponse r = toMaskedResponse(row);
        String plainValue = row.getConfigValue();
        if (Boolean.TRUE.equals(row.getEncryptStorage())) {
            try {
                plainValue = ConfigValueCrypto.decrypt(row.getConfigValue());
            } catch (Exception ignored) {
                plainValue = null;
            }
        }
        r.setConfigValue(plainValue);
        return r;
    }

    private static String maskSecret(String value) {
        if (value == null || value.isEmpty()) {
            return "****";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}

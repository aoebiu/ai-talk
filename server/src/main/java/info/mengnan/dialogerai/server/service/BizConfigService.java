package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.crypto.ConfigValueCrypto;
import info.mengnan.dialogerai.repository.entity.BizConfig;
import info.mengnan.dialogerai.repository.repo.BizConfigRepository;
import info.mengnan.dialogerai.server.param.config.ConfigSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BizConfigService {

    private final BizConfigRepository bizConfigRepository;

    public List<BizConfig> listByMember(Long memberId) {
        return bizConfigRepository.listByMember(memberId);
    }

    public Optional<BizConfig> find(Long memberId, String configKey) {
        BizConfig row = bizConfigRepository.findByMemberAndKey(memberId, configKey);
        return Optional.ofNullable(row);
    }

    public BizConfig save(Long memberId, ConfigSaveRequest request) {
        String stored = request.isEncryptStorage()
                ? ConfigValueCrypto.encrypt(request.getConfigValue())
                : request.getConfigValue();

        BizConfig existing = bizConfigRepository.findByMemberAndKey(memberId, request.getConfigKey());
        if (existing == null) {
            BizConfig entity = new BizConfig();
            entity.setMemberId(memberId);
            entity.setConfigKey(request.getConfigKey());
            entity.setConfigValue(stored);
            entity.setEncryptStorage(request.isEncryptStorage());
            entity.setRemark(request.getRemark());
            bizConfigRepository.insert(entity);
            return entity;
        }
        existing.setConfigValue(stored);
        existing.setEncryptStorage(request.isEncryptStorage());
        if (request.getRemark() != null) {
            existing.setRemark(request.getRemark());
        }
        bizConfigRepository.update(existing);
        return existing;
    }

    public void delete(Long memberId, String configKey) {
        bizConfigRepository.deleteByMemberAndKey(memberId, configKey);
    }

    /**
     * 供业务代码读取明文配置
     */
    public String getPlainValue(Long memberId, String configKey) {
        BizConfig row = bizConfigRepository.findByMemberAndKey(memberId, configKey);
        if (row == null) return null;

        if (Boolean.TRUE.equals(row.getEncryptStorage()))
            return ConfigValueCrypto.decrypt(row.getConfigValue());

        return row.getConfigValue();
    }
}

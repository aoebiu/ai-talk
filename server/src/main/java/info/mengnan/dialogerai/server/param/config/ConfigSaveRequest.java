package info.mengnan.dialogerai.server.param.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigSaveRequest {

    @Size(max = 191)
    private String configKey;

    @NotBlank
    @Size(max = 8000)
    private String configValue;

    /**
     * 是否在写入数据库前加密（使用 {@link info.mengnan.dialogerai.common.crypto.ConfigValueCrypto} 内置密钥）
     */
    private boolean encryptStorage;

    @Size(max = 500)
    private String remark;
}

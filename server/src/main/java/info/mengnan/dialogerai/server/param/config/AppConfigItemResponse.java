package info.mengnan.dialogerai.server.param.config;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppConfigItemResponse {

    private Long id;
    private String configKey;
    /**
     * 脱敏后的展示值，非明文
     */
    private String displayValue;
    /**
     * 明文配置值，仅详情接口返回
     */
    private String configValue;
    private boolean encryptStorage;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

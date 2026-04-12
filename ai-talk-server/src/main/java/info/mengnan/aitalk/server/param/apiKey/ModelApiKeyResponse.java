package info.mengnan.aitalk.server.param.apiKey;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模型 API Key 视图对象
 */
@Data
public class ModelApiKeyResponse {

    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型提供商
     */
    private String modelProvider;

    /**
     * API Key 类型
     */
    private String keyType;

    /**
     * 脱敏后的 API Key
     */
    private String maskedApiKey;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
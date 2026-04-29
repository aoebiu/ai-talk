package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_config")
public class BizConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    /**
     * 配置键，如 amap.web_service_key
     */
    private String configKey;

    /**
     * 配置值；当 encryptStorage 为 true 时为 AES-GCM 密文（Base64）
     */
    private String configValue;

    /**
     * 是否在落库前加密存储
     */
    private Boolean encryptStorage;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

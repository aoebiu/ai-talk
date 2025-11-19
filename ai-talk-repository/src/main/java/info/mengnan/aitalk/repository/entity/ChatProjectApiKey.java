package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 本项目生成的API Key 实体类
 * 用于 OpenAI 兼容接口的鉴权
 */
@Data
@TableName("chat_project_api_key")
public class ChatProjectApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 所属用户 ID
     */
    private Long memberId;

    /**
     * API Key 名称/描述
     */
    private String name;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 过期时间,无过期时间则为null
     */
    private LocalDateTime expiresAt;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
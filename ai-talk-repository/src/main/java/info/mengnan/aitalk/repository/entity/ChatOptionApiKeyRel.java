package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * chat_option 和 chat_api_key 的关联表
 * 用于表示用户的聊天模型配置启用了哪些模型（多对多关系）
 */
@Data
@TableName("chat_option_api_key_rel")
public class ChatOptionApiKeyRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 聊天配置ID
     */
    private Long chatOptionId;

    /**
     * API Key配置ID
     */
    private Long chatApiKeyId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
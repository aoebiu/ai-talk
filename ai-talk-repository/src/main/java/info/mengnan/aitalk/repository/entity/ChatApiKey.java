package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_api_key")
public class ChatApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API Key类型: chat, streaming_chat, embedding
     */
    private String keyType;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型提供商
     */
    private String modelProvider;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
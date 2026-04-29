package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("direct_model_invoke_log")
public class DirectModelInvokeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 调用来源标识，如 ChatController.conversations
     */
    private String invokeSource;

    private String templateName;

    private String promptText;

    private String modelName;

    private String modelProvider;

    private String responseText;

    private Boolean success;

    private String errorMessage;

    private Long durationMs;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

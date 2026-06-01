package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message_rag_source")
public class ChatMessageRagSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 chat_messages.id，由 updateMessages 阶段写入 */
    private Long messageId;

    /** 会话ID，用于 inject → updateMessages 两阶段关联 */
    private String sessionId;

    private String kbName;

    private String indexName;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

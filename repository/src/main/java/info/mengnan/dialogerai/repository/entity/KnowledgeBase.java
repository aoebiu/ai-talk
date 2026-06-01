package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import info.mengnan.dialogerai.repository.enums.KnowledgeBaseStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private String name;

    private String description;

    /** private / public */
    private String visibility;

    /** DRAFT：新建向导草稿；ACTIVE：正式知识库 */
    private KnowledgeBaseStatus status;

    /** ES 索引名：{memberId}_kb_{id} */
    private String indexName;

    /** 当前知识库构建任务 ID（async_task.task_id） */
    private String buildTaskId;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

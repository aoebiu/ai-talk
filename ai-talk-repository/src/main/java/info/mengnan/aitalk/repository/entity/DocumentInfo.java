package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_info")
public class DocumentInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    /** 所属知识库 ID */
    private Long kbId;

    /** 关联 async_task.task_id */
    private String taskId;

    /** 用户上传的原始文件名 */
    private String originalName;

    /** 磁盘存储文件名（{memberId}_{uuid}{ext}，防止重名） */
    private String storedName;

    /** ES 索引名（与所属知识库 index_name 一致，一库一索引） */
    private String indexName;

    /** 文件扩展名，如 .pdf / .docx */
    private String fileType;

    /** 文档语义类型：short_text / paper / contract / novel */
    private String docType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 清洗规则配置 JSON */
    private String cleaningConfig;

    /** 当前处理状态：PENDING / PARSING / CLEANING / CHUNKING / EMBEDDING / DONE / FAILED */
    private String status;

    /** 解析后原始字符数（PARSING 完成后写入） */
    private Integer originalCharCount;

    /** 清洗后字符数（CLEANING 完成后写入） */
    private Integer cleanedCharCount;

    /** 分块总数（CHUNKING 完成后写入） */
    private Integer totalChunks;

    /** 已完成向量化的分块数（EMBEDDING 阶段滚动更新） */
    private Integer processedChunks;

    /** 最近一次失败的错误原因 */
    private String errorMessage;

    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

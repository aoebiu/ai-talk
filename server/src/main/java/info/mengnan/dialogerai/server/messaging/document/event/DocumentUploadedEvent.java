package info.mengnan.dialogerai.server.messaging.document.event;

import info.mengnan.dialogerai.server.param.document.CleaningConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档上传完成事件（文件已落盘、MySQL 记录已创建）。
 * 由 DocumentController 发布，触发 ParsingListener 开始解析。
 */
@Getter
@RequiredArgsConstructor
public class DocumentUploadedEvent {

    /** document_info.id */
    private final Long documentId;

    private final Long memberId;

    /** async_task.task_id */
    private final String taskId;

    /** 存储的文件名（storedName），消费者需通过 FileStorage.resolvePath 获取实际路径 */
    private final String storedName;

    /** 存储类型：LOCAL / OSS */
    private final String storageType;

    /** 文件扩展名，如 .pdf / .docx */
    private final String fileType;

    /** 文档语义类型：short_text / paper / contract / novel */
    private final String docType;

    /** 用户配置的清洗规则 */
    private final CleaningConfig cleaningConfig;
}

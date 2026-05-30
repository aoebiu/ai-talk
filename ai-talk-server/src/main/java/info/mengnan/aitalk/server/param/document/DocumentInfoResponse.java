package info.mengnan.aitalk.server.param.document;

import info.mengnan.aitalk.repository.entity.DocumentInfo;
import info.mengnan.aitalk.repository.enums.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档列表项响应 DTO
 */
@Data
public class DocumentInfoResponse {

    private Long id;
    private Long kbId;
    private String originalName;
    private String indexName;
    private String fileType;
    private String docType;
    private Long fileSize;

    /** 当前处理状态：PENDING/PARSING/CLEANING/CHUNKING/EMBEDDING/DONE/FAILED */
    private String status;

    private Integer totalChunks;
    private Integer processedChunks;

    /** 处理进度 0-100 */
    private Integer progress;

    private String errorMessage;
    private String taskId;
    private LocalDateTime createdAt;

    public static DocumentInfoResponse from(DocumentInfo doc) {
        DocumentInfoResponse resp = new DocumentInfoResponse();
        resp.setId(doc.getId());
        resp.setKbId(doc.getKbId());
        resp.setOriginalName(doc.getOriginalName());
        resp.setIndexName(doc.getIndexName());
        resp.setFileType(doc.getFileType());
        resp.setDocType(doc.getDocType());
        resp.setFileSize(doc.getFileSize());
        resp.setStatus(doc.getStatus());
        resp.setTotalChunks(doc.getTotalChunks());
        resp.setProcessedChunks(doc.getProcessedChunks());
        resp.setProgress(calcProgress(doc));
        resp.setErrorMessage(doc.getErrorMessage());
        resp.setTaskId(doc.getTaskId());
        resp.setCreatedAt(doc.getCreatedAt());
        return resp;
    }

    private static int calcProgress(DocumentInfo doc) {
        if (doc.getStatus() == null) return 0;
        DocumentStatus status;
        try {
            status = DocumentStatus.valueOf(doc.getStatus());
        } catch (IllegalArgumentException e) {
            return 0;
        }
        return switch (status) {
            case PENDING, FAILED -> 0;
            case PARSING   -> 10;
            case CLEANING  -> 30;
            case CHUNKING  -> 55;
            case EMBEDDING -> {
                if (doc.getTotalChunks() != null && doc.getTotalChunks() > 0
                        && doc.getProcessedChunks() != null) {
                    yield 60 + (doc.getProcessedChunks() * 40 / doc.getTotalChunks());
                }
                yield 60;
            }
            case DONE      -> 100;
        };
    }
}

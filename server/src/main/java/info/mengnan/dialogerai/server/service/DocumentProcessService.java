package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.DocumentInfo;
import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.enums.DocumentStatus;
import info.mengnan.dialogerai.repository.repo.DocumentInfoRepository;
import info.mengnan.dialogerai.repository.repo.KnowledgeBaseRepository;
import info.mengnan.dialogerai.server.core.storage.FileUploadStorage;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.messaging.document.event.DocumentUploadedEvent;
import info.mengnan.dialogerai.server.param.document.CleaningConfig;
import info.mengnan.dialogerai.server.param.document.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文档处理过程中的状态管理，供四个阶段 Listener 共享复用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessService {

    private final DocumentInfoRepository documentInfoRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseBuildService knowledgeBaseBuildService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileUploadStorage fileStorage;

    public DocumentUploadResponse upload(MultipartFile file, Long kbId, String type, String cleaningJson, Long memberId) throws IOException {
        String originalFilename = file.getOriginalFilename();

        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndMemberId(kbId, memberId);
        if (kb == null)
            throw new BusinessException(ErrorCode.KB_NOT_FOUND);

        if (documentInfoRepository.findByKbIdAndOriginalName(kbId, originalFilename) != null)
            throw new BusinessException(ErrorCode.DOC_DUPLICATE);

        FileUploadStorage.StoredFile storedFile = fileStorage.store(file.getInputStream(), originalFilename, memberId);
        String fileType = getFileExtension(originalFilename);
        CleaningConfig cleaningConfig = CleaningConfig.fromJson(cleaningJson);

        DocumentInfo docInfo = buildDocumentInfo(memberId, kbId, originalFilename, storedFile, fileType, type, cleaningConfig, file.getSize(), kb.getIndexName());
        documentInfoRepository.insert(docInfo);

        String taskId = knowledgeBaseBuildService.ensureBuildTask(kbId, memberId);
        docInfo.setTaskId(taskId);
        documentInfoRepository.updateById(docInfo);
        knowledgeBaseBuildService.refreshBuildProgress(kbId);

        eventPublisher.publishEvent(new DocumentUploadedEvent(
                docInfo.getId(), memberId, taskId,
                storedFile.getStoredName(), storedFile.getStorageType(),
                fileType, type, cleaningConfig
        ));

        log.info("document uploaded to kb: kbId={}, originalName={}, documentId={}, taskId={}",
                kbId, originalFilename, docInfo.getId(), taskId);

        return new DocumentUploadResponse(docInfo.getId(), kbId, taskId, originalFilename);
    }

    /**
     * 更新 document_info.status。
     */
    public void updateStatus(Long documentId, DocumentStatus status) {
        DocumentInfo info = documentInfoRepository.findById(documentId);
        if (info == null) {
            log.warn("updateStatus: documentId={} 不存在", documentId);
            return;
        }
        info.setStatus(status.name());
        documentInfoRepository.updateById(info);
    }

    /**
     * 将文档标记为失败：更新 document_info 状态和错误信息，同时通知 async_task。
     */
    public void markFailed(Long documentId, String taskId, String errorMessage) {
        DocumentInfo info = documentInfoRepository.findById(documentId);
        if (info != null) {
            info.setStatus(DocumentStatus.FAILED.name());
            info.setErrorMessage(errorMessage);
            documentInfoRepository.updateById(info);
        }
        if (info != null && info.getKbId() != null) {
            knowledgeBaseBuildService.refreshBuildProgress(info.getKbId());
        }
        log.error("document processing failed: documentId={}, taskId={}, reason={}", documentId, taskId, errorMessage);
    }

    private DocumentInfo buildDocumentInfo(Long memberId, Long kbId, String originalFilename,
                                           FileUploadStorage.StoredFile storedFile, String fileType,
                                           String type, CleaningConfig cleaningConfig,
                                           long fileSize, String indexName) {
        DocumentInfo docInfo = new DocumentInfo();
        docInfo.setMemberId(memberId);
        docInfo.setKbId(kbId);
        docInfo.setOriginalName(originalFilename);
        docInfo.setStoredName(storedFile.getStoredName());
        docInfo.setIndexName(indexName);
        docInfo.setFileType(fileType);
        docInfo.setDocType(type);
        docInfo.setFileSize(fileSize);
        docInfo.setCleaningConfig(cleaningConfig.toJsonString());
        docInfo.setStatus(DocumentStatus.PENDING.name());
        docInfo.setProcessedChunks(0);
        return docInfo;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}

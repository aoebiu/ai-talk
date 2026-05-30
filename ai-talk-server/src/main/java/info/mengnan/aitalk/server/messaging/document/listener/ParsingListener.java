package info.mengnan.aitalk.server.messaging.document.listener;

import dev.langchain4j.data.document.Document;
import info.mengnan.aitalk.kb.core.SequentialDocumentExtractor;
import info.mengnan.aitalk.kb.param.ContentElement;
import info.mengnan.aitalk.repository.entity.DocumentInfo;
import info.mengnan.aitalk.repository.enums.DocumentStatus;
import info.mengnan.aitalk.repository.repo.DocumentInfoRepository;
import info.mengnan.aitalk.server.core.DocumentEmbedding;
import info.mengnan.aitalk.server.core.storage.FileUploadStorage;
import info.mengnan.aitalk.server.messaging.document.event.DocumentParsedEvent;
import info.mengnan.aitalk.server.messaging.document.event.DocumentUploadedEvent;
import info.mengnan.aitalk.server.service.DocumentProcessService;
import info.mengnan.aitalk.server.service.KnowledgeBaseBuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 阶段1：文档解析。
 * 监听 DocumentUploadedEvent，从磁盘文件中提取文本和图片内容元素。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParsingListener {

    private final SequentialDocumentExtractor sequentialExtractor;
    private final DocumentEmbedding documentEmbedding;
    private final DocumentInfoRepository documentInfoRepository;
    private final DocumentProcessService documentProcessService;
    private final KnowledgeBaseBuildService KnowledgeBaseBuildService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileUploadStorage fileStorage;

    @Async("docProcessPool")
    @EventListener
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        Long documentId = event.getDocumentId();
        String taskId = event.getTaskId();

        try {
            documentProcessService.updateStatus(documentId, DocumentStatus.PARSING);
            refreshKbBuildProgress(documentId);

            Path filePath = fileStorage.resolvePath(event.getStoredName());
            List<ContentElement> elements = sequentialExtractor.extractContentSequentially(filePath, event.getFileType());
            Document document = documentEmbedding.parseDocument(filePath, event.getFileType());
            String parsedText = document.text();
            int charCount = parsedText != null ? parsedText.length() : 0;

            // 顺序提取器目前只支持 docx/pptx；pdf/txt 等类型走通用解析文本兜底。
            boolean hasTextElement = elements.stream()
                    .anyMatch(e -> e.getType() == ContentElement.Type.TEXT && StringUtils.hasText(e.getText()));
            if (!hasTextElement && StringUtils.hasText(parsedText)) {
                List<ContentElement> merged = new ArrayList<>(elements);
                merged.add(ContentElement.ofText(parsedText, merged.size()));
                elements = merged;
            }

            long imageCount = elements.stream()
                    .filter(e -> e.getType() == ContentElement.Type.IMAGE)
                    .count();

            DocumentInfo info = documentInfoRepository.findById(documentId);
            info.setOriginalCharCount(charCount);
            documentInfoRepository.updateById(info);

            refreshKbBuildProgress(documentId);

            eventPublisher.publishEvent(new DocumentParsedEvent(
                    documentId, event.getMemberId(), taskId,
                    event.getDocType(), event.getCleaningConfig(), elements
            ));

        } catch (Exception e) {
            log.error("document parsing failed, documentId={}", documentId, e);
            documentProcessService.markFailed(documentId, taskId, "解析失败: " + e.getMessage());
        } finally {
            try {
                fileStorage.delete(event.getStoredName());
            } catch (Exception ignore) {
                log.warn("temporary file deletion failed: storedName={}", event.getStoredName());
            }
        }
    }

    private void refreshKbBuildProgress(Long documentId) {
        DocumentInfo info = documentInfoRepository.findById(documentId);
        if (info != null && info.getKbId() != null) {
            KnowledgeBaseBuildService.refreshBuildProgress(info.getKbId());
        }
    }
}

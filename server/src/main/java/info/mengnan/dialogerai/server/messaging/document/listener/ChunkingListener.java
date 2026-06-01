package info.mengnan.dialogerai.server.messaging.document.listener;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import info.mengnan.dialogerai.kb.param.ContentElement;
import info.mengnan.dialogerai.kb.param.DocumentImage;
import info.mengnan.dialogerai.repository.entity.DocumentInfo;
import info.mengnan.dialogerai.repository.enums.DocumentStatus;
import info.mengnan.dialogerai.repository.repo.DocumentInfoRepository;
import info.mengnan.dialogerai.server.core.DocumentEmbedding;
import info.mengnan.dialogerai.server.messaging.document.event.DocumentChunkedEvent;
import info.mengnan.dialogerai.server.messaging.document.event.DocumentCleanedEvent;
import info.mengnan.dialogerai.server.service.DocumentProcessService;
import info.mengnan.dialogerai.server.service.KnowledgeBaseBuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 阶段3：文档分块。
 * 监听 DocumentCleanedEvent，将清洗后的内容合并为 LangChain4j Document，
 * 按 docType 选择分块策略，并创建携带图片元数据的增强 TextSegment 列表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkingListener {

    private final DocumentEmbedding documentEmbedding;
    private final DocumentInfoRepository documentInfoRepository;
    private final KnowledgeBaseBuildService KnowledgeBaseBuildService;
    private final DocumentProcessService documentProcessService;
    private final ApplicationEventPublisher eventPublisher;

    @Async("docProcessPool")
    @EventListener
    public void onDocumentCleaned(DocumentCleanedEvent event) {
        Long documentId = event.getDocumentId();
        String taskId = event.getTaskId();

        try {
            documentProcessService.updateStatus(documentId, DocumentStatus.CHUNKING);
            DocumentInfo docRef = documentInfoRepository.findById(documentId);
            if (docRef != null) {
                KnowledgeBaseBuildService.refreshBuildProgress(docRef.getKbId());
            }

            List<ContentElement> elements = event.getContentElements();

            String combinedText = elements.stream()
                    .filter(e -> e.getType() == ContentElement.Type.TEXT && StringUtils.hasText(e.getText()))
                    .map(ContentElement::getText)
                    .collect(Collectors.joining("\n\n"));
            if (!StringUtils.hasText(combinedText)) {
                throw new IllegalArgumentException("文档清洗后无可用文本，请检查文档内容或调整清洗规则");
            }

            List<DocumentImage> images = elements.stream()
                    .filter(e -> e.getType() == ContentElement.Type.IMAGE && e.getImage() != null)
                    .map(ContentElement::getImage)
                    .collect(Collectors.toList());

            Document document = Document.from(combinedText);

            DocumentSplitter splitter = switch (event.getDocType().toLowerCase()) {
                case "short_text" -> DocumentSplitters.recursive(150, 20);
                case "paper"      -> DocumentSplitters.recursive(400, 40);
                case "contract"   -> DocumentSplitters.recursive(300, 0);
                case "novel"      -> DocumentSplitters.recursive(750, 50);
                default           -> DocumentSplitters.recursive(300, 50);
            };

            List<TextSegment> textSegments = splitter.split(document).stream()
                    .filter(segment -> StringUtils.hasText(segment.text()))
                    .collect(Collectors.toList());
            if (textSegments.isEmpty()) {
                throw new IllegalArgumentException("文档分块结果为空，请检查文档内容");
            }
            List<TextSegment> enhanced = documentEmbedding.createEnhancedSegmentsWithPosition(textSegments, images);
            String documentIdStr = String.valueOf(documentId);
            for (TextSegment segment : enhanced) {
                segment.metadata().put("document_id", documentIdStr);
            }

            DocumentInfo info = documentInfoRepository.findById(documentId);
            info.setTotalChunks(enhanced.size());
            info.setProcessedChunks(0);
            documentInfoRepository.updateById(info);

            KnowledgeBaseBuildService.refreshBuildProgress(info.getKbId());

            eventPublisher.publishEvent(new DocumentChunkedEvent(
                    documentId, event.getMemberId(), taskId, info.getIndexName(), enhanced
            ));

        } catch (Exception e) {
            log.error("文档分块失败, documentId={}", documentId, e);
            documentProcessService.markFailed(documentId, taskId, "分块失败: " + e.getMessage());
        }
    }
}

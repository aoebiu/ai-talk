package info.mengnan.dialogerai.server.messaging.document.listener;

import info.mengnan.dialogerai.kb.param.ContentElement;
import info.mengnan.dialogerai.repository.entity.DocumentInfo;
import info.mengnan.dialogerai.repository.enums.DocumentStatus;
import info.mengnan.dialogerai.repository.repo.DocumentInfoRepository;
import info.mengnan.dialogerai.server.messaging.document.event.DocumentCleanedEvent;
import info.mengnan.dialogerai.server.messaging.document.event.DocumentParsedEvent;
import info.mengnan.dialogerai.server.param.document.CleaningConfig;
import info.mengnan.dialogerai.server.service.DocumentProcessService;
import info.mengnan.dialogerai.server.service.KnowledgeBaseBuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 阶段2：文档内容清洗。
 * 监听 DocumentParsedEvent，按用户配置的规则过滤和规范化文本内容元素。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningListener {

    private final DocumentInfoRepository documentInfoRepository;
    private final DocumentProcessService documentProcessService;
    private final KnowledgeBaseBuildService KnowledgeBaseBuildService;
    private final ApplicationEventPublisher eventPublisher;

    @Async("docProcessPool")
    @EventListener
    public void onDocumentParsed(DocumentParsedEvent event) {
        Long documentId = event.getDocumentId();
        String taskId = event.getTaskId();

        try {
            documentProcessService.updateStatus(documentId, DocumentStatus.CLEANING);
            refreshKbBuildProgress(documentId);

            List<ContentElement> original = event.getContentElements();
            List<ContentElement> cleaned = applyRules(original, event.getCleaningConfig());

            int cleanedCharCount = cleaned.stream()
                    .filter(e -> e.getType() == ContentElement.Type.TEXT)
                    .mapToInt(e -> e.getText() != null ? e.getText().length() : 0)
                    .sum();

            DocumentInfo info = documentInfoRepository.findById(documentId);
            info.setCleanedCharCount(cleanedCharCount);
            documentInfoRepository.updateById(info);

            int originalCharCount = info.getOriginalCharCount() != null ? info.getOriginalCharCount() : 0;

            refreshKbBuildProgress(documentId);

            eventPublisher.publishEvent(new DocumentCleanedEvent(
                    documentId, event.getMemberId(), taskId, event.getDocType(), cleaned
            ));

        } catch (Exception e) {
            log.error("文档清洗失败, documentId={}", documentId, e);
            documentProcessService.markFailed(documentId, taskId, "清洗失败: " + e.getMessage());
        }
    }

    private void refreshKbBuildProgress(Long documentId) {
        DocumentInfo info = documentInfoRepository.findById(documentId);
        if (info != null && info.getKbId() != null) {
            KnowledgeBaseBuildService.refreshBuildProgress(info.getKbId());
        }
    }

    private List<ContentElement> applyRules(List<ContentElement> elements, CleaningConfig config) {
        if (config == null) {
            return elements;
        }
        List<ContentElement> result = new ArrayList<>(elements);

        if (config.isNormalizeWhitespace()) {
            result = result.stream().map(e -> {
                if (e.getType() != ContentElement.Type.TEXT || e.getText() == null) return e;
                String normalized = e.getText()
                        .replaceAll("[ \t]+", " ")
                        .replaceAll("(\r\n|\r|\n){3,}", "\n\n")
                        .trim();
                return ContentElement.ofText(normalized, e.getPosition() != null ? e.getPosition() : 0);
            }).collect(Collectors.toList());
        }

        if (config.isMergeLineBreaks()) {
            result = result.stream().map(e -> {
                if (e.getType() != ContentElement.Type.TEXT || e.getText() == null) return e;
                String merged = e.getText()
                        .replaceAll("(?<!\n)\n(?!\n)", " ")
                        .trim();
                return ContentElement.ofText(merged, e.getPosition() != null ? e.getPosition() : 0);
            }).collect(Collectors.toList());
        }

        if (config.isFilterLowValueParagraphs()) {
            int minLen = config.getMinParagraphLength();
            result = result.stream().filter(e -> {
                if (e.getType() == ContentElement.Type.IMAGE) return true;
                String text = e.getText() != null ? e.getText().trim() : "";
                if (text.length() < minLen) return false;
                return !text.matches("[\\d\\s\\p{Punct}]+");
            }).collect(Collectors.toList());
        }

        if (config.isDeduplicateParagraphs()) {
            Set<String> seen = new LinkedHashSet<>();
            result = result.stream().filter(e -> {
                if (e.getType() == ContentElement.Type.IMAGE) return true;
                String key = e.getText() != null ? e.getText().trim() : "";
                return seen.add(key);
            }).collect(Collectors.toList());
        }

        return result;
    }
}

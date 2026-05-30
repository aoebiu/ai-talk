package info.mengnan.aitalk.server.messaging.document.listener;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.repository.entity.DocumentInfo;
import info.mengnan.aitalk.repository.enums.DocumentStatus;
import info.mengnan.aitalk.repository.repo.DocumentInfoRepository;
import info.mengnan.aitalk.server.messaging.document.event.DocumentChunkedEvent;
import info.mengnan.aitalk.server.service.DocumentProcessService;
import info.mengnan.aitalk.server.service.KnowledgeBaseBuildService;
import info.mengnan.aitalk.server.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阶段4：向量化存储。
 * 监听 DocumentChunkedEvent，分批调用 EmbeddingModel 生成向量，写入 Elasticsearch。
 * 每批完成后滚动更新 document_info.processed_chunks 用于进度展示。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingListener {

    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final ModelRegistry modelRegistry;
    private final ModelConfigService modelConfigService;
    private final DocumentInfoRepository documentInfoRepository;
    private final DocumentProcessService documentProcessService;
    private final KnowledgeBaseBuildService KnowledgeBaseBuildService;

    @Value("${embedding.model-name:text-embedding-v2}")
    private String embeddingModelName;

    private static final int BATCH_SIZE = 10;

    @Async("docProcessPool")
    @EventListener
    public void onDocumentChunked(DocumentChunkedEvent event) {
        Long documentId = event.getDocumentId();
        String taskId = event.getTaskId();

        try {
            documentProcessService.updateStatus(documentId, DocumentStatus.EMBEDDING);
            refreshKbBuildProgress(documentId);

            ModelConfig embeddingConfig = modelConfigService.findModel(
                    event.getMemberId(), embeddingModelName, ModelType.EMBEDDING);
            if (embeddingConfig == null) {
                throw new RuntimeException("Embedding 模型配置不存在：" + embeddingModelName);
            }

            EmbeddingModel embeddingModel = modelRegistry.createEmbeddingModel(embeddingConfig);
            EmbeddingStore<TextSegment> embeddingStore =
                    embeddingStoreRegistry.createEmbeddingStore(event.getIndexName());

            List<TextSegment> segments = event.getEnhancedSegments();
            int total = segments.size();
            int processed = 0;

            for (int i = 0; i < total; i += BATCH_SIZE) {
                List<TextSegment> batch = segments.subList(i, Math.min(i + BATCH_SIZE, total));
                List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                embeddingStore.addAll(embeddings, batch);

                processed += batch.size();
                DocumentInfo info = documentInfoRepository.findById(documentId);
                info.setProcessedChunks(processed);
                documentInfoRepository.updateById(info);
                refreshKbBuildProgress(documentId);
            }

            documentProcessService.updateStatus(documentId, DocumentStatus.DONE);
            refreshKbBuildProgress(documentId);

            log.info("document vectorization completed, documentId={}, indexName={}, totalChunks={}",
                    documentId, event.getIndexName(), total);

        } catch (Exception e) {
            log.error("document vectorization failed, documentId={}", documentId, e);
            documentProcessService.markFailed(documentId, taskId, "向量化失败: " + e.getMessage());
        }
    }

    private void refreshKbBuildProgress(Long documentId) {
        DocumentInfo info = documentInfoRepository.findById(documentId);
        if (info != null && info.getKbId() != null) {
            KnowledgeBaseBuildService.refreshBuildProgress(info.getKbId());
        }
    }
}

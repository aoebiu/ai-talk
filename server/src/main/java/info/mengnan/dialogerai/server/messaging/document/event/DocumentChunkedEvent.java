package info.mengnan.dialogerai.server.messaging.document.event;

import dev.langchain4j.data.segment.TextSegment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 文档分块完成事件。
 * 由 ChunkingListener 发布，携带已增强的 TextSegment 列表，交由 EmbeddingListener 向量化。
 */
@Getter
@RequiredArgsConstructor
public class DocumentChunkedEvent {

    private final Long documentId;
    private final Long memberId;
    private final String taskId;

    /** ES 索引名，EmbeddingListener 需要用它创建 EmbeddingStore */
    private final String indexName;

    /** 分块后携带图片元数据的增强文本段列表 */
    private final List<TextSegment> enhancedSegments;
}

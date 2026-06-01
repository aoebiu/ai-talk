package info.mengnan.dialogerai.server.messaging.document.event;

import info.mengnan.dialogerai.kb.param.ContentElement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 文档清洗完成事件。
 * 由 CleaningListener 发布，携带清洗后的内容元素列表。
 */
@Getter
@RequiredArgsConstructor
public class DocumentCleanedEvent {

    private final Long documentId;
    private final Long memberId;
    private final String taskId;

    /** 文档语义类型，分块阶段需要用于选择切分策略 */
    private final String docType;

    /** 经过清洗规则处理后的内容元素列表 */
    private final List<ContentElement> contentElements;
}

package info.mengnan.aitalk.server.messaging.document.event;

import info.mengnan.aitalk.kb.param.ContentElement;
import info.mengnan.aitalk.server.param.document.CleaningConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 文档解析完成事件。
 * 由 ParsingListener 发布，携带从文件中提取的有序内容元素列表（文本 + 图片）。
 */
@Getter
@RequiredArgsConstructor
public class DocumentParsedEvent {

    private final Long documentId;
    private final Long memberId;
    private final String taskId;

    /** 文档语义类型，分块阶段需要用于选择切分策略 */
    private final String docType;

    /** 传递给清洗阶段的规则配置 */
    private final CleaningConfig cleaningConfig;

    /** 从文档中按顺序提取的内容元素（文本段落 + 图片） */
    private final List<ContentElement> contentElements;
}

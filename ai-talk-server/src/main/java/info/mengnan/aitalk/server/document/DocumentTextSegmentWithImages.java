package info.mengnan.aitalk.server.document;

import dev.langchain4j.data.segment.TextSegment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 扩展的文本段落，包含关联的图片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTextSegmentWithImages {
    /**
     * 文本段落
     */
    private TextSegment textSegment;

    /**
     * 关联的图片列表
     */
    @Builder.Default
    private List<DocumentImage> images = Collections.emptyList();

    /**
     * 获取文本内容
     */
    public String getText() {
        return textSegment.text();
    }
}

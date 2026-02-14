package info.mengnan.aitalk.server.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档内容单元
 * 表示文档中的一个内容元素，可以是文本或图片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentElement {

    /**
     * 内容类型
     */
    public enum Type {
        TEXT,      // 文本内容
        IMAGE      // 图片内容
    }

    /**
     * 内容类型
     */
    private Type type;

    /**
     * 如果是文本，存储文本内容
     */
    private String text;

    /**
     * 如果是图片，存储图片信息
     */
    private DocumentImage image;

    /**
     * 内容在文档中的顺序位置（0-based）
     */
    private Integer position;

    /**
     * 创建文本内容元素
     */
    public static ContentElement ofText(String text, int position) {
        return ContentElement.builder()
                .type(Type.TEXT)
                .text(text)
                .position(position)
                .build();
    }

    /**
     * 创建图片内容元素
     */
    public static ContentElement ofImage(DocumentImage image, int position) {
        return ContentElement.builder()
                .type(Type.IMAGE)
                .image(image)
                .position(position)
                .build();
    }
}

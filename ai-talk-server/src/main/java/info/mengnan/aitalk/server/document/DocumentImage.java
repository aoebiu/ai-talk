package info.mengnan.aitalk.server.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档中提取的图片信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentImage {
    /**
     * 图片文件名
     */
    private String filename;

    /**
     * 页码（PDF 为页码，Word/PPT 为 1）
     */
    private Integer pageNumber;

    /**
     * 图片格式 (png, jpg, etc.)
     */
    private String format;

    /**
     * 图片描述（通过图生文生成）
     */
    private String imageDescription;
}

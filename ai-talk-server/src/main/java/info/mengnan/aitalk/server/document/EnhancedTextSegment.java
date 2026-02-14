package info.mengnan.aitalk.server.document;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 扩展的 TextSegment，可以在元数据中存储图片描述信息
 */
public class EnhancedTextSegment extends TextSegment {
    private List<DocumentImage> images;

    public EnhancedTextSegment(String text, Metadata metadata, List<DocumentImage> images) {
        super(text, metadata);
        this.images = images;
    }

    public static EnhancedTextSegment from(String text, List<DocumentImage> images) {
        Metadata metadata = new Metadata();

        // 将图片信息添加到元数据
        if (!images.isEmpty()) {
            metadata.put("image_count", String.valueOf(images.size()));

            // 存储图片文件名列表
            StringBuilder imageList = new StringBuilder();
            for (int i = 0; i < images.size(); i++) {
                DocumentImage image = images.get(i);
                if (i > 0) {
                    imageList.append(";");
                }
                imageList.append(image.getFilename());
            }
            metadata.put("images", imageList.toString());

            // 存储图片描述列表
            StringBuilder descriptionList = new StringBuilder();
            for (int i = 0; i < images.size(); i++) {
                DocumentImage image = images.get(i);
                if (i > 0) {
                    descriptionList.append(";");
                }
                descriptionList.append(image.getImageDescription() != null ? image.getImageDescription() : "");
            }
            metadata.put("image_descriptions", descriptionList.toString());
        }

        return new EnhancedTextSegment(text, metadata, images);
    }

    public List<DocumentImage> getImages() {
        return images;
    }
}

package info.mengnan.aitalk.server.document;

import info.mengnan.aitalk.rag.service.DirectModelInvoker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * 按顺序提取文档内容
 * 保持文本和图片在原文档中的相对位置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SequentialDocumentExtractor {

    private final DirectModelInvoker directModelInvoker;

    /**
     * 从 Word 文档按顺序提取内容
     *
     * @param filePath Word 文件路径
     * @return 内容元素列表，保持原始顺序
     */
    public List<ContentElement> extractWordContentSequentially(Path filePath) {
        List<ContentElement> elements = new ArrayList<>();
        int position = 0;

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {
             // 遍历所有段落
             position = getPosition(elements, position, document.getParagraphs(), document);

            // 处理表格中的内容
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        position = getPosition(elements, position, cell.getParagraphs(), document);
                    }
                }
            }

            log.info("Extracted {} content elements from Word document: {}", elements.size(), filePath.getFileName());
        } catch (IOException e) {
            log.error("Error extracting content from Word document: {}", filePath, e);
        }

        return elements;
    }

    private int getPosition(List<ContentElement> elements, int position, List<XWPFParagraph> paragraphs, XWPFDocument document) {
        for (XWPFParagraph paragraph : paragraphs) {
            String paragraphText = paragraph.getText();

            // 先添加段落文本
            if (!paragraphText.isEmpty()) {
                elements.add(ContentElement.ofText(paragraphText, position++));
            }

            // 检查段落中的图片
            for (XWPFRun run : paragraph.getRuns()) {
                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                    XWPFPictureData pictureData = picture.getPictureData();
                    if (pictureData != null) {
                        DocumentImage image = createDocumentImage(pictureData, position);
                        elements.add(ContentElement.ofImage(image, position++));
                    }
                }
            }
        }
        return position;
    }

    /**
     * 从 PowerPoint 按顺序提取内容
     *
     * @param filePath PowerPoint 文件路径
     * @return 内容元素列表，保持原始顺序
     */
    public List<ContentElement> extractPowerPointContentSequentially(Path filePath) {
        List<ContentElement> elements = new ArrayList<>();
        int position = 0;

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XMLSlideShow slideShow = new XMLSlideShow(fis)) {

            // 遍历所有幻灯片
            for (XSLFSlide slide : slideShow.getSlides()) {
                // 遍历幻灯片中的所有形状
                for (XSLFShape shape : slide.getShapes()) {
                    // 处理图片
                    if (shape instanceof XSLFPictureShape) {
                        XSLFPictureShape pictureShape = (XSLFPictureShape) shape;
                        try {
                            DocumentImage image = createPowerPointImage(pictureShape, position);
                            elements.add(ContentElement.ofImage(image, position++));
                        } catch (Exception e) {
                            log.warn("Failed to extract picture from PowerPoint: {}", e.getMessage());
                        }
                    }
                    // 处理文本形状
                    else if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        try {
                            String text = textShape.getText();
                            if (text != null && !text.trim().isEmpty()) {
                                elements.add(ContentElement.ofText(text, position++));
                            }
                        } catch (Exception e) {
                            log.debug("Failed to extract text from shape: {}", e.getMessage());
                        }
                    }
                }
            }

            log.info("Extracted {} content elements from PowerPoint: {}", elements.size(), filePath.getFileName());
        } catch (IOException e) {
            log.error("Error extracting content from PowerPoint: {}", filePath, e);
        }

        return elements;
    }

    /**
     * 按文件类型提取内容
     *
     * @param filePath 文件路径
     * @param fileExtension 文件扩展名
     * @return 内容元素列表
     */
    public List<ContentElement> extractContentSequentially(Path filePath, String fileExtension) {
        return switch (fileExtension.toLowerCase()) {
            case ".docx" -> extractWordContentSequentially(filePath);
            case ".pptx" -> extractPowerPointContentSequentially(filePath);
            default -> {
                log.info("File type {} does not support sequential content extraction", fileExtension);
                yield Collections.emptyList();
            }
        };
    }

    /**
     * 为 Word 文档中的图片创建 DocumentImage 对象
     */
    private DocumentImage createDocumentImage(XWPFPictureData pictureData, int position) {
        String pictureName = pictureData.getFileName();
        String extension = "png";
        if (pictureName != null && pictureName.contains(".")) {
            extension = pictureName.substring(pictureName.lastIndexOf(".") + 1).toLowerCase();
        }

        String filename = String.format("word_image_%d.%s", position, extension);
        byte[] data = pictureData.getData();
        String imageDescription = directModelInvoker.imageToText(data, "identify_picture","image/png");

        return DocumentImage.builder()
                .filename(filename)
                .pageNumber(1)
                .format(extension)
                .imageDescription(imageDescription)
                .build();
    }

    /**
     * 为 PowerPoint 中的图片创建 DocumentImage 对象
     */
    private DocumentImage createPowerPointImage(XSLFPictureShape pictureData, int position) {
        String pictureName = pictureData.getPictureData().getFileName();
        String extension = "png";
        if (pictureName.contains(".")) {
            extension = pictureName.substring(pictureName.lastIndexOf(".") + 1).toLowerCase();
        }

        String filename = String.format("ppt_image_%d.%s", position, extension);
        byte[] data = pictureData.getPictureData().getData();
        String imageDescription = directModelInvoker.imageToText(data, "identify_picture","image/png");

        return DocumentImage.builder()
                .filename(filename)
                .pageNumber(1)
                .format(extension)
                .imageDescription(imageDescription)
                .build();
    }
}

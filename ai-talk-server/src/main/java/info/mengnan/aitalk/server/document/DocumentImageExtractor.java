package info.mengnan.aitalk.server.document;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.service.DirectModelInvoker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * 文档图片提取工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentImageExtractor {

    private final DirectModelInvoker directModelInvoker;

    /**
     * 从 PDF 提取图片
     * @param filePath PDF 文件路径
     * @return 图片信息列表（暂不支持）
     */
    public List<DocumentImage> extractImagesFromPDF(Path filePath) {
        log.info("PDF image extraction is not fully supported. Consider using dedicated libraries for better PDF image extraction.");
        return Collections.emptyList();
    }

    /**
     * 从 Word 文档 (.docx) 提取图片
     *
     * @param filePath Word 文件路径
     * @return 图片信息列表
     */
    public List<DocumentImage> extractImagesFromWord(Path filePath) {
        List<DocumentImage> images = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            List<XWPFPictureData> pictureDataList = document.getAllPictures();
            int imageCount = 0;

            for (XWPFPictureData pictureData : pictureDataList) {
                // 获取图片数据的 MIME 类型
                String pictureName = pictureData.getFileName();
                String extension = "png";
                if (pictureName != null && pictureName.contains(".")) {
                    extension = pictureName.substring(pictureName.lastIndexOf(".") + 1).toLowerCase();
                }

                String filename = String.format("word_image_%d.%s", ++imageCount, extension);
                byte[] data = pictureData.getData();

                String imageDescription = directModelInvoker.imageToText(data, "identify_picture","image/png");

                DocumentImage image = DocumentImage.builder()
                        .filename(filename)
                        .pageNumber(1)
                        .format(extension)
                        .imageDescription(imageDescription)
                        .build();

                images.add(image);
                log.debug("Extracted image from Word document: {}", filename);
            }
        } catch (IOException e) {
            log.error("Error extracting images from Word document: {}", filePath, e);
        }

        log.info("Extracted {} images from Word document: {}", images.size(), filePath.getFileName());
        return images;
    }

    /**
     * 从 PowerPoint 文档 (.pptx) 提取图片
     *
     * @param filePath PowerPoint 文件路径
     * @return 图片信息列表
     */
    public List<DocumentImage> extractImagesFromPowerPoint(Path filePath) {
        List<DocumentImage> images = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XMLSlideShow slideShow = new XMLSlideShow(fis)) {

            List<XSLFPictureData> pictureDataList = slideShow.getPictureData();
            int imageCount = 0;

            for (XSLFPictureData pictureData : pictureDataList) {
                // 从 contentType 获取扩展名
                String contentType = pictureData.getContentType();
                String extension = "png";
                if (contentType != null) {
                    if (contentType.contains("jpeg")) {
                        extension = "jpeg";
                    } else if (contentType.contains("png")) {
                        extension = "png";
                    } else if (contentType.contains("gif")) {
                        extension = "gif";
                    } else if (contentType.contains("bmp")) {
                        extension = "bmp";
                    }
                }

                String filename = String.format("ppt_image_%d.%s", ++imageCount, extension);

                // 生成图片描述而不保存图片
                byte[] data = pictureData.getData();
                String imageDescription = directModelInvoker.imageToText(data, "identify_picture","image/png");

                DocumentImage image = DocumentImage.builder()
                        .filename(filename)
                        .pageNumber(1)
                        .format(extension)
                        .imageDescription(imageDescription)
                        .build();

                images.add(image);
                log.debug("Extracted image from PowerPoint: {}", filename);
            }
        } catch (IOException e) {
            log.error("Error extracting images from PowerPoint: {}", filePath, e);
        }

        log.info("Extracted {} images from PowerPoint: {}", images.size(), filePath.getFileName());
        return images;
    }

    /**
     * 根据文件类型提取所有图片
     *
     * @param filePath 文件路径
     * @param fileExtension 文件扩展名
     * @return 图片信息列表
     */
    public List<DocumentImage> extractImages(Path filePath, String fileExtension) {
        return switch (fileExtension.toLowerCase()) {
            case ".pdf" -> extractImagesFromPDF(filePath);
            case ".docx" -> extractImagesFromWord(filePath);
            case ".pptx" -> extractImagesFromPowerPoint(filePath);
            default -> {
                log.info("File type {} does not support image extraction", fileExtension);
                yield Collections.emptyList();
            }
        };
    }
}

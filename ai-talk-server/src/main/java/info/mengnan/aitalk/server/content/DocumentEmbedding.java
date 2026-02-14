package info.mengnan.aitalk.server.content;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.container.assemble.DynamicEmbeddingStoreRegistry;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.server.document.DocumentImage;
import info.mengnan.aitalk.server.document.DocumentImageExtractor;
import info.mengnan.aitalk.server.document.EnhancedTextSegment;
import info.mengnan.aitalk.server.document.ContentElement;
import info.mengnan.aitalk.server.document.SequentialDocumentExtractor;
import info.mengnan.aitalk.server.param.DocumentUploadResult;
import info.mengnan.aitalk.server.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbedding {

    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final ModelRegistry modelRegistry;
    private final ModelConfigService modelConfigService;
    private final DocumentImageExtractor imageExtractor;
    private final SequentialDocumentExtractor sequentialExtractor;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${embedding.model-name:text-embedding-v2}")
    private String embeddingModelName;

    /**
     * 上传文件并进行向量化存储
     */
    public DocumentUploadResult uploadAndProcessDocument(Long memberId, MultipartFile file, String type) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return DocumentUploadResult.error("文件名无效");
        }

        String indexName = sanitizeIndexName(originalFilename);
        if (indexExists(indexName)) {
            log.warn("Document {} already exists in index {}, skipping upload", originalFilename, indexName);
            return DocumentUploadResult.duplicate("文件已存在，无需重复上传", indexName);
        }

        // 创建上传目录
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileExtension = getFileExtension(originalFilename);

            // 按顺序提取内容
            List<ContentElement> contentElements = sequentialExtractor.extractContentSequentially(filePath, fileExtension);
            log.info("Extracted {} content elements (text + images) from document: {}", contentElements.size(), originalFilename);

            // 统计图片数量
            int imageCount = (int) contentElements.stream()
                    .filter(e -> e.getType() == ContentElement.Type.IMAGE)
                    .count();
            log.info("Total images: {}", imageCount);

            // 解析文档用于 LangChain4j（获取基础文本）
            Document document = parseDocument(filePath, fileExtension);

            // 分割文档
            DocumentSplitter splitter = switch (type.toLowerCase()) {
                case "short_text" -> DocumentSplitters.recursive(150, 20);
                case "paper" -> DocumentSplitters.recursive(400, 40);
                case "contract" -> DocumentSplitters.recursive(300, 0);
                case "novel" -> DocumentSplitters.recursive(750, 50);
                default -> DocumentSplitters.recursive(300, 50);
            };

            List<TextSegment> textSegments = splitter.split(document);
            log.info("Document split into {} fragments", textSegments.size());

            // 提取所有图片对象用于元数据
            List<DocumentImage> images = contentElements.stream()
                    .filter(e -> e.getType() == ContentElement.Type.IMAGE)
                    .map(ContentElement::getImage)
                    .toList();

            // 创建增强的文本段落，保存位置信息和图片元数据
            List<TextSegment> enhancedSegments = createEnhancedSegmentsWithPosition(textSegments, images);

            // 动态创建 EmbeddingStore
            EmbeddingStore<TextSegment> embeddingStore = embeddingStoreRegistry.createEmbeddingStore(indexName);

            // 从数据库查询 EmbeddingModel 配置
            ModelConfig embeddingConfig = modelConfigService.findModel(memberId, embeddingModelName, ModelType.EMBEDDING);
            if (embeddingConfig == null) {
                log.error("embedding Model Configuration Not Found: {}", embeddingModelName);
                throw new RuntimeException("Embedding 模型配置不存在: " + embeddingModelName);
            }

            // 动态创建 EmbeddingModel
            EmbeddingModel embeddingModel = modelRegistry.createEmbeddingModel(embeddingConfig);

            // 生成向量并存储
            List<Embedding> embeddings = embeddingModel.embedAll(enhancedSegments).content();
            embeddingStore.addAll(embeddings, enhancedSegments);

            log.info("Document {} was successfully vectorized and stored to the index {}. Total images: {}",
                    originalFilename, indexName, imageCount);
            return DocumentUploadResult.success(originalFilename, indexName);
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /**
     * 创建增强的文本段落，包含位置信息和图片元数据
     */
    private List<TextSegment> createEnhancedSegmentsWithPosition(
            List<TextSegment> textSegments,
            List<DocumentImage> images) {

        List<TextSegment> enhancedSegments = new ArrayList<>();

        for (int i = 0; i < textSegments.size(); i++) {
            TextSegment segment = textSegments.get(i);

            // 创建增强段落，保存位置和图片元数据
            EnhancedTextSegment enhancedSegment = EnhancedTextSegment.from(segment.text(), images);

            // 添加段落位置信息到元数据
            enhancedSegment.metadata().put("segment_index", String.valueOf(i));
            enhancedSegment.metadata().put("segment_position", String.valueOf(i * 100 / Math.max(1, textSegments.size())));

            enhancedSegments.add(enhancedSegment);
        }

        return enhancedSegments;
    }

    /**
     * 将图片描述穿插在文本中
     * 策略：将每个图片的描述添加到各个文本段落中，模拟图片在文档中的原始位置
     */
    private List<TextSegment> integrateImagesIntoText(List<TextSegment> textSegments, List<DocumentImage> images) {
        if (images.isEmpty()) {
            return textSegments;
        }

        List<TextSegment> enhancedSegments = new ArrayList<>();
        // 计算每个图片应该穿插在哪个段落
        int imagesPerSegment = Math.max(1, images.size() / Math.max(1, textSegments.size()));
        int imageIndex = 0;

        for (int segmentIndex = 0; segmentIndex < textSegments.size(); segmentIndex++) {
            TextSegment segment = textSegments.get(segmentIndex);
            StringBuilder enrichedText = new StringBuilder(segment.text());

            // 为当前段落穿插相应的图片描述
            int imagesToAdd = Math.min(imagesPerSegment, images.size() - imageIndex);
            for (int i = 0; i < imagesToAdd && imageIndex < images.size(); i++) {
                DocumentImage image = images.get(imageIndex++);
                if (image.getImageDescription() != null && !image.getImageDescription().isEmpty()) {
                    enrichedText.append("\n[图片] ").append(image.getImageDescription());
                }
            }

            // 如果是最后一个段落，添加剩余的图片描述
            if (segmentIndex == textSegments.size() - 1) {
                while (imageIndex < images.size()) {
                    DocumentImage image = images.get(imageIndex++);
                    if (image.getImageDescription() != null && !image.getImageDescription().isEmpty()) {
                        enrichedText.append("\n[图片] ").append(image.getImageDescription());
                    }
                }
            }

            // 创建增强的文本段落，保存图片元数据
            EnhancedTextSegment enhancedSegment = EnhancedTextSegment.from(enrichedText.toString(), images);
            enhancedSegments.add(enhancedSegment);

            log.debug("Text segment {} integrated with images", segmentIndex);
        }

        return enhancedSegments;
    }

    /**
     * 检查ES中索引是否存在
     */
    private boolean indexExists(String indexName) {
        List<String> allIndexNames = embeddingStoreRegistry.queryAllIndexNames();
        return allIndexNames.contains(indexName);

    }

    /**
     * 根据文件类型选择对应的解析器
     */
    private Document parseDocument(Path filePath, String extension) throws IOException {
        DocumentParser parser = switch (extension.toLowerCase()) {
            case ".pdf" -> new ApachePdfBoxDocumentParser();
            case ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx" -> new ApachePoiDocumentParser();
            case ".md", ".txt" -> new TextDocumentParser();
            default -> throw new RuntimeException("不支持的文件类型: " + extension);
        };

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return parser.parse(inputStream);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 清理文件名，使其适合作为 Elasticsearch 索引名
     * ES 索引名要求：小写、不能包含特殊字符
     */
    private String sanitizeIndexName(String filename) {
        // 移除文件扩展名
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf("."))
                : filename;

        // 转小写，替换特殊字符为下划线
        return nameWithoutExt.toLowerCase().replaceAll("[' \"*,/<>?\\\\|]", "");
    }
}
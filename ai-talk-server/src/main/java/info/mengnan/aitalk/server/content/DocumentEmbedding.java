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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbedding {

    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final ModelRegistry modelRegistry;
    private final ModelConfigService modelConfigService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${embedding.model-name:text-embedding-v2}")
    private String embeddingModelName;

    /**
     * 上传文件并进行向量化存储
     */
    public String uploadAndProcessDocument(MultipartFile file) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 保存文件
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("文件名为空");
            return null;
        }

        Path filePath = uploadPath.resolve(originalFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 解析文档
        String fileExtension = getFileExtension(originalFilename);
        Document document = parseDocument(filePath, fileExtension);

        // 文档分割
        // todo 遵循不同文件类型,不同分割数量
        DocumentSplitter splitter = DocumentSplitters.recursive(
                300,  // 每个片段最大字符数
                50    // 片段重叠字符数
        );
        List<TextSegment> segments = splitter.split(document);
        log.info("文档已分割成 {} 个片段", segments.size());

        // 动态创建 EmbeddingStore（使用文件名作为索引名）
        String indexName = sanitizeIndexName(originalFilename);
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreRegistry.createEmbeddingStore(indexName);

        // 从数据库查询 EmbeddingModel 配置
        ModelConfig embeddingConfig = modelConfigService.findModel(embeddingModelName, ModelType.EMBEDDING);
        if (embeddingConfig == null) {
            log.error("未找到 Embedding 模型配置: {}", embeddingModelName);
            throw new RuntimeException("Embedding 模型配置不存在: " + embeddingModelName);
        }

        // 动态创建 EmbeddingModel
        EmbeddingModel embeddingModel = modelRegistry.createEmbeddingModel(embeddingConfig);

        // 生成向量并存储
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        log.info("文档 {} 已成功向量化并存储到索引 {}", originalFilename, indexName);
        return originalFilename;
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
        return nameWithoutExt.toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_");
    }
}
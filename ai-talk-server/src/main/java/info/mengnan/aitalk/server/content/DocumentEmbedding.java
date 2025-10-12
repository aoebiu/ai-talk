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
import info.mengnan.aitalk.server.rag.container.RagContainer;
import info.mengnan.aitalk.server.rag.container.assemble.DynamicEmbeddingStoreRegistry;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbedding {

    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final RagContainer ragContainer;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

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
        if (originalFilename == null) return null;

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

        String storeBean = embeddingStoreRegistry.registerEmbeddingStoreBean(originalFilename);
        EmbeddingStore<TextSegment> embeddingStore = ragContainer.getEmbeddingStore(storeBean);

        List<Embedding> embeddings = ragContainer.getEmbeddingModel("embedding:text-embedding-v2").embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

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
            default -> throw new RuntimeException("");
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
}
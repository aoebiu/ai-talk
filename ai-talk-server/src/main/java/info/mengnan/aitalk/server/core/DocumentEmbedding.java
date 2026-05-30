package info.mengnan.aitalk.server.core;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import info.mengnan.aitalk.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.aitalk.kb.param.DocumentImage;
import info.mengnan.aitalk.kb.param.EnhancedTextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理工具类，提供各阶段 Listener 复用的基础能力：
 * <ul>
 *   <li>文件解析（parseDocument）</li>
 *   <li>增强分块创建（createEnhancedSegmentsWithPosition）</li>
 *   <li>知识库索引名构建（buildKbIndexName）</li>
 *   <li>ES 索引删除（deleteIndex）</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class DocumentEmbedding {

    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;

    /**
     * 根据文件路径和扩展名解析文档，返回 LangChain4j Document（纯文本）。
     */
    public Document parseDocument(Path filePath, String extension) throws IOException {
        DocumentParser parser = switch (extension.toLowerCase()) {
            case ".pdf" -> new ApachePdfBoxDocumentParser();
            case ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx" -> new ApachePoiDocumentParser();
            case ".md", ".txt" -> new TextDocumentParser();
            default -> throw new RuntimeException("不支持的文件类型：" + extension);
        };
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return parser.parse(inputStream);
        }
    }

    /**
     * 将 TextSegment 列表增强：注入图片元数据并添加位置信息到 Metadata。
     */
    public List<TextSegment> createEnhancedSegmentsWithPosition(
            List<TextSegment> textSegments,
            List<DocumentImage> images) {

        List<TextSegment> enhanced = new ArrayList<>();
        for (int i = 0; i < textSegments.size(); i++) {
            TextSegment segment = textSegments.get(i);
            EnhancedTextSegment enhancedSegment = EnhancedTextSegment.from(segment.text(), images);
            enhancedSegment.metadata().put("segment_index", String.valueOf(i));
            enhancedSegment.metadata().put("segment_position",
                    String.valueOf(i * 100 / Math.max(1, textSegments.size())));
            enhanced.add(enhancedSegment);
        }
        return enhanced;
    }

    /**
     * 构建知识库 ES 索引名：{memberId}_kb_{kbId}
     */
    public String buildKbIndexName(Long memberId, Long kbId) {
        return memberId + "_kb_" + kbId;
    }

    /**
     * 删除 ES 索引（物理删除向量数据）。
     */
    public void deleteIndex(String indexName) {
        embeddingStoreRegistry.deleteIndex(indexName);
        log.info("ES 索引已删除: {}", indexName);
    }

    /**
     * 获取文件扩展名（含点号，如 .pdf）。
     */
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 清理文件名，使其符合 Elasticsearch 索引命名规范（小写、无特殊字符）。
     */
    public String sanitizeIndexName(String filename) {
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf("."))
                : filename;
        return nameWithoutExt.toLowerCase().replaceAll("[' \"*,/<>?\\\\|]", "");
    }
}

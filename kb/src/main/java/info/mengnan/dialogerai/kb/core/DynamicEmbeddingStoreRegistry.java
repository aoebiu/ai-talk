package info.mengnan.dialogerai.kb.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchRequestFailedException;
import info.mengnan.dialogerai.kb.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态 Elasticsearch 索引管理器,提供动态创建 EmbeddingStore 的能力
 */
@Slf4j
public class DynamicEmbeddingStoreRegistry {

    /**
     * Elasticsearch 配置属性
     */
    private final ElasticsearchProperties properties;

    public DynamicEmbeddingStoreRegistry(ElasticsearchProperties properties) {
        this.properties = properties;

    }

    /**
     * 查询指定用户的 Elasticsearch 索引名称
     *
     * @param memberId 用户 ID，用于按用户维度过滤索引
     * @return 索引名称列表
     */
    public List<String> queryAllIndexNames(Long memberId) {
        try {
            RestClient restClient = createRestClient(this.properties);
            List<String> indexNames = queryAllIndices(restClient, properties, memberId);
            log.info("Found {} indices for member {}: {}", indexNames.size(), memberId, indexNames);
            restClient.close();
            return indexNames;
        } catch (Exception e) {
            throw new ElasticsearchRequestFailedException("Failed to query Elasticsearch indices", e);
        }
    }

    /**
     * 为指定索引动态创建 EmbeddingStore
     * 每次调用都会创建新的实例
     *
     * @param indexName Elasticsearch 索引名称
     * @return EmbeddingStore 实例
     */
    public EmbeddingStore<TextSegment> createEmbeddingStore(String indexName) {
        try {
            log.debug("Creating EmbeddingStore for index: {}", indexName);

            // 为每个索引创建独立的 RestClient
            RestClient restClient = createRestClient(properties);

            // 创建 EmbeddingStore
            EmbeddingStore<TextSegment> embeddingStore = ElasticsearchEmbeddingStore.builder()
                    .restClient(restClient)
                    .indexName(indexName)
                    .build();

            log.debug("Successfully created EmbeddingStore for index: {}", indexName);
            return embeddingStore;
        } catch (Exception e) {
            log.error("Failed to create EmbeddingStore for index: {}", indexName, e);
            throw new RuntimeException("Failed to create EmbeddingStore for index: " + indexName, e);
        }
    }

    /**
     * 创建 RestClient
     */
    private RestClient createRestClient(ElasticsearchProperties properties) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (properties.getUsername() != null && properties.getPassword() != null) {
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
            );
        }

        return RestClient.builder(
                        new HttpHost(properties.getHost(), properties.getPort(), "http"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                ).build();
    }

    /**
     * 删除指定的 Elasticsearch 索引
     *
     * @param indexName 索引名称
     */
    @SuppressWarnings("resource")
    public void deleteIndex(String indexName) {
        try (RestClient restClient = createRestClient(this.properties);
             RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {
            ElasticsearchClient client = new ElasticsearchClient(transport);
            client.indices().delete(builder -> builder.index(indexName));
            log.info("Successfully deleted index: {}", indexName);
        } catch (Exception e) {
            log.error("Failed to delete index: {}", indexName, e);
            throw new RuntimeException("Failed to delete index: " + indexName, e);
        }
    }

    /**
     * 读取指定文档在知识库索引中的文本分块，按 segment_index 升序返回。
     */
    @SuppressWarnings({"resource", "rawtypes"})
    public List<String> fetchDocumentSegments(String indexName, Long documentId, int maxSegments) {
        if (maxSegments <= 0) {
            return Collections.emptyList();
        }
        String docIdStr = String.valueOf(documentId);
        try (RestClient restClient = createRestClient(this.properties);
             RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {
            ElasticsearchClient client = new ElasticsearchClient(transport);
            SearchResponse<Map> response = client.search(builder -> builder
                            .index(indexName)
                            .query(query -> query.term(term -> term
                                    .field("metadata.document_id")
                                    .value(docIdStr)))
                            .size(maxSegments),
                    Map.class);

            return response.hits().hits().stream()
                    .sorted(Comparator.comparingInt(this::extractSegmentIndex))
                    .map(Hit::source)
                    .map(this::extractSegmentText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch document segments from index: {}, documentId={}", indexName, documentId, e);
            throw new RuntimeException("Failed to fetch document segments from index: " + indexName, e);
        }
    }

    /**
     * 删除知识库索引中指定文档的全部向量分块（不删除整个索引）。
     */
    @SuppressWarnings("resource")
    public void deleteDocumentSegments(String indexName, Long documentId) {
        String docIdStr = String.valueOf(documentId);
        try (RestClient restClient = createRestClient(this.properties);
             RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {
            ElasticsearchClient client = new ElasticsearchClient(transport);
            client.deleteByQuery(builder -> builder
                    .index(indexName)
                    .query(query -> query.term(term -> term
                            .field("metadata.document_id")
                            .value(docIdStr))));
            log.info("Deleted ES segments for documentId={} in index={}", documentId, indexName);
        } catch (Exception e) {
            log.error("Failed to delete document segments from index: {}, documentId={}", indexName, documentId, e);
            throw new RuntimeException("Failed to delete document segments from index: " + indexName, e);
        }
    }

    /**
     * 查询指定用户的 ES 索引
     */
    @SuppressWarnings("resource")
    private List<String> queryAllIndices(RestClient restClient, ElasticsearchProperties properties, Long memberId) throws IOException {
        List<String> indexNames = new ArrayList<>();

        // 如果配置了手动指定的索引列表，按用户前缀过滤后返回
        if (!properties.getIndexNames().isEmpty()) {
            String prefix = memberId + "_";
            List<String> filtered = properties.getIndexNames().stream()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toList());
            log.info("Using manually configured index names for member {}: {}", memberId, filtered);
            return filtered;
        }

        // 如果启用了自动发现，从 ES 查询索引并按用户前缀过滤
        if (properties.isAutoDiscoverIndices()) {
            String userPrefix = memberId + "_";
            try (RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {
                ElasticsearchClient client = new ElasticsearchClient(transport);
                IndicesResponse response = client.cat().indices();

                for (IndicesRecord record : response.valueBody()) {
                    String indexName = record.index();
                    if (indexName == null || !indexName.startsWith(userPrefix)) continue;
                    // 如果配置了索引名称过滤模式，进行额外过滤
                    if (properties.getIndexNamePattern() != null && !properties.getIndexNamePattern().isEmpty()) {
                        if (indexName.matches(properties.getIndexNamePattern())) {
                            indexNames.add(indexName);
                        }
                    } else {
                        indexNames.add(indexName);
                    }
                }
            }
        }

        return indexNames;
    }

    @SuppressWarnings("rawtypes")
    private int extractSegmentIndex(Hit<Map> hit) {
        Map<?, ?> source = hit.source();
        if (source == null) {
            return Integer.MAX_VALUE;
        }
        Object metadataObj = source.get("metadata");
        if (!(metadataObj instanceof Map<?, ?> metadata)) {
            return Integer.MAX_VALUE;
        }
        Object indexObj = metadata.get("segment_index");
        if (indexObj == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(indexObj.toString());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private String extractSegmentText(Map<?, ?> source) {
        if (source == null) {
            return null;
        }
        Object text = source.get("text");
        return text != null ? text.toString() : null;
    }
}
package info.mengnan.aitalk.rag.container.assemble;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import info.mengnan.aitalk.rag.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
     * 查询所有可用的 Elasticsearch 索引名称
     *
     * @return 索引名称列表
     */
    public List<String> queryAllIndexNames() {
        try {
            RestClient restClient = createRestClient(this.properties);
            List<String> indexNames = queryAllIndices(restClient, properties);
            log.info("Found {} indices in Elasticsearch: {}", indexNames.size(), indexNames);
            restClient.close();
            return indexNames;
        } catch (Exception e) {
            log.error("Failed to query Elasticsearch indices", e);
            return new ArrayList<>();
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
     * 查询所有 ES 索引
     */
    private List<String> queryAllIndices(RestClient restClient, ElasticsearchProperties properties) throws IOException {
        List<String> indexNames = new ArrayList<>();

        // 如果配置了手动指定的索引列表，使用配置的列表
        if (!properties.getIndexNames().isEmpty()) {
            log.info("Using manually configured index names: {}", properties.getIndexNames());
            return new ArrayList<>(properties.getIndexNames());
        }

        // 如果启用了自动发现，从 ES 查询所有索引
        if (properties.isAutoDiscoverIndices()) {
            try (RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {
                ElasticsearchClient client = new ElasticsearchClient(transport);

                // 使用 cat indices API 查询所有索引
                IndicesResponse response = client.cat().indices();

                for (IndicesRecord record : response.valueBody()) {
                    String indexName = record.index();
                    // 过滤系统索引（以 . 开头的索引）
                    if (indexName != null && !indexName.startsWith(".")) {
                        // 如果配置了索引名称过滤模式，进行过滤
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
        }

        return indexNames;
    }
}
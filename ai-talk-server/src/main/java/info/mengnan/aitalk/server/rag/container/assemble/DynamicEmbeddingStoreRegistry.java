package info.mengnan.aitalk.server.rag.container.assemble;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import info.mengnan.aitalk.server.config.ElasticsearchProperties;
import info.mengnan.aitalk.server.rag.container.RagContainer;
import info.mengnan.aitalk.server.util.Cast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 动态 Elasticsearch 索引管理器
 * 在应用启动时查询所有 ES 索引，并为每个索引动态注册一个 EmbeddingStore Bean
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicEmbeddingStoreRegistry implements InitializingBean {

    /**
     * Elasticsearch 配置属性
     */
    private final ElasticsearchProperties properties;

    private final RagContainer ragContainer;

    @Override
    public void afterPropertiesSet() {
        try {
            RestClient restClient = createRestClient(this.properties);

            // 查询所有索引,并为每个索引注册一个 Bean
            List<String> indexNames = queryAllIndices(restClient, properties);
            log.info("Found {} indices in Elasticsearch: {}", indexNames.size(), indexNames);

            for (String indexName : indexNames) {
                registerEmbeddingStoreBeanInternal(ragContainer, indexName, properties);
            }

        } catch (Exception e) {
            log.error("Failed to dynamically register embedding stores", e);
            throw new RuntimeException("Failed to initialize dynamic embedding stores", e);
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

        // 确保默认索引名称也被包含（向后兼容）
        if (properties.getIndexName() != null && !indexNames.contains(properties.getIndexName())) {
            indexNames.add(properties.getIndexName());
        }

        return indexNames;
    }

    /**
     * 内部方法：为指定索引注册 EmbeddingStore Bean（启动时使用）
     */
    private void registerEmbeddingStoreBeanInternal(RagContainer ragContainer,
                                           String indexName,
                                           ElasticsearchProperties properties) {
        String beanName = doRegisterEmbeddingStoreBean(ragContainer, indexName, properties);
        if (beanName != null) {
            log.info("Registered EmbeddingStore bean: {} for index: {}", beanName, indexName);
        } else {
            log.error("Failed to register EmbeddingStore for index: {}", indexName);
        }
    }

    /**
     *动态注册 EmbeddingStore Bean 到 Spring IOC 容器
     *
     * <p>外部服务可以通过此方法动态创建和注册新的 EmbeddingStore Bean</p>
     *
     * @param indexName Elasticsearch 索引名称
     * @return 注册成功的 Bean 名称，失败返回 null
     * @throws IllegalStateException 如果在 Spring 容器初始化完成前调用此方法
     */
    public String registerEmbeddingStoreBean(String indexName) {
        return doRegisterEmbeddingStoreBean(this.ragContainer, indexName, this.properties);
    }

    /**
     * 核心注册逻辑：创建并注册 EmbeddingStore Bean
     *
     * @param beanFactory Spring Bean 工厂
     * @param indexName Elasticsearch 索引名称
     * @param properties Elasticsearch 配置属性
     * @return 注册成功的 Bean 名称，失败返回 null
     */
    private String doRegisterEmbeddingStoreBean(RagContainer ragContainer,
                                                String indexName,
                                                ElasticsearchProperties properties) {
        try {
            // 生成 Bean 名称
            String beanName = "embedding_store:" + indexName;

            // 检查 Bean 是否已存在
            if (ragContainer.containsEmbeddingStore(beanName)) {
                log.warn("EmbeddingStore bean already exists: {} for index: {}", beanName, indexName);
                return beanName;
            }

            // 为每个索引创建独立的 RestClient
            RestClient restClient = createRestClient(properties);

            // 创建 EmbeddingStore
            EmbeddingStore<TextSegment> embeddingStore = ElasticsearchEmbeddingStore.builder()
                    .restClient(restClient)
                    .indexName(indexName)
                    .build();

            // 使用 registerSingleton 直接注册实例
            ragContainer.registerEmbeddingStore(beanName, embeddingStore);

            log.info("Successfully registered EmbeddingStore bean: {} for index: {}", beanName, indexName);
            return beanName;
        } catch (Exception e) {
            log.error("Failed to register EmbeddingStore for index: {}", indexName, e);
            return null;
        }
    }

}
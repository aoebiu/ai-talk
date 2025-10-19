package info.mengnan.aitalk.server.config;

import com.alibaba.dashscope.tokenizers.QwenTokenizer;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.aitalk.rag.ChatService;
import info.mengnan.aitalk.rag.config.ElasticsearchProperties;
import info.mengnan.aitalk.rag.container.assemble.AssembledModelsConstruct;
import info.mengnan.aitalk.rag.container.assemble.DynamicEmbeddingStoreRegistry;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.rag.container.RagContainer;
import info.mengnan.aitalk.rag.container.factory.CapableModelFactory;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.service.ChatApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG组件配置类
 * 负责将RAG模块的组件手动创建并注册到Spring容器中
 */
@Slf4j
@Configuration
public class RagConfiguration {

    /**
     * 创建Elasticsearch配置属性Bean
     */
    @Bean
    @ConfigurationProperties(prefix = "elasticsearch")
    public ElasticsearchProperties elasticsearchProperties() {
        return new ElasticsearchProperties();
    }

    /**
     * 创建CapableModelFactory
     */
    @Bean
    public CapableModelFactory createModelFactory() {
        log.info("Creating CapableModelFactory...");
        return new CapableModelFactory();
    }

    @Bean
    public RagContainer createRagContainer() {
        log.info("Creating RagContainer...");
        return new RagContainer();
    }

    /**
     * 创建并初始化ModelRegistry
     */
    @Bean
    public ModelRegistry modelRegistry(ChatApiKeyService chatApiKeyService,
                                       CapableModelFactory factory,
                                       RagContainer ragContainer) {
        log.info("Creating and initializing ModelRegistry...");

        // 从数据库查询所有模型配置
        List<ChatApiKey> apiKeys = chatApiKeyService.findAll();

        List<ModelConfig> modelConfigs = apiKeys.stream()
                .map(this::modelConfig)
                .collect(Collectors.toList());

        // 创建 ModelRegistry 并初始化
        ModelRegistry modelRegistry = new ModelRegistry(factory, ragContainer);
        modelRegistry.initialize(modelConfigs);

        log.info("ModelRegistry initialized successfully with {} models", modelConfigs.size());
        return modelRegistry;
    }

    /**
     * 创建并初始化DynamicEmbeddingStoreRegistry
     */
    @Bean
    public DynamicEmbeddingStoreRegistry dynamicEmbeddingStoreRegistry(
            ElasticsearchProperties elasticsearchProperties,
            RagContainer ragContainer) {
        log.info("Creating and initializing DynamicEmbeddingStoreRegistry...");
        DynamicEmbeddingStoreRegistry registry = new DynamicEmbeddingStoreRegistry(
                elasticsearchProperties,
                ragContainer
        );
        try {
            registry.initialize();
            log.info("DynamicEmbeddingStoreRegistry initialized successfully");
            return registry;
        } catch (Exception e) {
            log.error("DynamicEmbeddingStoreRegistry initialized fail!");
        }
        return registry;
    }

    /**
     * 创建AssembledModelsConstruct
     */
    @Bean
    public AssembledModelsConstruct assembledModelsConstruct() {
        log.info("Creating AssembledModelsConstruct...");
        return new AssembledModelsConstruct();
    }

    /**
     * 创建ChatService
     */
    @Bean
    public ChatService chatService(ChatMemoryStore chatMemoryStore, RagContainer ragContainer) {
        log.info("Creating ChatService...");
        return new ChatService(
                chatMemoryStore,
                ragContainer
        );
    }

    /**
     * 创建Tokenizer
     */
    @Bean
    public Tokenizer tokenizer() {
        log.info("Creating QwenTokenizer...");
        return new QwenTokenizer();
    }


    private ModelConfig modelConfig(ChatApiKey apiKey) {
        ModelConfig config = new ModelConfig();
        config.setModelName(apiKey.getModelName());
        config.setApiKey(apiKey.getApiKey());
        config.setModelProvider(apiKey.getModelProvider());
        config.setKeyType(apiKey.getKeyType());
        return config;
    }
}
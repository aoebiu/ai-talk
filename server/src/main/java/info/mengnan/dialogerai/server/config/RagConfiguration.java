package info.mengnan.dialogerai.server.config;

import com.alibaba.dashscope.tokenizers.QwenTokenizer;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.dialogerai.rag.ChatService;
import info.mengnan.dialogerai.rag.config.DefaultModelConfig;
import info.mengnan.dialogerai.kb.config.ElasticsearchProperties;
import info.mengnan.dialogerai.rag.container.assemble.AssembledModelsConstruct;
import info.mengnan.dialogerai.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.dialogerai.rag.container.factory.CapableModelFactory;
import info.mengnan.dialogerai.rag.container.factory.UniversalModelFactory;
import info.mengnan.dialogerai.rag.service.PromptTemplateManager;
import info.mengnan.dialogerai.rag.injector.RagSourceStore;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import info.mengnan.dialogerai.rag.service.SingleModelConfigProvider;
import info.mengnan.dialogerai.server.service.ModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;


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
    public UniversalModelFactory createModelFactory() {
        log.info("Creating CapableModelFactory...");
        return new CapableModelFactory();
    }

    /**
     * 创建 DynamicEmbeddingStoreRegistry
     */
    @Bean
    public DynamicEmbeddingStoreRegistry dynamicEmbeddingStoreRegistry(
            ElasticsearchProperties elasticsearchProperties) {
        log.info("Creating DynamicEmbeddingStoreRegistry...");
        // 注册销毁回调
        return new DynamicEmbeddingStoreRegistry(elasticsearchProperties);
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
    public ChatService chatService(ChatMemoryStore chatMemoryStore,
                                   UniversalModelFactory modelFactory,
                                   DynamicEmbeddingStoreRegistry embeddingStoreRegistry,
                                   ModelConfigService modelConfigService,
                                   RagSourceStore ragSourceStore,
                                   @Qualifier("ragExecutor") Executor ragExecutor) {
        log.info("Creating ChatService...");
        return new ChatService(chatMemoryStore,
                modelFactory,
                embeddingStoreRegistry,
                modelConfigService::loadModelConfigs,
                ragSourceStore,
                ragExecutor);
    }

    /**
     * 创建Tokenizer
     */
    @Bean
    public Tokenizer tokenizer() {
        log.info("Creating QwenTokenizer...");
        return new QwenTokenizer();
    }

    @Bean
    public DefaultModelConfig modelConfig() {
        return new DefaultModelConfig();
    }

    @Bean
    public PromptTemplateManager promptTemplateManager() {
        return new PromptTemplateManager();
    }

    @Bean
    public DirectModelInvoker directModelInvoker(UniversalModelFactory modelFactory,
                                                 ModelConfigService modelConfigService,
                                                 DefaultModelConfig modelConfig,
                                                 PromptTemplateManager promptTemplateManager) {
        return new DirectModelInvoker(modelFactory, modelConfigService::findModel, promptTemplateManager, modelConfig);
    }

}
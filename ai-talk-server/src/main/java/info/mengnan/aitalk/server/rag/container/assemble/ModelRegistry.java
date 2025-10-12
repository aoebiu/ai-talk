package info.mengnan.aitalk.server.rag.container.assemble;

import com.alibaba.dashscope.tokenizers.QwenTokenizer;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.service.*;
import info.mengnan.aitalk.repository.entity.ChatApiKey;
import info.mengnan.aitalk.repository.service.ChatApiKeyService;
import info.mengnan.aitalk.server.common.ModelType;
import info.mengnan.aitalk.server.rag.container.RagContainer;
import info.mengnan.aitalk.server.rag.container.factory.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class ModelRegistry implements InitializingBean {

    private final ChatApiKeyService chatApiKeyService;
    private final RagContainer ragContainer;

    /**
     * 通义千问 Tokenizer
     */
    @Bean
    public Tokenizer tokenizer() {
        return new QwenTokenizer();
    }


    /**
     * 在Bean属性设置完成后，从数据库加载所有API Keys并注册到Spring容器
     */
    @Override
    public void afterPropertiesSet() {
        Map<ModelType, List<ChatApiKey>> apiKeyMap = chatApiKeyService.findAll().stream()
                .collect(Collectors.groupingBy(e -> ModelType.valueOf(e.getKeyType().toUpperCase(Locale.ROOT))));

        for (Map.Entry<ModelType, List<ChatApiKey>> entry : apiKeyMap.entrySet()) {
            ModelType keyType = entry.getKey();
            List<ChatApiKey> apiKeys = entry.getValue();

            for (ChatApiKey apiKey : apiKeys) {
                String modelName = apiKey.getModelName();
                try {
                    registerModelBean(keyType, modelName, apiKey);
                    log.info("Successfully registered {} model bean: {} (provider: {}, model: {})",
                            keyType, modelName, apiKey.getModelProvider(), apiKey.getModelName());
                } catch (Exception e) {
                    log.error("Failed to register {} model bean: {} - {}",
                            keyType, modelName, e.getMessage(), e);
                }
            }
        }
    }

    private String generateBeanName(ModelType keyType, String modelName) {
        return keyType.n() + ":" + modelName;
    }

    /**
     * 根据类型注册模型Bean到Spring容器
     */
    private void registerModelBean(ModelType keyType, String beanName, ChatApiKey apiKey) {
        switch (keyType) {
            case CHAT:
                ChatModel chatModel = ModelFactory.createChatModel(apiKey);
                ragContainer.registerChatModel(beanName, chatModel);
                break;

            case STREAMING_CHAT:
                StreamingChatModel streamingChatModel = ModelFactory.createStreamingChatModel(apiKey);
                ragContainer.registerStreamingChatModel(beanName, streamingChatModel);
                break;

            case EMBEDDING:
                EmbeddingModel embeddingModel = ModelFactory.createEmbeddingModel(apiKey);
                ragContainer.registerEmbeddingModel(beanName, embeddingModel);
                break;

            case SCORING:
                ScoringModel scoringModel = ModelFactory.createScoringModel(apiKey);
                ragContainer.registerScoringModel(beanName, scoringModel);
                break;

            default:
                throw new IllegalArgumentException("Unsupported key type: " + keyType);
        }
    }


    public interface AssistantUnique {
        @SystemMessage(fromResource = "rag/customer_message.txt")
        TokenStream chatStreaming(@MemoryId String memoryId, @UserMessage String userMessage);
    }

}
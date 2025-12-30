package info.mengnan.aitalk.rag.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import info.mengnan.aitalk.common.param.ModelType;
import info.mengnan.aitalk.rag.config.DefaultModelConfig;
import info.mengnan.aitalk.rag.config.ModelConfig;
import info.mengnan.aitalk.rag.container.assemble.ModelRegistry;
import info.mengnan.aitalk.rag.container.factory.ModelTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 直接模型调用器
 */
@Slf4j
@RequiredArgsConstructor
public class DirectModelInvoker {

    private final ModelRegistry modelRegistry;
    private final ModelConfigProvider modelConfigProvider;
    private final PromptTemplateManager promptTemplateManager;
    private final DefaultModelConfig defaultModelConfig;

    // 重试配置
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_DELAY_MS = 1000L;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public String directInvoke(String templateName, Map<String, Object> variables, ModelType modelType) {
        if (templateName == null) {
            log.error("No template name provided");
            throw new IllegalArgumentException("No template name provided");
        }
        ModelConfig modelConfig = modelConfigProvider.findModel(null, defaultModelConfig.getModelName(), modelType);
        if (modelConfig == null) {
            log.error("No default model configuration found for type: {}", modelType);
            throw new IllegalArgumentException("No default model configuration found for type: " + modelType);
        }

        ChatModel chatModel = getModel(modelConfig, ChatModel.class);
        Prompt prompt = promptTemplateManager.createPrompt(templateName, variables);

        return executeWithRetry(() -> {
            String response = chatModel.chat(prompt.text());
            return UserMessage.from(response).singleText();
        }, MAX_ATTEMPTS, BACKOFF_DELAY_MS, BACKOFF_MULTIPLIER);
    }

    /**
     * 执行带重试的操作
     * @param operation 要执行的操作
     * @param maxAttempts 最大尝试次数
     * @param initialDelayMs 延迟毫秒数
     * @param multiplier 延迟倍增因子
     * @return 操作结果
     */
    private <T> T executeWithRetry(Supplier<T> operation, int maxAttempts, long initialDelayMs, double multiplier) {
        Exception lastException = null;
        long delayMs = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                if (e.getMessage() != null && e.getMessage().startsWith("4")) {
                    log.warn("Client error occurred, not retrying: {}", e.getMessage());
                    break;
                }

                // 如果不是最后一次尝试，等待后重试
                if (attempt < maxAttempts) {
                    // 添加抖动避免雪崩
                    long jitter = ThreadLocalRandom.current().nextLong(delayMs / 2);
                    long sleepTime = delayMs + jitter;

                    log.warn("Attempt {} failed, retrying in {}ms... Error:", attempt, sleepTime, e);

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation was interrupted", ie);
                    }

                    // 指数退避
                    delayMs = (long) (delayMs * multiplier);
                }
            }
        }
        if(lastException !=null) {
            throw new RuntimeException(String.format(
                    "Operation failed after %d attempts. Last error: %s", maxAttempts, lastException.getMessage()), lastException);
        }
        throw new RuntimeException();
    }

    public <T> T getModel(ModelConfig modelConfig, Class<T> modelClass) {
        try {
            ModelTypeMapper mapper = ModelTypeMapper.findByClass(modelClass);
            return mapper.create(modelRegistry, modelConfig);
        } catch (Exception e) {
            log.error("Failed to create model: {}", modelConfig.getModelName(), e);
            throw new RuntimeException("Failed to create model: " + e.getMessage(), e);
        }
    }
}
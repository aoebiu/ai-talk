package info.mengnan.dialogerai.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步配置：启用 @Async 注解，并为文档处理流水线提供专用线程池。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 文档处理专用线程池。
     * 核心线程2个保证并发，最大4个应对突发，队列50控制背压。
     */
    @Bean("docProcessPool")
    public Executor docProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-process-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

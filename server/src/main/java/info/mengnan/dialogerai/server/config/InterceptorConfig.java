package info.mengnan.dialogerai.server.config;

import info.mengnan.dialogerai.server.interceptor.OpenAiApiKeyInterceptor;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final OpenAiApiKeyInterceptor openAiApiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加OpenAI API Key拦截器
        registry.addInterceptor(openAiApiKeyInterceptor)
                .addPathPatterns("/v1/chat/completions")
                .order(1);

        // 登录检查
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/member/login", "/api/member/register")
                .order(2);
    }
}
package info.mengnan.aitalk.server.interceptor;

import info.mengnan.aitalk.repository.entity.ChatProjectApiKey;
import info.mengnan.aitalk.repository.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * OpenAI API Key 拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取 Authorization header
        String authorization = request.getHeader("Authorization");

        if (authorization == null || authorization.trim().isEmpty()) {
            log.warn("Missing Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":{\"message\":\"Missing Authorization header\",\"type\":\"invalid_request_error\"}}");
            return false;
        }

        String apiKey = authorization.replace("Bearer ", "").trim();

        if (!apiKey.startsWith("sk-")) {
            log.warn("Invalid API Key format, must start with 'sk-': {}", authorization);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":{\"message\":\"Invalid API Key format, must start with 'sk-'\",\"type\":\"invalid_request_error\"}}");
            return false;
        }

        // 验证 API Key
        ChatProjectApiKey validatedKey = apiKeyService.validateApiKey(apiKey);
        if (validatedKey == null) {
            log.warn("Invalid or expired API Key: {}", apiKey);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":{\"message\":\"Invalid or expired API Key\",\"type\":\"invalid_request_error\"}}");
            return false;
        }

        log.debug("API Key validated successfully for member: {}", validatedKey.getMemberId());
        return true;
    }
}
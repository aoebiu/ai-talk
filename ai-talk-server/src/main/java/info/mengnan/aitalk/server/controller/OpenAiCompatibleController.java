package info.mengnan.aitalk.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.rag.container.AssembledModels;
import info.mengnan.aitalk.rag.handler.StreamingResponseHandler;
import info.mengnan.aitalk.server.param.ChatRequest;
import info.mengnan.aitalk.rag.ChatService;
import info.mengnan.aitalk.server.handler.OpenAiStreamingResponseHandler;
import info.mengnan.aitalk.server.param.openai.OpenApiChatRequest;
import info.mengnan.aitalk.server.service.RagAdapterService;
import info.mengnan.aitalk.server.service.ToolAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI 兼容的 API 控制器
 * 提供标准的 OpenAI API 格式接口,可以通过本接口从第三方客户端执行
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiCompatibleController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final RagAdapterService ragAdapterService;
    private final ToolAdapterService toolAdapterService;

    // TODO 全局配置中获取
    private final static Long DEFAULT_OPTION_ID = 1L;
    private final static String DEFAULT_SESSION = "default-session";

    /**
     * OpenAI 兼容的聊天接口
     */
    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatCompletions(@RequestBody OpenApiChatRequest request,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!request.getStream()) {
            return Flux.error(new UnsupportedOperationException("当前仅支持流式响应,请设置 stream=true"));
        }

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return Flux.error(new IllegalArgumentException("messages 不能为空"));
        }

        log.info("收到 OpenAI请求 - model: {}, messages: {}",
                request.getModel(), request.getMessages().size());

        ChatRequest chatRequest = convertToInternalRequest(request);
        return streamResponse(chatRequest, request.getModel());

    }

    /**
     * 流式响应 - 返回 SSE 格式
     * 使用回调接口将 ChatService 的响应转换为 Flux
     */
    private Flux<String> streamResponse(ChatRequest chatRequest, String model) {
        String requestId = "chatcmpl-" + UUID.randomUUID();
        long timestamp = System.currentTimeMillis() / 1000;

        return Flux.<String>create(sink -> {
            try {
                // 从数据库查询并组装 AssembledModels
                AssembledModels assembledModels = ragAdapterService.assembleModels(chatRequest.getOptionId());
                Map<ToolSpecification, ToolExecutor> toolMap = toolAdapterService.dynamicTools();

                StreamingResponseHandler handler = new OpenAiStreamingResponseHandler(
                        sink, objectMapper, requestId, timestamp, model);

                chatService.chatStreaming(chatRequest.getSessionId(),chatRequest.getMessage(), handler, assembledModels,toolMap);
            } catch (Exception e) {
                log.error("组装模型配置失败", e);
                sink.error(e);
            }
        })
        .delayElements(Duration.ofMillis(1));
    }

    /**
     * 将 OpenAI 请求转换为内部请求格式
     */
    private ChatRequest convertToInternalRequest(OpenApiChatRequest openAiRequest) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setInDB(false);
        chatRequest.setSessionId(DEFAULT_SESSION);
        chatRequest.setOptionId(DEFAULT_OPTION_ID);

        // 获取最后一条用户消息作为当前消息
        String message = openAiRequest.getMessages().stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .reduce((first, second) -> second)
                .map(OpenApiChatRequest.Message::getContent)
                .orElse("");

        chatRequest.setMessage(message);
        return chatRequest;
    }
}
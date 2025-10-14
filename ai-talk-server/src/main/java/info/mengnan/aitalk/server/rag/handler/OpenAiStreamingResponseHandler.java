package info.mengnan.aitalk.server.rag.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.mengnan.aitalk.server.param.openai.OpenApiChatStreamResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;

import java.util.List;

/**
 * OpenAI 兼容的流式响应处理器
 * 用于将 ChatService 的流式响应转换为 OpenAI 格式的 SSE 流
 */
@Slf4j
public class OpenAiStreamingResponseHandler implements StreamingResponseHandler {

    private final FluxSink<String> sink;
    private final ObjectMapper objectMapper;
    private final String requestId;
    private final long timestamp;
    private final String model;

    public OpenAiStreamingResponseHandler(FluxSink<String> sink, ObjectMapper objectMapper,
                                         String requestId, long timestamp, String model) {
        this.sink = sink;
        this.objectMapper = objectMapper;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.model = model;
    }

    @Override
    public void onToken(String token) {
        if (!sink.isCancelled()) {
            // 构建流式响应对象
            OpenApiChatStreamResponse response = OpenApiChatStreamResponse.builder()
                    .id(requestId)
                    .object("chat.completion.chunk")
                    .created(timestamp)
                    .model(model)
                    .choices(List.of(
                            OpenApiChatStreamResponse.Choice.builder()
                                    .index(0)
                                    .delta(OpenApiChatStreamResponse.Delta.builder()
                                            .content(token)
                                            .build())
                                    .finishReason(null)
                                    .build()
                    ))
                    .build();
            sink.next(toFormat(response));
        }
    }

    @Override
    public void onComplete(String completeResponse) {
        if (!sink.isCancelled()) {
            // 发送完成消息
            sink.next(createDoneMessage());
            sink.next("data: [DONE]\n\n");
            sink.complete();
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("流式响应失败", error);
        if (!sink.isCancelled()) {
            sink.next(createErrorMessage(error.getMessage()));
            sink.error(error);
        }
    }

    @Override
    public boolean isCancelled() {
        return sink.isCancelled();
    }

    /**
     * 将响应对象转换为 SSE 格式
     */
    private String toFormat(OpenApiChatStreamResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            return "data: " + json + "\n\n";
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return "";
        }
    }

    /**
     * 创建完成消息
     */
    private String createDoneMessage() {
        OpenApiChatStreamResponse doneResponse = OpenApiChatStreamResponse.builder()
                .id(requestId)
                .object("chat.completion.chunk")
                .created(timestamp)
                .model(model)
                .choices(List.of(
                        OpenApiChatStreamResponse.Choice.builder()
                                .index(0)
                                .delta(OpenApiChatStreamResponse.Delta.builder().build())
                                .finishReason("stop")
                                .build()
                ))
                .build();

        return toFormat(doneResponse);
    }

    /**
     * 创建错误消息
     */
    private String createErrorMessage(String errorMessage) {
        return String.format("data: {\"error\": {\"message\": \"%s\", \"type\": \"internal_error\"}}\n\n",
                errorMessage.replace("\"", "\\\""));
    }
}
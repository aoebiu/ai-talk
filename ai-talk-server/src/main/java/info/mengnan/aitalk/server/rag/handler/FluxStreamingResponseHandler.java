package info.mengnan.aitalk.server.rag.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;

/**
 * Flux 流式响应处理器
 * 用于将 ChatService 的流式响应转换为 Reactor Flux 流
 */
@Slf4j
public class FluxStreamingResponseHandler implements StreamingResponseHandler {

    private final FluxSink<String> sink;
    private final String sessionId;

    public FluxStreamingResponseHandler(FluxSink<String> sink, String sessionId) {
        this.sink = sink;
        this.sessionId = sessionId;
    }

    @Override
    public void onToken(String token) {
        if (!sink.isCancelled()) {
            sink.next(token);
        }
    }

    @Override
    public void onComplete(String completeResponse) {
        if (!sink.isCancelled()) {
            sink.complete();
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("流式响应失败,sessionId: {}", sessionId, error);
        if (!sink.isCancelled()) {
            sink.error(error);
        }
    }

    @Override
    public boolean isCancelled() {
        return sink.isCancelled();
    }
}
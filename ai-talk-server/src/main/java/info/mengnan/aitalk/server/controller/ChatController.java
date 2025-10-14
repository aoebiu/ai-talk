package info.mengnan.aitalk.server.controller;

import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.service.ChatMessageService;
import info.mengnan.aitalk.server.param.common.ChatRequest;
import info.mengnan.aitalk.server.param.common.R;
import info.mengnan.aitalk.server.rag.ChatService;
import info.mengnan.aitalk.server.rag.handler.FluxStreamingResponseHandler;
import info.mengnan.aitalk.server.rag.handler.StreamingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

import static info.mengnan.aitalk.server.param.common.MessageRole.*;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;

    /**
     * 流式对话接口 - 使用 HTTP Streaming (application/stream+json)
     */
    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空"));
        }

        log.info("收到流式对话请求 - sessionId: {}, message: {}", request.getSessionId(), request.getMessage());
        return streamResponse(request);
    }

    /**
     * 流式响应 - 返回纯文本流
     * 使用回调接口将 ChatService 的响应转换为 Flux
     */
    private Flux<String> streamResponse(ChatRequest chatRequest) {
        return Flux.create(sink -> {
            // 创建回调处理器,将回调转换为 Flux 事件
            StreamingResponseHandler handler = new FluxStreamingResponseHandler(sink, chatRequest.getSessionId());

            // 调用 ChatService 的流式方法
            chatService.chatStreaming(chatRequest, handler);
        });
    }

    /**
     * 对话列表
     */
    @PostMapping(value = "/history/{sessionId}")
    public R history(@PathVariable String sessionId) {
        List<ChatMessage> list = chatMessageService.findChatByRole(sessionId,
                List.of(ASSISTANT.n(), SYSTEM.n(), USER.n()));
        return R.ok(list);
    }


    // todo 提供接口,返回给前端sessionId(新加一张表)
    // todo 提供接口,设计修改指定的用户消息,并且删除此条消息之后发出的消息

    /**
     * 清空会话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public R clearHistory(@PathVariable String sessionId) {
        chatMessageService.deleteBySessionId(sessionId);
        return R.ok("会话历史已清空");
    }
}
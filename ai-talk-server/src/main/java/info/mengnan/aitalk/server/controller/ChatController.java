package info.mengnan.aitalk.server.controller;

import info.mengnan.aitalk.server.common.ChatRequest;
import info.mengnan.aitalk.server.common.R;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.service.ChatMessageService;
import info.mengnan.aitalk.server.rag.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

import static info.mengnan.aitalk.server.common.MessageRole.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;

    /**
     * 流式对话接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        log.info("收到流式对话请求 - sessionId: {}, message: {}", request.getSessionId(), request.getMessage());
        return chatService.chatStreaming(request);
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
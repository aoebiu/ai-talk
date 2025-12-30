package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.rag.container.assemble.AssembledModels;
import info.mengnan.aitalk.rag.handler.StreamingResponseHandler;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.entity.ChatSession;
import info.mengnan.aitalk.repository.service.ChatMessageService;
import info.mengnan.aitalk.repository.service.ChatSessionService;
import info.mengnan.aitalk.server.param.ChatRequest;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.rag.ChatService;
import info.mengnan.aitalk.server.handler.FluxStreamingResponseHandler;
import info.mengnan.aitalk.server.param.chat.ChatSessionResult;
import info.mengnan.aitalk.server.service.RagAdapterService;
import info.mengnan.aitalk.server.service.ToolAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static info.mengnan.aitalk.common.param.MessageRole.*;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final RagAdapterService ragAdapterService;
    private final ToolAdapterService toolAdapterService;

    /**
     * 流式对话接口 - 使用 HTTP Streaming (application/x-ndjson)
     */
    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空"));
        }
        if (chatSessionService.findLastBySessionId(sessionId) == null) {
            throw new IllegalArgumentException("sessionId 不存在");
        }
        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        return streamResponse(request);
    }

    @GetMapping(value = "/createChat")
    public R createChat() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatSession chatSession = chatSessionService.findLastByMemberId(memberId);
        if (chatSession != null) {
            List<ChatMessage> chatMessageList = chatMessageService.findChat(chatSession.getChatSessionId());
            if (chatMessageList.isEmpty()) {
                ChatSessionResult sessionResult = new ChatSessionResult(chatSession.getChatSessionId(), "新对话");
                return R.ok(sessionResult);
            }
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        ChatSession session = new ChatSession();
        session.setChatSessionId(sessionId);
        session.setMemberId(StpUtil.getLoginIdAsLong());
        session.setTitle("新对话");
        chatSessionService.createChat(session);
        ChatSessionResult sessionResult = new ChatSessionResult(session.getChatSessionId(), "新对话");
        return R.ok(sessionResult);
    }

    /**
     * 流式响应 - 返回纯文本流
     * 使用回调接口将 ChatService 的响应转换为 Flux
     */
    private Flux<String> streamResponse(ChatRequest chatRequest) {
        return Flux.create(sink -> {
            try {
                // 组装 AssembledModels
                AssembledModels assembledModels = ragAdapterService.assembleModels(chatRequest.getOptionId());
                Map<ToolSpecification, ToolExecutor> toolMap = toolAdapterService.dynamicTools();
                // 创建回调处理器
                StreamingResponseHandler handler = new FluxStreamingResponseHandler(sink, chatRequest.getSessionId());

                // 调用 ChatService 的流式方法
                chatService.chatStreaming(
                        chatRequest.getMemberId(),
                        chatRequest.getSessionId(),
                        chatRequest.getMessage(),
                        handler, assembledModels, toolMap);
            } catch (Exception e) {
                sink.error(e);
            }
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


    // todo 提供接口,设计修改指定的用户消息,并且删除此条消息之后发出的消息

    /**
     * 清空会话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public R clearHistory(@PathVariable String sessionId) {
        chatMessageService.deleteBySessionId(sessionId);
        return R.ok();
    }
}
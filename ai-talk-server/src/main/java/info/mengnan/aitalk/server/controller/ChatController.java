package info.mengnan.aitalk.server.controller;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.rag.container.AssembledModels;
import info.mengnan.aitalk.rag.handler.StreamingResponseHandler;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.service.ChatMessageService;
import info.mengnan.aitalk.server.param.ChatRequest;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.rag.ChatService;
import info.mengnan.aitalk.server.handler.FluxStreamingResponseHandler;
import info.mengnan.aitalk.server.service.RagAdapterService;
import info.mengnan.aitalk.server.service.ToolAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static info.mengnan.aitalk.common.param.MessageRole.*;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
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

        log.info("收到流式对话请求 - sessionId: {}, message: {}", request.getSessionId(), request.getMessage());
        return streamResponse(request);
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
                chatService.chatStreaming(chatRequest.getSessionId(), chatRequest.getMessage(), handler, assembledModels, toolMap);
            } catch (Exception e) {
                log.error("组装模型配置失败", e);
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
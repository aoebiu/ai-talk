package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.kb.core.KnowledgeBaseIndexResolver;
import info.mengnan.aitalk.rag.container.assemble.AssembledModels;
import info.mengnan.aitalk.rag.handler.StreamingResponseHandler;
import info.mengnan.aitalk.rag.service.DirectModelInvoker;
import info.mengnan.aitalk.repository.entity.ChatMessage;
import info.mengnan.aitalk.repository.entity.ChatSession;
import info.mengnan.aitalk.server.service.ChatMessageService;
import info.mengnan.aitalk.repository.repo.ChatSessionRepository;
import info.mengnan.aitalk.server.core.DbKnowledgeBaseIndexResolver;
import info.mengnan.aitalk.server.param.chat.ChatRequest;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.rag.ChatService;
import info.mengnan.aitalk.server.handler.FluxStreamingResponseHandler;
import info.mengnan.aitalk.server.param.chat.ChatConversations;
import info.mengnan.aitalk.server.param.chat.ChatHistoryResponse;
import info.mengnan.aitalk.server.param.chat.ChatSessionResponse;
import info.mengnan.aitalk.server.param.chat.RagSourceDto;
import info.mengnan.aitalk.server.service.RagAdapterService;
import info.mengnan.aitalk.server.service.ToolAdapterService;
import info.mengnan.aitalk.repository.repo.ChatMessageRagSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static info.mengnan.aitalk.common.param.MessageRole.*;
import static info.mengnan.aitalk.server.param.chat.ChatSessionResponse.DEFAULT_TITLE;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageService chatMessageService;
    private final RagAdapterService ragAdapterService;
    private final ToolAdapterService toolAdapterService;
    private final DirectModelInvoker directModelInvoker;
    private final ChatMessageRagSourceRepository ragSourceRepository;
    private final KnowledgeBaseIndexResolver knowledgeBaseIndexResolver;

    /**
     * 流式对话接口 - 使用 HTTP Streaming (application/x-ndjson)
     */
    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空"));
        }
        if (chatSessionRepository.findBySessionId(sessionId) == null) {
            return Flux.error(new IllegalArgumentException("sessionId 不存在"));
        }
        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        return streamResponse(request);
    }

    @GetMapping(value = "/createChat")
    public R createChat() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatSession chatSession = chatSessionRepository.findLastByMemberId(memberId);
        if (chatSession != null) {
            List<ChatMessage> chatMessageList = chatMessageService.findBySessionId(chatSession.getChatSessionId());
            if (chatMessageList.isEmpty()) {
                return R.ok();
            }
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        ChatSession session = new ChatSession();
        session.setChatSessionId(sessionId);
        session.setMemberId(StpUtil.getLoginIdAsLong());
        session.setTitle(DEFAULT_TITLE);
        chatSessionRepository.createChat(session);
        ChatSessionResponse sessionResult = new ChatSessionResponse(sessionId, DEFAULT_TITLE, session.getUpdatedAt());
        return R.ok(sessionResult);
    }


    @GetMapping(value = "/conversations")
    public R conversations(@RequestParam("sessionId") String sessionId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatConversations chatConversations = new ChatConversations(memberId,sessionId);

        ChatSession chatSession = chatSessionRepository.findBySessionId(sessionId);
        if (chatSession != null && DEFAULT_TITLE.equals(chatSession.getTitle())) {
            List<String> list = chatMessageService.findChat(sessionId, List.of(ASSISTANT.n(), USER.n())).stream()
                    .map(ChatMessage::getContent)
                    .limit(3)
                    .toList();
            if (list.size() >= 2) {
                Map<String, Object> params = Map.of("query", list);
                String title = directModelInvoker.directInvoke("conversations.titleGeneration",
                        "title_generation", params);
                chatConversations.setTitle(title);
                chatSessionRepository.updateChatTitle(sessionId, title);
            }
        }
        ChatMessage latest = chatMessageService.findLatest(sessionId, ASSISTANT.n());
        if (latest != null) {
            List<Long> sourceIds = ragSourceRepository.findIdsForMessage(latest.getId());
            if (!sourceIds.isEmpty()) chatConversations.setSourceIds(sourceIds);
        }

        return R.ok(chatConversations);
    }


    /**
     * 流式响应 - 返回纯文本流
     * 使用回调接口将 ChatService 的响应转换为 Flux
     */
    private Flux<String> streamResponse(ChatRequest chatRequest) {
        return Flux.create(sink -> {
            try {
                // 截断消息
                if (chatRequest.getFromMessageId() != null)
                    chatMessageService.truncateMessages(chatRequest.getSessionId(), chatRequest.getFromMessageId());

                // 组装 AssembledModels
                AssembledModels assembledModels = ragAdapterService.assembleModels(chatRequest.getOptionId());
                Map<ToolSpecification, ToolExecutor> toolMap = toolAdapterService.dynamicTools(chatRequest.getMemberId());
                List<KnowledgeBaseIndexResolver.KbIndexRef> kbIndexRefs = knowledgeBaseIndexResolver.resolveActiveIndexes(chatRequest.getMemberId());
                // 创建回调处理器
                StreamingResponseHandler handler = new FluxStreamingResponseHandler(sink, chatRequest.getSessionId());

                // 调用 ChatService 的流式方法
                chatService.chatStreaming(
                        chatRequest.getMemberId(),
                        chatRequest.getSessionId(),
                        chatRequest.getMessage(),
                        handler, assembledModels, toolMap, kbIndexRefs);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * 对话列表
     */
    @GetMapping(value = "/history/{sessionId}")
    public R history(@PathVariable("sessionId") String sessionId) {
        List<ChatMessage> list = chatMessageService.findChat(sessionId,
                List.of(ASSISTANT.n(), SYSTEM.n(), USER.n()));
        Map<Long, List<Long>> ragSourceMap = ragSourceRepository.findRagSourceIdMap(sessionId);
        return R.ok(new ChatHistoryResponse(list, ragSourceMap));
    }

    /**
     * 获取指定知识库来源内容
     */
    @GetMapping("/ragSources")
    public R getRagSources(@RequestParam("ids") List<Long> ids) {
        List<RagSourceDto> sources = ragSourceRepository.find(ids).stream().map(RagSourceDto::from).toList();
        return R.ok(Map.of("sources", sources));
    }

    /**
     * 获取指定会话最新 assistant 消息的
     */
    @GetMapping("/ragSources/latest")
    public R getRagSourcesLatest(@RequestParam("sessionId") String sessionId) {
        ChatMessage latest = chatMessageService.findLatest(sessionId, ASSISTANT.n());
        List<Long> sourceIds = List.of();
        if (latest != null) {
            sourceIds = ragSourceRepository.findIdsForMessage(latest.getId());
        }
        return R.ok(Map.of("sourceIds", sourceIds));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public R clearHistory(@PathVariable(name = "sessionId") String sessionId) {
        chatMessageService.deleteBySessionId(sessionId);
        chatSessionRepository.deleteBySessionId(sessionId);
        return R.ok();
    }

    /**
     * 获取所有对话会话
     */
    @GetMapping(value = "/sessions")
    public R getAllSessions() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatSession> sessions = chatSessionRepository.findAllByMemberId(memberId);
        List<ChatSessionResponse> responses = sessions.stream()
                .map(session -> new ChatSessionResponse(session.getChatSessionId(), session.getTitle(), session.getUpdatedAt()))
                .toList();
        return R.ok(responses);
    }
}
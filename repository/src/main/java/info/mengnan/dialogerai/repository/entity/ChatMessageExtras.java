package info.mengnan.dialogerai.repository.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 持久化在 {@code chat_messages.extras} JSON 中的扩展字段，
 * 用于还原 LangChain4j 消息对象上未映射到 {@code content} 的语义
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChatMessageExtras {

    /** {@link dev.langchain4j.data.message.UserMessage#name()} */
    private String userName;

    /** {@link dev.langchain4j.data.message.AiMessage#thinking()} */
    private String thinking;

    /** {@link dev.langchain4j.data.message.AiMessage#toolExecutionRequests()} */
    private List<ToolExecutionRequestSnapshot> toolExecutionRequests;

    /** {@link dev.langchain4j.data.message.AiMessage#attributes()} */
    private Map<String, Object> attributes;

    /** {@link dev.langchain4j.data.message.ToolExecutionResultMessage#id()} */
    private String toolCallId;

    /** {@link dev.langchain4j.data.message.ToolExecutionResultMessage#toolName()} */
    private String toolName;

    /** 本条用户消息命中的知识库片段（仅查询时填充，不写入 DB：@JsonInclude(NON_EMPTY) 保护） */
    private List<RagSourceDto> ragSources;

    @Data
    public static class RagSourceDto {
        private String kbName;
        private String indexName;
        private String text;
    }

    @Data
    public static class ToolExecutionRequestSnapshot {
        private String id;
        private String name;
        private String arguments;
    }

}

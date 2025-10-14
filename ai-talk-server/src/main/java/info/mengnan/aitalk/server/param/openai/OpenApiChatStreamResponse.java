package info.mengnan.aitalk.server.param.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI 兼容的流式响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiChatStreamResponse {

    private String id;

    private String object = "chat.completion.chunk";

    private Long created;

    private String model;

    private List<Choice> choices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;

        private Delta delta;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {
        private String role;

        private String content;
    }
}
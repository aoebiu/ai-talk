package info.mengnan.aitalk.server.param.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的聊天请求格式
 */
@Data
public class OpenApiChatRequest {

    /**
     * 模型名称 (必需)
     */
    private String model;

    /**
     * 消息列表 (必需)
     */
    private List<Message> messages;

    /**
     * 是否流式返回
     */
    private Boolean stream = false;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature;

    /**
     * 最大生成 token 数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * Top P 采样
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * 频率惩罚 (-2.0 到 2.0)
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /**
     * 存在惩罚 (-2.0 到 2.0)
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 扩展字段 - 用于传递自定义参数
     */
    private Map<String, Object> metadata;

    @Data
    @JsonDeserialize(using = MessageContentDeserializer.class)
    public static class Message {
        /**
         * 角色: system, user, assistant
         */
        private String role;

        /**
         * 消息内容 - 字符串类型的简单消息
         */
        private String content;

        /**
         * 多模态消息内容部分 - 当content是数组时使用
         */
        private List<ContentPart> contentParts;

        /**
         * 消息名称(可选)
         */
        private String name;

        // 内部类定义
        @Data
        public static class ContentPart {
            private String type; // text, image_url

            private String text;

            @Data
            public static class ImageUrl {
                private String url;
                private String detail = "auto"; // low, high, auto
            }

            @JsonProperty("image_url")
            private ImageUrl imageUrl;
        }
    }
}
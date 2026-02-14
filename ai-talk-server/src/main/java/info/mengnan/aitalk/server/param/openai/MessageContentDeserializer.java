package info.mengnan.aitalk.server.param.openai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义反序列化器，处理 OpenAI content 字段的多态性
 * content 可以是字符串或对象数组
 */
public class MessageContentDeserializer extends JsonDeserializer<OpenApiChatRequest.Message> {

    @Override
    public OpenApiChatRequest.Message deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);

        OpenApiChatRequest.Message message = new OpenApiChatRequest.Message();

        // 解析 role
        if (node.has("role")) {
            message.setRole(node.get("role").asText());
        }

        // 解析 name
        if (node.has("name")) {
            message.setName(node.get("name").asText());
        }

        // 解析 content - 可能是字符串或数组
        if (node.has("content")) {
            JsonNode contentNode = node.get("content");

            if (contentNode.isTextual()) {
                // content 是字符串
                message.setContent(contentNode.asText());
            } else if (contentNode.isArray()) {
                // content 是数组（多模态内容）
                List<OpenApiChatRequest.Message.ContentPart> parts = new ArrayList<>();
                for (JsonNode partNode : contentNode) {
                    OpenApiChatRequest.Message.ContentPart part = mapper.treeToValue(partNode, OpenApiChatRequest.Message.ContentPart.class);
                    parts.add(part);
                }
                message.setContentParts(parts);
            }
        }

        return message;
    }
}

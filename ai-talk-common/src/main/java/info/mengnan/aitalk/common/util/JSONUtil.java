package info.mengnan.aitalk.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import info.mengnan.aitalk.common.json.JSONArray;
import info.mengnan.aitalk.common.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON工具类，基于Jackson实现
 *
 */
public class JSONUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 配置ObjectMapper
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert to JSON string", e);
        }
    }

    /**
     * 将对象转换为格式化的JSON字符串（美化）
     *
     * @param obj 对象
     * @return 格式化的JSON字符串
     */
    public static String toJsonPrettyStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert to JSON string", e);
        }
    }

    /**
     * 将JSON字符串解析为指定类型的对象
     *
     * @param jsonStr JSON字符串
     * @param clazz   目标类型
     * @param <T>     泛型
     * @return 对象
     */
    public static <T> T toBean(String jsonStr, Class<T> clazz) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }

    /**
     * 将JSON字符串解析为List
     *
     * @param jsonStr      JSON字符串
     * @param elementClass 元素类型
     * @param <T>          泛型
     * @return List
     */
    public static <T> List<T> toList(String jsonStr, Class<T> elementClass) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string to List", e);
        }
    }

    /**
     * 将JSON字符串解析为JSONObject
     *
     * @param jsonStr JSON字符串
     * @return JSONObject
     */
    public static JSONObject parseObj(String jsonStr) {
        return new JSONObject(jsonStr);
    }

    /**
     * 将对象转换为JSONObject
     *
     * @param obj 对象
     * @return JSONObject
     */
    public static JSONObject parseObj(Object obj) {
        return new JSONObject(obj);
    }

    /**
     * 将JSON字符串解析为JSONArray
     *
     * @param jsonStr JSON字符串
     * @return JSONArray
     */
    public static JSONArray parseArray(String jsonStr) {
        return new JSONArray(jsonStr);
    }

    /**
     * 将对象转换为JSONArray
     *
     * @param obj 对象
     * @return JSONArray
     */
    public static JSONArray parseArray(Object obj) {
        return new JSONArray(obj);
    }

    /**
     * 创建一个空的JSONObject
     *
     * @return JSONObject
     */
    public static JSONObject createObj() {
        return new JSONObject();
    }

    /**
     * 创建一个空的JSONArray
     *
     * @return JSONArray
     */
    public static JSONArray createArray() {
        return new JSONArray();
    }

    /**
     * 判断字符串是否为有效的JSON
     *
     * @param jsonStr JSON字符串
     * @return 是否为有效的JSON
     */
    public static boolean isJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(jsonStr);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为JSON对象
     *
     * @param jsonStr JSON字符串
     * @return 是否为JSON对象
     */
    public static boolean isJsonObj(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(jsonStr);
            return node.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为JSON数组
     *
     * @param jsonStr JSON字符串
     * @return 是否为JSON数组
     */
    public static boolean isJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(jsonStr);
            return node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 解析JSON字符串为JsonNode
     *
     * @param jsonStr JSON字符串
     * @return JsonNode
     */
    public static JsonNode parseNode(String jsonStr) {
        try {
            return OBJECT_MAPPER.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }

    /**
     * 将对象转换为JsonNode
     *
     * @param obj 对象
     * @return JsonNode
     */
    public static JsonNode valueToTree(Object obj) {
        return OBJECT_MAPPER.valueToTree(obj);
    }

    /**
     * 将JsonNode转换为对象（公共方法，供json包使用）
     *
     * @param node  JsonNode
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象
     */
    public static <T> T treeToValue(JsonNode node, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JsonNode", e);
        }
    }

    /**
     * 创建ObjectNode
     *
     * @return ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * 创建ArrayNode
     *
     * @return ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

    public static Map<String, String> jsonObjectToStrMap(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Map.of();
        }
        return new JSONObject(jsonStr).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.valueOf(e.getValue())
                ));

    }
}
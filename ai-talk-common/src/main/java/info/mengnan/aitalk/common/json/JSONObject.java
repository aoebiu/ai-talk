package info.mengnan.aitalk.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import info.mengnan.aitalk.common.util.JSONUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSONObject类，基于Jackson实现
 *
 */
public class JSONObject implements Map<String, Object> {

    private final ObjectNode node;

    /**
     * 构造空的JSONObject
     */
    public JSONObject() {
        this.node = JSONUtil.createObjectNode();
    }

    /**
     * 从JSON字符串构造JSONObject
     *
     * @param jsonStr JSON字符串
     */
    public JSONObject(String jsonStr) {
        JsonNode parsedNode = JSONUtil.parseNode(jsonStr);
        if (!parsedNode.isObject()) {
            throw new IllegalArgumentException("JSON string is not an object");
        }
        this.node = (ObjectNode) parsedNode;
    }

    /**
     * 从对象构造JSONObject
     *
     * @param obj 对象
     */
    public JSONObject(Object obj) {
        if (obj instanceof JSONObject) {
            this.node = ((JSONObject) obj).node.deepCopy();
        } else if (obj instanceof String) {
            JsonNode parsedNode = JSONUtil.parseNode((String) obj);
            if (!parsedNode.isObject()) {
                throw new IllegalArgumentException("JSON string is not an object");
            }
            this.node = (ObjectNode) parsedNode;
        } else {
            JsonNode jsonNode = JSONUtil.valueToTree(obj);
            if (!jsonNode.isObject()) {
                throw new IllegalArgumentException("Object cannot be converted to JSONObject");
            }
            this.node = (ObjectNode) jsonNode;
        }
    }

    /**
     * 从ObjectNode构造JSONObject（内部使用）
     *
     * @param node ObjectNode
     */
    protected JSONObject(ObjectNode node) {
        this.node = node;
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     * @return this
     */
    public JSONObject set(String key, Object value) {
        if (value == null) {
            node.putNull(key);
        } else if (value instanceof String) {
            node.put(key, (String) value);
        } else if (value instanceof Integer) {
            node.put(key, (Integer) value);
        } else if (value instanceof Long) {
            node.put(key, (Long) value);
        } else if (value instanceof Double) {
            node.put(key, (Double) value);
        } else if (value instanceof Float) {
            node.put(key, (Float) value);
        } else if (value instanceof Boolean) {
            node.put(key, (Boolean) value);
        } else if (value instanceof BigDecimal) {
            node.put(key, (BigDecimal) value);
        } else if (value instanceof BigInteger) {
            node.put(key, (BigInteger) value);
        } else if (value instanceof byte[]) {
            node.put(key, (byte[]) value);
        } else if (value instanceof JSONObject) {
            node.set(key, ((JSONObject) value).node);
        } else if (value instanceof JSONArray) {
            node.set(key, ((JSONArray) value).getNode());
        } else {
            node.set(key, JSONUtil.valueToTree(value));
        }
        return this;
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    public Object get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        JsonNode valueNode = node.get((String) key);
        return convertJsonNodeToObject(valueNode);
    }

    /**
     * 获取字符串值
     *
     * @param key 键
     * @return 字符串值
     */
    public String getStr(String key) {
        JsonNode valueNode = node.get(key);
        return valueNode != null && !valueNode.isNull() ? valueNode.asText() : null;
    }

    /**
     * 获取字符串值（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public String getStr(String key, String defaultValue) {
        String value = getStr(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数值
     *
     * @param key 键
     * @return 整数值
     */
    public Integer getInt(String key) {
        JsonNode valueNode = node.get(key);
        return valueNode != null && !valueNode.isNull() ? valueNode.asInt() : null;
    }

    /**
     * 获取整数值（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 整数值
     */
    public Integer getInt(String key, Integer defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取长整数值
     *
     * @param key 键
     * @return 长整数值
     */
    public Long getLong(String key) {
        JsonNode valueNode = node.get(key);
        return valueNode != null && !valueNode.isNull() ? valueNode.asLong() : null;
    }

    /**
     * 获取长整数值（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 长整数值
     */
    public Long getLong(String key, Long defaultValue) {
        Long value = getLong(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取双精度浮点数值
     *
     * @param key 键
     * @return 双精度浮点数值
     */
    public Double getDouble(String key) {
        JsonNode valueNode = node.get(key);
        return valueNode != null && !valueNode.isNull() ? valueNode.asDouble() : null;
    }

    /**
     * 获取双精度浮点数值（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    public Double getDouble(String key, Double defaultValue) {
        Double value = getDouble(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取布尔值
     *
     * @param key 键
     * @return 布尔值
     */
    public Boolean getBool(String key) {
        JsonNode valueNode = node.get(key);
        return valueNode != null && !valueNode.isNull() ? valueNode.asBoolean() : null;
    }

    /**
     * 获取布尔值（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public Boolean getBool(String key, Boolean defaultValue) {
        Boolean value = getBool(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取BigDecimal值
     *
     * @param key 键
     * @return BigDecimal值
     */
    public BigDecimal getBigDecimal(String key) {
        JsonNode valueNode = node.get(key);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.decimalValue();
    }

    /**
     * 获取BigInteger值
     *
     * @param key 键
     * @return BigInteger值
     */
    public BigInteger getBigInteger(String key) {
        JsonNode valueNode = node.get(key);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.bigIntegerValue();
    }

    /**
     * 获取JSONObject值
     *
     * @param key 键
     * @return JSONObject值
     */
    public JSONObject getJSONObject(String key) {
        JsonNode valueNode = node.get(key);
        if (valueNode == null || valueNode.isNull() || !valueNode.isObject()) {
            return null;
        }
        return new JSONObject((ObjectNode) valueNode);
    }

    /**
     * 获取JSONArray值
     *
     * @param key 键
     * @return JSONArray值
     */
    public JSONArray getJSONArray(String key) {
        JsonNode valueNode = node.get(key);
        if (valueNode == null || valueNode.isNull() || !valueNode.isArray()) {
            return null;
        }
        return new JSONArray(valueNode);
    }

    /**
     * 获取指定类型的Bean
     *
     * @param key   键
     * @param clazz 类型
     * @param <T>   泛型
     * @return Bean对象
     */
    public <T> T getBean(String key, Class<T> clazz) {
        JsonNode valueNode = node.get(key);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return JSONUtil.treeToValue(valueNode, clazz);
    }

    /**
     * 将JSONObject转换为指定类型的Bean
     *
     * @param clazz 类型
     * @param <T>   泛型
     * @return Bean对象
     */
    public <T> T toBean(Class<T> clazz) {
        return JSONUtil.treeToValue(node, clazz);
    }

    /**
     * 转换为JSON字符串
     *
     * @return JSON字符串
     */
    @Override
    public String toString() {
        return JSONUtil.toJsonStr(node);
    }

    /**
     * 转换为格式化的JSON字符串
     *
     * @return 格式化的JSON字符串
     */
    public String toStringPretty() {
        return JSONUtil.toJsonPrettyStr(node);
    }

    /**
     * 获取内部的ObjectNode（内部使用）
     *
     * @return ObjectNode
     */
    protected ObjectNode getNode() {
        return node;
    }

    /**
     * 将JsonNode转换为Java对象
     *
     * @param jsonNode JsonNode
     * @return Java对象
     */
    private Object convertJsonNodeToObject(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isObject()) {
            return new JSONObject((ObjectNode) jsonNode);
        }
        if (jsonNode.isArray()) {
            return new JSONArray(jsonNode);
        }
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        if (jsonNode.isInt()) {
            return jsonNode.asInt();
        }
        if (jsonNode.isLong()) {
            return jsonNode.asLong();
        }
        if (jsonNode.isDouble() || jsonNode.isFloat()) {
            return jsonNode.asDouble();
        }
        if (jsonNode.isBigDecimal()) {
            return jsonNode.decimalValue();
        }
        if (jsonNode.isBigInteger()) {
            return jsonNode.bigIntegerValue();
        }
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        return jsonNode.asText();
    }

    // ========== Map接口实现 ==========

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public boolean isEmpty() {
        return node.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof String && node.has((String) key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (JsonNode jsonNode : node) {
            Object nodeValue = convertJsonNodeToObject(jsonNode);
            if (Objects.equals(nodeValue, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object put(String key, Object value) {
        Object oldValue = get(key);
        set(key, value);
        return oldValue;
    }

    @Override
    public Object remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        Object oldValue = get(key);
        node.remove((String) key);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        if (m != null) {
            m.forEach(this::set);
        }
    }

    @Override
    public void clear() {
        node.removeAll();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    @Override
    public Collection<Object> values() {
        List<Object> values = new ArrayList<>();
        node.forEach(jsonNode -> values.add(convertJsonNodeToObject(jsonNode)));
        return values;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new LinkedHashSet<>();
        node.fields().forEachRemaining(entry ->
                entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(),
                        convertJsonNodeToObject(entry.getValue()))));
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSONObject that = (JSONObject) o;
        return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }
}
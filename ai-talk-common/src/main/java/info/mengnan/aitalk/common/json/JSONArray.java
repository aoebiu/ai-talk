package info.mengnan.aitalk.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import info.mengnan.aitalk.common.util.JSONUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * JSONArray类，基于Jackson实现
 *
 */
public class JSONArray implements List<Object> {

    private final ArrayNode node;

    /**
     * 构造空的JSONArray
     */
    public JSONArray() {
        this.node = JSONUtil.createArrayNode();
    }

    /**
     * 从JSON字符串构造JSONArray
     *
     * @param jsonStr JSON字符串
     */
    public JSONArray(String jsonStr) {
        JsonNode parsedNode = JSONUtil.parseNode(jsonStr);
        if (!parsedNode.isArray()) {
            throw new IllegalArgumentException("JSON string is not an array");
        }
        this.node = (ArrayNode) parsedNode;
    }

    /**
     * 从对象构造JSONArray
     *
     * @param obj 对象
     */
    public JSONArray(Object obj) {
        if (obj instanceof JSONArray) {
            this.node = ((JSONArray) obj).node.deepCopy();
        } else if (obj instanceof String) {
            JsonNode parsedNode = JSONUtil.parseNode((String) obj);
            if (!parsedNode.isArray()) {
                throw new IllegalArgumentException("JSON string is not an array");
            }
            this.node = (ArrayNode) parsedNode;
        } else {
            JsonNode jsonNode = JSONUtil.valueToTree(obj);
            if (!jsonNode.isArray()) {
                throw new IllegalArgumentException("Object cannot be converted to JSONArray");
            }
            this.node = (ArrayNode) jsonNode;
        }
    }

    /**
     * 从JsonNode构造JSONArray（内部使用）
     *
     * @param node JsonNode
     */
    protected JSONArray(JsonNode node) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("JsonNode is not an array");
        }
        this.node = (ArrayNode) node;
    }

    /**
     * 添加元素（链式调用）
     *
     * @param value 值
     * @return this
     */
    public JSONArray set(Object value) {
        addValueToNode(value, -1);
        return this;
    }

    /**
     * 在指定位置设置元素（链式调用）
     *
     * @param index 索引
     * @param value 值
     * @return this
     */
    public JSONArray setElement(int index, Object value) {
        if (index < 0 || index >= node.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + node.size());
        }
        addValueToNode(value, index);
        return this;
    }

    /**
     * 添加元素（链式调用）
     *
     * @param value 值
     * @return this
     */
    public JSONArray addElement(Object value) {
        addValueToNode(value, -1);
        return this;
    }

    /**
     * 添加元素到ArrayNode
     *
     * @param value 值
     * @param index 索引，-1表示添加到末尾
     */
    private void addValueToNode(Object value, int index) {
        JsonNode jsonNode;
        if (value == null) {
            jsonNode = JSONUtil.valueToTree(null);
        } else if (value instanceof String) {
            jsonNode = JSONUtil.valueToTree(value);
        } else if (value instanceof Number) {
            jsonNode = JSONUtil.valueToTree(value);
        } else if (value instanceof Boolean) {
            jsonNode = JSONUtil.valueToTree(value);
        } else if (value instanceof JSONObject) {
            jsonNode = ((JSONObject) value).getNode();
        } else if (value instanceof JSONArray) {
            jsonNode = ((JSONArray) value).node;
        } else {
            jsonNode = JSONUtil.valueToTree(value);
        }

        if (index >= 0) {
            node.set(index, jsonNode);
        } else {
            node.add(jsonNode);
        }
    }

    /**
     * 获取指定索引的元素
     *
     * @param index 索引
     * @return 元素
     */
    @Override
    public Object get(int index) {
        if (index < 0 || index >= node.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + node.size());
        }
        JsonNode valueNode = node.get(index);
        return convertJsonNodeToObject(valueNode);
    }

    /**
     * 获取字符串值
     *
     * @param index 索引
     * @return 字符串值
     */
    public String getStr(int index) {
        JsonNode valueNode = node.get(index);
        return valueNode != null && !valueNode.isNull() ? valueNode.asText() : null;
    }

    /**
     * 获取字符串值（带默认值）
     *
     * @param index        索引
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public String getStr(int index, String defaultValue) {
        String value = getStr(index);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数值
     *
     * @param index 索引
     * @return 整数值
     */
    public Integer getInt(int index) {
        JsonNode valueNode = node.get(index);
        return valueNode != null && !valueNode.isNull() ? valueNode.asInt() : null;
    }

    /**
     * 获取整数值（带默认值）
     *
     * @param index        索引
     * @param defaultValue 默认值
     * @return 整数值
     */
    public Integer getInt(int index, Integer defaultValue) {
        Integer value = getInt(index);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取长整数值
     *
     * @param index 索引
     * @return 长整数值
     */
    public Long getLong(int index) {
        JsonNode valueNode = node.get(index);
        return valueNode != null && !valueNode.isNull() ? valueNode.asLong() : null;
    }

    /**
     * 获取长整数值（带默认值）
     *
     * @param index        索引
     * @param defaultValue 默认值
     * @return 长整数值
     */
    public Long getLong(int index, Long defaultValue) {
        Long value = getLong(index);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取双精度浮点数值
     *
     * @param index 索引
     * @return 双精度浮点数值
     */
    public Double getDouble(int index) {
        JsonNode valueNode = node.get(index);
        return valueNode != null && !valueNode.isNull() ? valueNode.asDouble() : null;
    }

    /**
     * 获取双精度浮点数值（带默认值）
     *
     * @param index        索引
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    public Double getDouble(int index, Double defaultValue) {
        Double value = getDouble(index);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取布尔值
     *
     * @param index 索引
     * @return 布尔值
     */
    public Boolean getBool(int index) {
        JsonNode valueNode = node.get(index);
        return valueNode != null && !valueNode.isNull() ? valueNode.asBoolean() : null;
    }

    /**
     * 获取布尔值（带默认值）
     *
     * @param index        索引
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public Boolean getBool(int index, Boolean defaultValue) {
        Boolean value = getBool(index);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取BigDecimal值
     *
     * @param index 索引
     * @return BigDecimal值
     */
    public BigDecimal getBigDecimal(int index) {
        JsonNode valueNode = node.get(index);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.decimalValue();
    }

    /**
     * 获取BigInteger值
     *
     * @param index 索引
     * @return BigInteger值
     */
    public BigInteger getBigInteger(int index) {
        JsonNode valueNode = node.get(index);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.bigIntegerValue();
    }

    /**
     * 获取JSONObject值
     *
     * @param index 索引
     * @return JSONObject值
     */
    public JSONObject getJSONObject(int index) {
        JsonNode valueNode = node.get(index);
        if (valueNode == null || valueNode.isNull() || !valueNode.isObject()) {
            return null;
        }
        return new JSONObject((ObjectNode) valueNode);
    }

    /**
     * 获取JSONArray值
     *
     * @param index 索引
     * @return JSONArray值
     */
    public JSONArray getJSONArray(int index) {
        JsonNode valueNode = node.get(index);
        if (valueNode == null || valueNode.isNull() || !valueNode.isArray()) {
            return null;
        }
        return new JSONArray(valueNode);
    }

    /**
     * 获取指定类型的Bean
     *
     * @param index 索引
     * @param clazz 类型
     * @param <T>   泛型
     * @return Bean对象
     */
    public <T> T getBean(int index, Class<T> clazz) {
        JsonNode valueNode = node.get(index);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return JSONUtil.treeToValue(valueNode, clazz);
    }

    /**
     * 将JSONArray转换为指定类型的List
     *
     * @param elementClass 元素类型
     * @param <T>          泛型
     * @return List对象
     */
    public <T> List<T> toList(Class<T> elementClass) {
        List<T> list = new ArrayList<>();
        for (JsonNode jsonNode : node) {
            list.add(JSONUtil.treeToValue(jsonNode, elementClass));
        }
        return list;
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
     * 获取内部的ArrayNode（内部使用）
     *
     * @return ArrayNode
     */
    protected ArrayNode getNode() {
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

    // ========== List接口实现 ==========

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public boolean isEmpty() {
        return node.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        for (JsonNode jsonNode : node) {
            Object nodeValue = convertJsonNodeToObject(jsonNode);
            if (Objects.equals(nodeValue, o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private final Iterator<JsonNode> nodeIterator = node.iterator();

            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public Object next() {
                return convertJsonNodeToObject(nodeIterator.next());
            }

            @Override
            public void remove() {
                nodeIterator.remove();
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[node.size()];
        for (int i = 0; i < node.size(); i++) {
            array[i] = get(i);
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < node.size()) {
            return (T[]) Arrays.copyOf(toArray(), node.size(), a.getClass());
        }
        System.arraycopy(toArray(), 0, a, 0, node.size());
        if (a.length > node.size()) {
            a[node.size()] = null;
        }
        return a;
    }

    @Override
    public boolean add(Object o) {
        addValueToNode(o, -1);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        for (int i = 0; i < node.size(); i++) {
            if (Objects.equals(get(i), o)) {
                node.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<?> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        c.forEach(this::add);
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        int i = index;
        for (Object o : c) {
            add(i++, o);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        for (int i = node.size() - 1; i >= 0; i--) {
            if (!c.contains(get(i))) {
                node.remove(i);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        node.removeAll();
    }

    @Override
    public Object set(int index, Object element) {
        Object oldValue = get(index);
        addValueToNode(element, index);
        return oldValue;
    }

    @Override
    public void add(int index, Object element) {
        if (index < 0 || index > node.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + node.size());
        }
        node.insert(index, JSONUtil.valueToTree(element));
    }

    @Override
    public Object remove(int index) {
        if (index < 0 || index >= node.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + node.size());
        }
        Object oldValue = get(index);
        node.remove(index);
        return oldValue;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < node.size(); i++) {
            if (Objects.equals(get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = node.size() - 1; i >= 0; i--) {
            if (Objects.equals(get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<Object> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return new ListIterator<Object>() {
            private int cursor = index;

            @Override
            public boolean hasNext() {
                return cursor < node.size();
            }

            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(cursor++);
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            public Object previous() {
                if (!hasPrevious()) {
                    throw new NoSuchElementException();
                }
                return get(--cursor);
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                JSONArray.this.remove(--cursor);
            }

            @Override
            public void set(Object o) {
                JSONArray.this.set(cursor - 1, o);
            }

            @Override
            public void add(Object o) {
                JSONArray.this.add(cursor++, o);
            }
        };
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > node.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", Size: " + node.size());
        }
        List<Object> subList = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            subList.add(get(i));
        }
        return subList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSONArray that = (JSONArray) o;
        return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }
}
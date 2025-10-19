package info.mengnan.aitalk.rag.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.common.json.JSONObject;
import info.mengnan.aitalk.common.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tools 测试类
 */
@Slf4j
class ToolsTest {

    private Tools tools;
    private final static String JAVASCRIPT_PATH = "info/mengnan/aitalk/rag/tools/js/";

    @BeforeEach
    void setUp() {
        tools = new Tools();
    }

    private String loadScript(String fileName) {
        String path = JAVASCRIPT_PATH + fileName;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Script file not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load script: " + path, e);
        }
    }

    /**
     * 测试模拟天气查询工具 - 使用本地数据
     */
    @Test
    void testWeatherQueryTool() {
        ToolDescription weatherTool = new ToolDescription();
        weatherTool.setName("get_weather");
        weatherTool.setDescription("查询城市天气信息");

        Map<String, String> properties = new HashMap<>();
        properties.put("city", "城市名称");
        properties.put("unit", "温度单位,celsius");
        weatherTool.setProperty(properties);

        weatherTool.setRequired(List.of("city"));

        // 从 js 包下加载脚本
        weatherTool.setExecute(loadScript("weather-query.js"));

        // 创建工具
        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(weatherTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        // 测试1: 查询北京天气
        JSONObject args1 = new JSONObject();
        args1.set("city", "北京");
        args1.set("unit", "温度");

        ToolExecutionRequest request1 = ToolExecutionRequest.builder()
                .name("get_weather")
                .arguments(JSONUtil.toJsonStr(args1))
                .build();

        String result1 = executor.execute(request1, null);
        log.info("北京天气(摄氏度): \n{}", result1);

        assertTrue(result1.contains("北京"));
        assertTrue(result1.contains("15°C"));
        assertTrue(result1.contains("晴"));
        assertTrue(result1.contains("45%"));

        // 测试2: 查询New York天气(华氏度)
        JSONObject args2 = new JSONObject();
        args2.set("city", "New York");
        args2.set("unit", "fahrenheit");

        ToolExecutionRequest request2 = ToolExecutionRequest.builder()
                .name("get_weather")
                .arguments(JSONUtil.toJsonStr(args2))
                .build();

        String result2 = executor.execute(request2, null);
        log.info("New York天气(华氏度): \n{}", result2);

        assertTrue(result2.contains("New York"));
        assertTrue(result2.contains("°F"));
        assertTrue(result2.contains("Sunny"));

        // 测试3: 查询不存在的城市
        JSONObject args3 = new JSONObject();
        args3.set("city", "未知城市");

        ToolExecutionRequest request3 = ToolExecutionRequest.builder()
                .name("get_weather")
                .arguments(JSONUtil.toJsonStr(args3))
                .build();

        String result3 = executor.execute(request3, null);
        log.info("查询不存在的城市: {}", result3);

        assertTrue(result3.contains("抱歉"));
    }

    /**
     * 测试HTTP GET请求 - 获取随机用户信息
     * 使用公开的 JSONPlaceholder API
     */
    @Test
    void testHttpGetRequest() {
        ToolDescription getTool = new ToolDescription();
        getTool.setName("get_user");
        getTool.setDescription("获取用户信息");

        Map<String, String> properties = new HashMap<>();
        properties.put("userId", "用户ID");
        getTool.setProperty(properties);
        getTool.setRequired(List.of("userId"));

        getTool.setExecute(loadScript("http-get.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(getTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("userId", "1");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("get_user")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("HTTP GET 请求结果: {}", result);

        assertNotNull(result);
        // 成功的响应应该包含用户信息
        assertTrue(result.contains("name") || result.contains("HTTP请求失败"));
    }

    /**
     * 测试HTTP POST请求 - 创建新资源
     * 使用公开的 JSONPlaceholder API
     */
    @Test
    void testHttpPostRequest() {
        ToolDescription postTool = new ToolDescription();
        postTool.setName("create_post");
        postTool.setDescription("创建新帖子");

        Map<String, String> properties = new HashMap<>();
        properties.put("title", "标题");
        properties.put("body", "内容");
        properties.put("userId", "用户ID");
        postTool.setProperty(properties);
        postTool.setRequired(List.of("title", "body", "userId"));

        postTool.setExecute(loadScript("http-post.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(postTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("title", "测试标题");
        args.set("body", "这是测试内容");
        args.set("userId", "1");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("create_post")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("HTTP POST 请求结果: {}", result);

        assertNotNull(result);
        assertTrue(result.contains("测试标题") || result.contains("HTTP POST请求失败"));
    }

    /**
     * 测试HTTP PUT请求 - 更新资源
     */
    @Test
    void testHttpPutRequest() {
        ToolDescription putTool = new ToolDescription();
        putTool.setName("update_post");
        putTool.setDescription("更新帖子");

        Map<String, String> properties = new HashMap<>();
        properties.put("postId", "帖子ID");
        properties.put("title", "新标题");
        properties.put("body", "新内容");
        putTool.setProperty(properties);
        putTool.setRequired(List.of("postId", "title", "body"));

        putTool.setExecute(loadScript("http-put.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(putTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("postId", "1");
        args.set("title", "更新的标题");
        args.set("body", "更新的内容");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("update_post")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("HTTP PUT 请求结果: {}", result);

        assertNotNull(result);
    }

    /**
     * 测试HTTP DELETE请求 - 删除资源
     */
    @Test
    void testHttpDeleteRequest() {
        ToolDescription deleteTool = new ToolDescription();
        deleteTool.setName("delete_post");
        deleteTool.setDescription("删除帖子");

        Map<String, String> properties = new HashMap<>();
        properties.put("postId", "帖子ID");
        deleteTool.setProperty(properties);
        deleteTool.setRequired(List.of("postId"));

        deleteTool.setExecute(loadScript("http-delete.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(deleteTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("postId", "1");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("delete_post")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("HTTP DELETE 请求结果: {}", result);

        assertNotNull(result);
    }

    /**
     * 测试带请求头的HTTP请求
     */
    @Test
    void testHttpRequestWithHeaders() {
        ToolDescription headerTool = new ToolDescription();
        headerTool.setName("get_with_headers");
        headerTool.setDescription("发送带自定义请求头的GET请求");

        Map<String, String> properties = new HashMap<>();
        properties.put("url", "请求URL");
        properties.put("authToken", "认证令牌");
        headerTool.setProperty(properties);
        headerTool.setRequired(List.of("url"));

        headerTool.setExecute(loadScript("http-with-headers.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(headerTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("url", "https://jsonplaceholder.typicode.com/posts/1");
        args.set("authToken", "Bearer test-token");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("get_with_headers")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("带请求头的HTTP请求结果: {}", result);

        assertNotNull(result);
    }

    /**
     * 测试复杂的业务逻辑 - 获取并处理数据
     */
    @Test
    void testComplexBusinessLogic() {
        ToolDescription complexTool = new ToolDescription();
        complexTool.setName("get_user_posts");
        complexTool.setDescription("获取用户的帖子列表并统计");

        Map<String, String> properties = new HashMap<>();
        properties.put("userId", "用户ID");
        complexTool.setProperty(properties);
        complexTool.setRequired(List.of("userId"));

        complexTool.setExecute(loadScript("complex-business-logic.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(complexTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("userId", "3");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("get_user_posts")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("复杂业务逻辑执行结果: {}", result);

        assertNotNull(result);
        assertTrue(result.contains("postCount") || result.contains("error"));
    }

    /**
     * 测试错误处理 - 无效的URL
     */
    @Test
    void testHttpRequestWithInvalidUrl() {
        ToolDescription errorTool = new ToolDescription();
        errorTool.setName("test_invalid_url");
        errorTool.setDescription("测试无效URL的错误处理");

        Map<String, String> properties = new HashMap<>();
        properties.put("dummy", "占位参数");
        errorTool.setProperty(properties);

        errorTool.setExecute(loadScript("invalid-url-test.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(errorTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("dummy", "test");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("test_invalid_url")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("Invalid URL error handling result: {}", result);

        assertNotNull(result);
        assertTrue(result.contains("502"));
    }
}
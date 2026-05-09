package info.mengnan.aitalk.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.common.json.JSONObject;
import info.mengnan.aitalk.common.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
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

    private final static String JAVASCRIPT_PATH = "info/mengnan/aitalk/tool/js/";

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
     * 测试 Node.js 代码支持 - 使用 require 语法
     * 验证是否支持 Node.js 风格的模块导入，例如 const _ = require("lodash")
     */
//    @Test
//    void testNodeJsRequireSyntax() {
//        ToolDescription nodeTool = new ToolDescription();
//        nodeTool.setName("nodejs_require_test");
//        nodeTool.setDescription("测试 Node.js require 语法支持");
//
//        Map<String, String> properties = new HashMap<>();
//        properties.put("dummy", "占位参数");
//        nodeTool.setProperty(properties);
//
//        nodeTool.setExecute(loadScript("nodejs-require-test.js"));
//
//        // 获取项目根目录（包含 node_modules）
//        String projectRoot = System.getProperty("user.dir");
//        log.info("Node.js require test, project root: {}", projectRoot);
//
//        // 使用支持 CommonJS require 的 Tools 构造函数，传入 node_modules 路径
//        Tools toolsWithRequire = new Tools(projectRoot);
//        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithRequire.createDynamicTools(List.of(nodeTool));
//        ToolExecutor executor = toolsMap.values().iterator().next();
//
//        JSONObject args = new JSONObject();
//        args.set("dummy", "test");
//
//        ToolExecutionRequest request = ToolExecutionRequest.builder()
//                .name("nodejs_require_test")
//                .arguments(JSONUtil.toJsonStr(args))
//                .build();
//
//        String result = executor.execute(request, null);
//        log.info("Node.js require 语法测试结果：{}", result);
//
//        assertNotNull(result);
//        // 如果支持 Node.js require 语法，应该成功执行并返回 lodash 相关信息
//        assertTrue(result.contains("测试成功"), "期望包含'测试成功'，实际结果：" + result);
//    }

    // ================================================
    // 以下测试方法已暂时注释，只保留 testNodeJsRequireSyntax
    // ================================================

    /*
    @BeforeEach
    void setUp() {
        tools = new Tools();
        toolsWithHttp = new Tools(new HttpClients(), (key) -> null);
    }

    private Tools tools;
    private Tools toolsWithHttp;

    @Test
    void testWeatherQueryTool() {
        ToolDescription weatherTool = new ToolDescription();
        weatherTool.setName("get_weather");
        weatherTool.setDescription("查询城市天气信息");

        Map<String, String> properties = new HashMap<>();
        properties.put("city", "城市名称");
        properties.put("unit", "温度单位，celsius");
        weatherTool.setProperty(properties);

        weatherTool.setRequired(List.of("city"));

        weatherTool.setExecute(loadScript("weather-query.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = tools.createDynamicTools(List.of(weatherTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args1 = new JSONObject();
        args1.set("city", "北京");
        args1.set("unit", "温度");

        ToolExecutionRequest request1 = ToolExecutionRequest.builder()
                .name("get_weather")
                .arguments(JSONUtil.toJsonStr(args1))
                .build();

        String result1 = executor.execute(request1, null);
        log.info("北京天气 (摄氏度): \n{}", result1);

        assertTrue(result1.contains("北京"));
        assertTrue(result1.contains("15°C"));

        JSONObject args2 = new JSONObject();
        args2.set("city", "New York");
        args2.set("unit", "fahrenheit");

        ToolExecutionRequest request2 = ToolExecutionRequest.builder()
                .name("get_weather")
                .arguments(JSONUtil.toJsonStr(args2))
                .build();

        String result2 = executor.execute(request2, null);
        log.info("New York 天气 (华氏度): \n{}", result2);

        assertTrue(result2.contains("New York"));
        assertTrue(result2.contains("°F"));
    }

    @Test
    void testHttpGetRequest() {
        ToolDescription getTool = new ToolDescription();
        getTool.setName("get_user");
        getTool.setDescription("获取用户信息");

        Map<String, String> properties = new HashMap<>();
        properties.put("userId", "用户 ID");
        getTool.setProperty(properties);
        getTool.setRequired(List.of("userId"));

        getTool.setExecute(loadScript("http-get.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(getTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("userId", "1");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("get_user")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("HTTP GET 请求结果：{}", result);

        assertNotNull(result);
    }

    @Test
    void testHttpPostRequest() {
        ToolDescription postTool = new ToolDescription();
        postTool.setName("create_post");
        postTool.setDescription("创建新帖子");

        Map<String, String> properties = new HashMap<>();
        properties.put("title", "标题");
        properties.put("body", "内容");
        properties.put("userId", "用户 ID");
        postTool.setProperty(properties);
        postTool.setRequired(List.of("title", "body", "userId"));

        postTool.setExecute(loadScript("http-post.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(postTool));
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
        log.info("HTTP POST 请求结果：{}", result);

        assertNotNull(result);
    }

    @Test
    void testHttpPutRequest() {
        ToolDescription putTool = new ToolDescription();
        putTool.setName("update_post");
        putTool.setDescription("更新帖子");

        Map<String, String> properties = new HashMap<>();
        properties.put("postId", "帖子 ID");
        properties.put("title", "新标题");
        properties.put("body", "新内容");
        putTool.setProperty(properties);
        putTool.setRequired(List.of("postId", "title", "body"));

        putTool.setExecute(loadScript("http-put.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(putTool));
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
        log.info("HTTP PUT 请求结果：{}", result);

        assertNotNull(result);
    }

    @Test
    void testHttpDeleteRequest() {
        ToolDescription deleteTool = new ToolDescription();
        deleteTool.setName("delete_post");
        deleteTool.setDescription("删除帖子");

        Map<String, String> properties = new HashMap<>();
        properties.put("postId", "帖子 ID");
        deleteTool.setProperty(properties);
        deleteTool.setRequired(List.of("postId"));

        deleteTool.setExecute(loadScript("http-delete.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(deleteTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("postId", "1");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("delete_post")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("HTTP DELETE 请求结果：{}", result);

        assertNotNull(result);
    }

    @Test
    void testHttpRequestWithHeaders() {
        ToolDescription headerTool = new ToolDescription();
        headerTool.setName("get_with_headers");
        headerTool.setDescription("发送带自定义请求头的 GET 请求");

        Map<String, String> properties = new HashMap<>();
        properties.put("url", "请求 URL");
        properties.put("authToken", "认证令牌");
        headerTool.setProperty(properties);
        headerTool.setRequired(List.of("url"));

        headerTool.setExecute(loadScript("http-with-headers.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(headerTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("url", "https://jsonplaceholder.typicode.com/posts/1");
        args.set("authToken", "Bearer test-token");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("get_with_headers")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("带请求头的 HTTP 请求结果：{}", result);

        assertNotNull(result);
    }

    @Test
    void testComplexBusinessLogic() {
        ToolDescription complexTool = new ToolDescription();
        complexTool.setName("get_user_posts");
        complexTool.setDescription("获取用户的帖子列表并统计");

        Map<String, String> properties = new HashMap<>();
        properties.put("userId", "用户 ID");
        complexTool.setProperty(properties);
        complexTool.setRequired(List.of("userId"));

        complexTool.setExecute(loadScript("complex-business-logic.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(complexTool));
        ToolExecutor executor = toolsMap.values().iterator().next();

        JSONObject args = new JSONObject();
        args.set("userId", "3");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("get_user_posts")
                .arguments(JSONUtil.toJsonStr(args))
                .build();

        String result = executor.execute(request, null);
        log.info("复杂业务逻辑执行结果：{}", result);

        assertNotNull(result);
    }

    @Test
    void testHttpRequestWithInvalidUrl() {
        ToolDescription errorTool = new ToolDescription();
        errorTool.setName("test_invalid_url");
        errorTool.setDescription("测试无效 URL 的错误处理");

        Map<String, String> properties = new HashMap<>();
        properties.put("dummy", "占位参数");
        errorTool.setProperty(properties);

        errorTool.setExecute(loadScript("invalid-url-test.js"));

        Map<ToolSpecification, ToolExecutor> toolsMap = toolsWithHttp.createDynamicTools(List.of(errorTool));
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
    */
}
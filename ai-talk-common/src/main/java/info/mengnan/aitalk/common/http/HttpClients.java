package info.mengnan.aitalk.common.http;

import info.mengnan.aitalk.common.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Java HTTP 客户端
 * 注意此类被 {@link info.mengnan.aitalk.rag.tools.Tools} 构造方法使用到,所以只做增强,不作修改!
 * 如果有大的改动新增类HttpClientsV2
 */
@Slf4j
public final class HttpClients {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, String> DEFAULT_HEADERS = Map.of(
            "Content-Type", "application/json; charset=UTF-8",
            "Accept", "application/json"
    );

    /**
     * 发起 GET 请求
     *
     * @param url 请求 URL
     * @return 响应内容(JSON 字符串)
     */
    public String get(String url) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET();

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return buildResponse(response);
        } catch (Exception e) {
            log.error("HTTP GET request failed: {}", url, e);
            return buildErrorResponse(e);
        }
    }

    /**
     * 发起 POST 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return 响应内容(JSON 字符串)
     */
    public String post(String url, String body) {
        return post(url, body, DEFAULT_HEADERS);
    }

    /**
     * 发起 POST 请求,支持自定义请求头
     *
     * @param url     请求 URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容(JSON 字符串, 包含 status, headers, body)
     */
    public String post(String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));

            // 添加自定义请求头
            headers.forEach(builder::header);

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return buildResponse(response);
        } catch (Exception e) {
            log.error("HTTP POST request failed: {}", url, e);
            return buildErrorResponse(e);
        }
    }

    /**
     * 发起 PUT 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return 响应内容(JSON 字符串)
     */
    public String put(String url, String body) {
        return put(url, body, DEFAULT_HEADERS);
    }

    /**
     * 发起 PUT 请求,支持自定义请求头
     *
     * @param url     请求 URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容(JSON 字符串, 包含 status, headers, body)
     */
    public String put(String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));

            // 添加自定义请求头
            headers.forEach(builder::header);

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return buildResponse(response);
        } catch (Exception e) {
            log.error("HTTP PUT request failed: {}", url, e);
            return buildErrorResponse(e);
        }
    }

    /**
     * 发起 DELETE 请求
     *
     * @param url 请求 URL
     * @return 响应内容(JSON 字符串)
     */
    public String delete(String url) {
        return delete(url, DEFAULT_HEADERS);
    }

    /**
     * 发起 DELETE 请求,支持自定义请求头
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应内容(JSON 字符串, 包含 status, headers, body)
     */
    public String delete(String url, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .DELETE();

            // 添加自定义请求头
            headers.forEach(builder::header);

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return buildResponse(response);
        } catch (Exception e) {
            log.error("HTTP DELETE request failed: {}", url, e);
            return buildErrorResponse(e);
        }
    }

    /**
     * 构建响应 JSON
     */
    private String buildResponse(HttpResponse<String> response) {
        JSONObject result = new JSONObject();
        result.set("status", response.statusCode());
        result.set("body", response.body());

        // 添加响应头
        JSONObject headers = new JSONObject();
        response.headers().map().forEach((key, values) -> {
            if (values.size() == 1) {
                headers.set(key, values.get(0));
            } else {
                headers.set(key, values);
            }
        });
        result.set("headers", headers);

        return result.toString();
    }

    /**
     * 构建错误响应 JSON
     */
    private String buildErrorResponse(Exception e) {
        JSONObject result = new JSONObject();
        result.set("error", true);
        result.set("message", e.getMessage());
        result.set("type", e.getClass().getSimpleName());
        return result.toString();
    }
}

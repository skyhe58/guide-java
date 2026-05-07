package com.example.network.http;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * HTTP 客户端示例 — Java 11+ HttpClient
 *
 * 演示内容：
 * 1. 使用 Java HttpClient 发送 GET/POST 请求
 * 2. 设置请求头、超时、HTTP 版本
 * 3. 同步与异步请求
 * 4. DNS 解析示例
 *
 * 知识点：
 * - Java 11 引入了全新的 HttpClient API，替代旧的 HttpURLConnection
 * - 支持 HTTP/1.1 和 HTTP/2
 * - 支持同步（send）和异步（sendAsync）请求
 *
 * @see <a href="docs/2-framework/2.1-network/02-http.md">HTTP 协议详解</a>
 */
public class HttpDemo {

    /**
     * 创建一个配置好的 HttpClient 实例
     * <p>
     * HttpClient 是线程安全的，建议复用同一个实例
     */
    public static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                // 优先使用 HTTP/2，服务端不支持时自动降级到 HTTP/1.1
                .version(HttpClient.Version.HTTP_2)
                // 连接超时 10 秒
                .connectTimeout(Duration.ofSeconds(10))
                // 自动跟随重定向
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 发送 GET 请求
     *
     * @param url 请求 URL
     * @return 响应体字符串
     */
    public static HttpResponse<String> sendGetRequest(String url) throws IOException, InterruptedException {
        HttpClient client = createHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Demo")
                .timeout(Duration.ofSeconds(30))
                .GET() // GET 是默认方法，可以省略
                .build();

        // 同步发送请求，阻塞直到收到响应
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 发送 POST 请求（JSON 请求体）
     *
     * @param url  请求 URL
     * @param json JSON 请求体
     * @return 响应
     */
    public static HttpResponse<String> sendPostRequest(String url, String json)
            throws IOException, InterruptedException {
        HttpClient client = createHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 构建带查询参数的 URL
     *
     * @param baseUrl 基础 URL
     * @param params  查询参数
     * @return 完整 URL
     */
    public static String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?");
        params.forEach((key, value) -> {
            sb.append(URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
            sb.append("&");
        });
        // 移除末尾的 &
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * DNS 解析示例 — 将域名解析为 IP 地址
     *
     * @param hostname 域名
     * @return IP 地址列表
     */
    public static List<String> resolveDns(String hostname) throws UnknownHostException {
        // InetAddress.getAllByName 会触发 DNS 解析
        // 解析顺序：JVM 缓存 → OS 缓存 → hosts 文件 → DNS 服务器
        InetAddress[] addresses = InetAddress.getAllByName(hostname);
        List<String> result = new ArrayList<>();
        for (InetAddress addr : addresses) {
            result.add(addr.getHostAddress());
        }
        return result;
    }

    /**
     * 打印 HTTP 响应的详细信息
     */
    public static void printResponse(HttpResponse<String> response) {
        System.out.println("--- HTTP 响应 ---");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("HTTP 版本: " + response.version());
        System.out.println("响应头:");
        response.headers().map().forEach((key, values) ->
                System.out.println("  " + key + ": " + String.join(", ", values)));

        String body = response.body();
        if (body.length() > 500) {
            body = body.substring(0, 500) + "... (截断)";
        }
        System.out.println("响应体: " + body);
        System.out.println("-----------------\n");
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("=== HTTP 客户端示例 ===\n");

        // 1. DNS 解析示例
        System.out.println("--- 1. DNS 解析 ---");
        try {
            List<String> ips = resolveDns("www.baidu.com");
            System.out.println("www.baidu.com 解析结果: " + ips);
        } catch (UnknownHostException e) {
            System.out.println("DNS 解析失败（可能无网络连接）: " + e.getMessage());
        }

        // 2. 构建带参数的 URL
        System.out.println("\n--- 2. URL 构建 ---");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "张三");
        params.put("age", "25");
        String url = buildUrlWithParams("https://httpbin.org/get", params);
        System.out.println("构建的 URL: " + url);

        // 3. 发送 GET 请求（需要网络连接）
        System.out.println("\n--- 3. GET 请求 ---");
        System.out.println("提示: 以下请求需要网络连接，如果无法访问会抛出异常");
        try {
            HttpResponse<String> getResponse = sendGetRequest("https://httpbin.org/get");
            printResponse(getResponse);
        } catch (Exception e) {
            System.out.println("GET 请求失败（可能无网络连接）: " + e.getMessage());
        }

        // 4. 发送 POST 请求
        System.out.println("--- 4. POST 请求 ---");
        try {
            String json = """
                    {
                        "name": "张三",
                        "age": 25,
                        "skills": ["Java", "Spring Boot"]
                    }
                    """;
            HttpResponse<String> postResponse = sendPostRequest("https://httpbin.org/post", json);
            printResponse(postResponse);
        } catch (Exception e) {
            System.out.println("POST 请求失败（可能无网络连接）: " + e.getMessage());
        }

        System.out.println("=== 演示结束 ===");
    }
}

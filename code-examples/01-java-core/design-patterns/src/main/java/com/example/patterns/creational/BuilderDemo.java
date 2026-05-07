package com.example.patterns.creational;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 建造者模式演示 — 链式调用构建复杂对象
 * <p>
 * 业务场景：构建 HTTP 请求对象
 * - 必填参数通过构造方法传入
 * - 可选参数通过链式调用设置
 * - build() 方法创建不可变对象
 * </p>
 */
public class BuilderDemo {

    public static void main(String[] args) {
        System.out.println("========== 建造者模式演示 ==========\n");

        // 链式调用构建 HTTP 请求
        HttpRequest request = HttpRequest.builder("https://api.example.com/users", "GET")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer token123")
                .timeout(5000)
                .retryCount(3)
                .build();

        System.out.println("构建的 HTTP 请求：");
        System.out.println("  URL: " + request.getUrl());
        System.out.println("  Method: " + request.getMethod());
        System.out.println("  Headers: " + request.getHeaders());
        System.out.println("  Timeout: " + request.getTimeout() + "ms");
        System.out.println("  Retry: " + request.getRetryCount() + " 次");

        System.out.println("\n--- 建造者模式的优势 ---");
        System.out.println("1. 链式调用，代码可读性好");
        System.out.println("2. 必填参数在 builder() 中强制传入");
        System.out.println("3. 可选参数按需设置");
        System.out.println("4. build() 创建不可变对象，线程安全");
    }

    /**
     * HTTP 请求对象（不可变）
     */
    static class HttpRequest {
        private final String url;
        private final String method;
        private final List<String> headers;
        private final int timeout;
        private final int retryCount;

        private HttpRequest(Builder builder) {
            this.url = builder.url;
            this.method = builder.method;
            this.headers = Collections.unmodifiableList(new ArrayList<>(builder.headers));
            this.timeout = builder.timeout;
            this.retryCount = builder.retryCount;
        }

        public static Builder builder(String url, String method) {
            return new Builder(url, method);
        }

        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public List<String> getHeaders() { return headers; }
        public int getTimeout() { return timeout; }
        public int getRetryCount() { return retryCount; }

        /**
         * 建造者（静态内部类）
         */
        static class Builder {
            // 必填参数
            private final String url;
            private final String method;
            // 可选参数（带默认值）
            private final List<String> headers = new ArrayList<>();
            private int timeout = 3000;
            private int retryCount = 0;

            Builder(String url, String method) {
                this.url = url;
                this.method = method;
            }

            public Builder header(String name, String value) {
                this.headers.add(name + ": " + value);
                return this; // 返回 this 实现链式调用
            }

            public Builder timeout(int timeout) {
                this.timeout = timeout;
                return this;
            }

            public Builder retryCount(int retryCount) {
                this.retryCount = retryCount;
                return this;
            }

            public HttpRequest build() {
                // 可以在这里做参数校验
                if (url == null || url.isBlank()) {
                    throw new IllegalStateException("URL 不能为空");
                }
                return new HttpRequest(this);
            }
        }
    }
}

package com.example.springcloud.gateway;

/**
 * API 网关演示 — 用 Map + 责任链模拟路由匹配和过滤器链
 *
 * <p>本示例用纯 Java 模拟 Spring Cloud Gateway 的核心机制：
 * <ul>
 *   <li>路由匹配（Route Predicate）：Path / Header / Method 断言</li>
 *   <li>过滤器链（Filter Chain）：限流 / 鉴权 / 日志 / 重写路径</li>
 *   <li>全局过滤器 vs 路由过滤器</li>
 *   <li>负载均衡（lb:// 协议）</li>
 * </ul>
 *
 * <h3>Spring Cloud Gateway 架构：</h3>
 * <pre>
 *  Client → Gateway → [Route Predicate] → [Filter Chain] → Downstream Service
 *
 *  请求处理流程：
 *  1. 客户端发送请求到 Gateway
 *  2. Route Predicate 匹配路由规则
 *  3. Pre Filter 链：鉴权 → 限流 → 日志 → 路径重写
 *  4. 转发到下游服务
 *  5. Post Filter 链：响应头添加 → 日志记录
 * </pre>
 */
public class GatewayDemo {

    // ==================== 请求/响应模型 ====================

    /** 模拟 HTTP 请求 */
    static class HttpRequest {
        final String method;
        final String path;
        final java.util.Map<String, String> headers;
        final java.util.Map<String, String> queryParams;
        String rewrittenPath; // 重写后的路径

        HttpRequest(String method, String path, java.util.Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers != null ? headers : new java.util.LinkedHashMap<>();
            this.queryParams = new java.util.LinkedHashMap<>();
            this.rewrittenPath = path;
        }
    }

    /** 模拟 HTTP 响应 */
    static class HttpResponse {
        int statusCode = 200;
        String body = "";
        final java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    // ==================== 路由断言 ====================

    /** 路由断言接口（Route Predicate） */
    interface RoutePredicate {
        boolean test(HttpRequest request);
        String description();
    }

    /** Path 断言：匹配请求路径 */
    static class PathPredicate implements RoutePredicate {
        private final String pattern;

        PathPredicate(String pattern) { this.pattern = pattern; }

        @Override
        public boolean test(HttpRequest request) {
            // 简单的前缀匹配（实际 Gateway 支持 Ant 风格通配符）
            String cleanPattern = pattern.replace("/**", "");
            return request.path.startsWith(cleanPattern);
        }

        @Override
        public String description() { return "Path=" + pattern; }
    }

    /** Method 断言：匹配 HTTP 方法 */
    static class MethodPredicate implements RoutePredicate {
        private final String method;

        MethodPredicate(String method) { this.method = method; }

        @Override
        public boolean test(HttpRequest request) { return method.equalsIgnoreCase(request.method); }

        @Override
        public String description() { return "Method=" + method; }
    }

    /** Header 断言：匹配请求头 */
    static class HeaderPredicate implements RoutePredicate {
        private final String headerName;
        private final String expectedValue;

        HeaderPredicate(String headerName, String expectedValue) {
            this.headerName = headerName;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean test(HttpRequest request) {
            String value = request.headers.get(headerName);
            return expectedValue.equals(value);
        }

        @Override
        public String description() { return "Header[" + headerName + "=" + expectedValue + "]"; }
    }

    // ==================== 过滤器 ====================

    /** 过滤器接口（GatewayFilter） */
    interface GatewayFilter {
        /** Pre 过滤：请求转发前执行，返回 false 表示拦截请求 */
        boolean preFilter(HttpRequest request, HttpResponse response);
        /** Post 过滤：响应返回后执行 */
        void postFilter(HttpRequest request, HttpResponse response);
        String name();
        int order(); // 执行顺序，数字越小越先执行
    }

    /** 鉴权过滤器 */
    static class AuthFilter implements GatewayFilter {
        @Override
        public boolean preFilter(HttpRequest request, HttpResponse response) {
            String token = request.headers.get("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                response.statusCode = 401;
                response.body = "{\"error\":\"未授权，缺少有效 Token\"}";
                System.out.println("      [AuthFilter] ✗ 拦截：缺少 Authorization 头");
                return false;
            }
            System.out.printf("      [AuthFilter] ✓ Token 验证通过: %s%n", token.substring(0, 20) + "...");
            return true;
        }

        @Override
        public void postFilter(HttpRequest request, HttpResponse response) {}

        @Override
        public String name() { return "AuthFilter"; }

        @Override
        public int order() { return 1; }
    }

    /** 限流过滤器（令牌桶算法） */
    static class RateLimitFilter implements GatewayFilter {
        private final int maxRequests;
        private final java.util.Map<String, Integer> requestCounts = new java.util.concurrent.ConcurrentHashMap<>();

        RateLimitFilter(int maxRequests) { this.maxRequests = maxRequests; }

        @Override
        public boolean preFilter(HttpRequest request, HttpResponse response) {
            String clientIp = request.headers.getOrDefault("X-Real-IP", "unknown");
            int count = requestCounts.merge(clientIp, 1, Integer::sum);
            if (count > maxRequests) {
                response.statusCode = 429;
                response.body = "{\"error\":\"请求过于频繁，请稍后重试\"}";
                System.out.printf("      [RateLimitFilter] ✗ 限流：%s 已请求 %d 次（上限 %d）%n",
                        clientIp, count, maxRequests);
                return false;
            }
            System.out.printf("      [RateLimitFilter] ✓ 通过（%s: %d/%d）%n", clientIp, count, maxRequests);
            return true;
        }

        @Override
        public void postFilter(HttpRequest request, HttpResponse response) {}

        @Override
        public String name() { return "RateLimitFilter"; }

        @Override
        public int order() { return 0; }
    }

    /** 日志过滤器 */
    static class LogFilter implements GatewayFilter {
        private long startTime;

        @Override
        public boolean preFilter(HttpRequest request, HttpResponse response) {
            startTime = System.nanoTime();
            System.out.printf("      [LogFilter] → %s %s%n", request.method, request.path);
            return true;
        }

        @Override
        public void postFilter(HttpRequest request, HttpResponse response) {
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            System.out.printf("      [LogFilter] ← %d %s (%dms)%n", response.statusCode, request.path, elapsed);
        }

        @Override
        public String name() { return "LogFilter"; }

        @Override
        public int order() { return -1; }
    }

    /** 路径重写过滤器 */
    static class RewritePathFilter implements GatewayFilter {
        private final String stripPrefix;
        private final String addPrefix;

        RewritePathFilter(String stripPrefix, String addPrefix) {
            this.stripPrefix = stripPrefix;
            this.addPrefix = addPrefix;
        }

        @Override
        public boolean preFilter(HttpRequest request, HttpResponse response) {
            if (request.path.startsWith(stripPrefix)) {
                request.rewrittenPath = addPrefix + request.path.substring(stripPrefix.length());
                System.out.printf("      [RewritePathFilter] %s → %s%n", request.path, request.rewrittenPath);
            }
            return true;
        }

        @Override
        public void postFilter(HttpRequest request, HttpResponse response) {}

        @Override
        public String name() { return "RewritePathFilter"; }

        @Override
        public int order() { return 2; }
    }

    // ==================== 路由 ====================

    /** 路由定义 */
    static class Route {
        final String id;
        final String uri;  // 下游服务地址
        final java.util.List<RoutePredicate> predicates;
        final java.util.List<GatewayFilter> filters;

        Route(String id, String uri, java.util.List<RoutePredicate> predicates,
              java.util.List<GatewayFilter> filters) {
            this.id = id;
            this.uri = uri;
            this.predicates = predicates;
            this.filters = filters;
        }

        boolean matches(HttpRequest request) {
            return predicates.stream().allMatch(p -> p.test(request));
        }
    }

    // ==================== 网关核心 ====================

    /** 模拟 Spring Cloud Gateway 核心 */
    static class SimulatedGateway {
        private final java.util.List<Route> routes = new java.util.ArrayList<>();
        private final java.util.List<GatewayFilter> globalFilters = new java.util.ArrayList<>();

        void addRoute(Route route) { routes.add(route); }
        void addGlobalFilter(GatewayFilter filter) { globalFilters.add(filter); }

        /** 处理请求 */
        HttpResponse handle(HttpRequest request) {
            System.out.printf("\n    请求: %s %s%n", request.method, request.path);

            // 1. 路由匹配
            Route matchedRoute = null;
            for (Route route : routes) {
                if (route.matches(request)) {
                    matchedRoute = route;
                    break;
                }
            }

            if (matchedRoute == null) {
                System.out.println("    ✗ 无匹配路由 → 404");
                return new HttpResponse(404, "{\"error\":\"Not Found\"}");
            }
            System.out.printf("    ✓ 匹配路由: %s → %s%n", matchedRoute.id, matchedRoute.uri);

            // 2. 合并全局过滤器和路由过滤器，按 order 排序
            java.util.List<GatewayFilter> allFilters = new java.util.ArrayList<>();
            allFilters.addAll(globalFilters);
            allFilters.addAll(matchedRoute.filters);
            allFilters.sort(java.util.Comparator.comparingInt(GatewayFilter::order));

            HttpResponse response = new HttpResponse(200, "");

            // 3. Pre Filter 链
            for (GatewayFilter filter : allFilters) {
                if (!filter.preFilter(request, response)) {
                    // 过滤器拦截，直接返回
                    return response;
                }
            }

            // 4. 转发到下游服务（模拟）
            response.body = String.format("{\"service\":\"%s\",\"path\":\"%s\",\"message\":\"OK\"}",
                    matchedRoute.uri, request.rewrittenPath);
            response.headers.put("X-Gateway-Route", matchedRoute.id);

            // 5. Post Filter 链（逆序执行）
            for (int i = allFilters.size() - 1; i >= 0; i--) {
                allFilters.get(i).postFilter(request, response);
            }

            return response;
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：路由匹配 + 过滤器链 */
    static void demoGateway() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：路由匹配 + 过滤器链");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedGateway gateway = new SimulatedGateway();

        // 全局过滤器
        gateway.addGlobalFilter(new LogFilter());

        // 路由1：用户服务
        gateway.addRoute(new Route("user-service", "lb://user-service",
                java.util.Arrays.asList(new PathPredicate("/api/users/**")),
                java.util.Arrays.asList(
                        new AuthFilter(),
                        new RewritePathFilter("/api/users", "/users"))));

        // 路由2：订单服务（带限流）
        gateway.addRoute(new Route("order-service", "lb://order-service",
                java.util.Arrays.asList(new PathPredicate("/api/orders/**")),
                java.util.Arrays.asList(
                        new AuthFilter(),
                        new RateLimitFilter(3),
                        new RewritePathFilter("/api/orders", "/orders"))));

        // 测试请求
        System.out.println("\n  【请求1】带 Token 访问用户服务：");
        java.util.Map<String, String> headers1 = new java.util.LinkedHashMap<>();
        headers1.put("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.test");
        headers1.put("X-Real-IP", "192.168.1.100");
        HttpResponse r1 = gateway.handle(new HttpRequest("GET", "/api/users/1", headers1));
        System.out.printf("    响应: %d %s%n", r1.statusCode, r1.body);

        System.out.println("\n  【请求2】无 Token 访问（被鉴权拦截）：");
        HttpResponse r2 = gateway.handle(new HttpRequest("GET", "/api/users/1", null));
        System.out.printf("    响应: %d %s%n", r2.statusCode, r2.body);

        System.out.println("\n  【请求3】不存在的路径（404）：");
        HttpResponse r3 = gateway.handle(new HttpRequest("GET", "/api/unknown", headers1));
        System.out.printf("    响应: %d %s%n", r3.statusCode, r3.body);

        // 测试限流
        System.out.println("\n  【请求4-7】连续访问订单服务（限流 3 次）：");
        for (int i = 1; i <= 4; i++) {
            HttpResponse r = gateway.handle(new HttpRequest("GET", "/api/orders/" + i, headers1));
            System.out.printf("    第 %d 次: %d%n", i, r.statusCode);
        }
        System.out.println();
    }

    /** 演示2：Spring Cloud Gateway 配置示例 */
    static void demoConfiguration() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Spring Cloud Gateway 配置示例");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  application.yml 配置：");
        System.out.println("  spring:");
        System.out.println("    cloud:");
        System.out.println("      gateway:");
        System.out.println("        routes:");
        System.out.println("          - id: user-service");
        System.out.println("            uri: lb://user-service");
        System.out.println("            predicates:");
        System.out.println("              - Path=/api/users/**");
        System.out.println("            filters:");
        System.out.println("              - StripPrefix=2");
        System.out.println("              - AddRequestHeader=X-Request-Source, gateway");
        System.out.println("          - id: order-service");
        System.out.println("            uri: lb://order-service");
        System.out.println("            predicates:");
        System.out.println("              - Path=/api/orders/**");
        System.out.println("              - Method=GET,POST");
        System.out.println("            filters:");
        System.out.println("              - StripPrefix=2");
        System.out.println("              - name: RequestRateLimiter");
        System.out.println("                args:");
        System.out.println("                  redis-rate-limiter.replenishRate: 10");
        System.out.println("                  redis-rate-limiter.burstCapacity: 20");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  API 网关演示 — 路由匹配 + 过滤器链（纯内存模拟）       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoGateway();
        demoConfiguration();
    }
}

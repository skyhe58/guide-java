package com.example.springcloud.tracing;

/**
 * 分布式链路追踪演示 — 模拟 TraceId/SpanId 传递 + MDC 日志关联
 *
 * <p>本示例用纯 Java 模拟分布式链路追踪的核心机制：
 * <ul>
 *   <li>TraceId / SpanId / ParentSpanId 生成与传递</li>
 *   <li>Span 生命周期（开始 → 标注 → 结束）</li>
 *   <li>MDC（Mapped Diagnostic Context）日志关联</li>
 *   <li>采样策略（全量 / 概率 / 限速）</li>
 *   <li>跨服务调用链路还原</li>
 * </ul>
 *
 * <h3>链路追踪模型：</h3>
 * <pre>
 *  Trace（一次完整请求）
 *  ├── Span A（Gateway，traceId=abc, spanId=1）
 *  │   ├── Span B（User Service，traceId=abc, spanId=2, parentId=1）
 *  │   │   └── Span D（MySQL Query，traceId=abc, spanId=4, parentId=2）
 *  │   └── Span C（Order Service，traceId=abc, spanId=3, parentId=1）
 *  │       └── Span E（Redis Cache，traceId=abc, spanId=5, parentId=3）
 *
 *  HTTP 头传递：
 *  X-B3-TraceId: abc
 *  X-B3-SpanId: 2
 *  X-B3-ParentSpanId: 1
 *  X-B3-Sampled: 1
 * </pre>
 */
public class TracingDemo {

    // ==================== 链路追踪模型 ====================

    /** Span：链路中的一个操作单元 */
    static class Span {
        final String traceId;
        final String spanId;
        final String parentSpanId;
        final String operationName;
        final String serviceName;
        final long startTime;
        long endTime;
        final java.util.Map<String, String> tags = new java.util.LinkedHashMap<>();
        final java.util.List<String> logs = new java.util.ArrayList<>();

        Span(String traceId, String spanId, String parentSpanId, String operationName, String serviceName) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.operationName = operationName;
            this.serviceName = serviceName;
            this.startTime = System.nanoTime();
        }

        void tag(String key, String value) { tags.put(key, value); }
        void log(String message) { logs.add(message); }

        void finish() { this.endTime = System.nanoTime(); }

        long durationMs() { return (endTime - startTime) / 1_000_000; }

        @Override
        public String toString() {
            return String.format("[%s] %s.%s (spanId=%s, parent=%s, %dms)",
                    traceId.substring(0, 8), serviceName, operationName, spanId, parentSpanId, durationMs());
        }
    }

    /** 链路追踪器 */
    static class Tracer {
        private int spanCounter = 0;
        private final java.util.List<Span> spans = new java.util.ArrayList<>();
        private final double samplingRate; // 采样率 0.0~1.0

        Tracer(double samplingRate) { this.samplingRate = samplingRate; }

        /** 创建新的 Trace（根 Span） */
        Span startTrace(String operationName, String serviceName) {
            String traceId = generateId();
            String spanId = String.valueOf(++spanCounter);
            Span span = new Span(traceId, spanId, "0", operationName, serviceName);
            spans.add(span);
            return span;
        }

        /** 创建子 Span */
        Span startChildSpan(Span parent, String operationName, String serviceName) {
            String spanId = String.valueOf(++spanCounter);
            Span span = new Span(parent.traceId, spanId, parent.spanId, operationName, serviceName);
            spans.add(span);
            return span;
        }

        /** 是否采样 */
        boolean shouldSample() { return Math.random() < samplingRate; }

        /** 获取完整调用链 */
        java.util.List<Span> getTrace(String traceId) {
            return spans.stream()
                    .filter(s -> s.traceId.equals(traceId))
                    .collect(java.util.stream.Collectors.toList());
        }

        private String generateId() {
            return Long.toHexString(System.nanoTime()) + Long.toHexString((long) (Math.random() * 0xFFFFFFL));
        }
    }

    /** 模拟 MDC（Mapped Diagnostic Context） */
    static class MDC {
        private static final ThreadLocal<java.util.Map<String, String>> context =
                ThreadLocal.withInitial(java.util.LinkedHashMap::new);

        static void put(String key, String value) { context.get().put(key, value); }
        static String get(String key) { return context.get().get(key); }
        static void clear() { context.get().clear(); }

        /** 模拟带 MDC 的日志输出 */
        static void log(String level, String message) {
            String traceId = get("traceId");
            String spanId = get("spanId");
            System.out.printf("    %s [%s] [traceId=%s, spanId=%s] %s%n",
                    java.time.LocalTime.now().toString().substring(0, 12),
                    level, traceId != null ? traceId.substring(0, 8) : "null",
                    spanId != null ? spanId : "null", message);
        }
    }

    // ==================== 演示方法 ====================

    static void demoTraceAndSpan() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：TraceId / SpanId 传递 + 调用链还原");
        System.out.println("═══════════════════════════════════════════════════");

        Tracer tracer = new Tracer(1.0);

        // 模拟一次完整请求：Gateway → UserService → MySQL
        //                              → OrderService → Redis
        System.out.println("\n  模拟请求：GET /api/users/1/orders");

        Span gateway = tracer.startTrace("GET /api/users/1/orders", "gateway");
        gateway.tag("http.method", "GET");
        gateway.tag("http.url", "/api/users/1/orders");

        // Gateway → UserService
        Span userService = tracer.startChildSpan(gateway, "getUser", "user-service");
        userService.tag("user.id", "1");

        // UserService → MySQL
        Span mysqlQuery = tracer.startChildSpan(userService, "SELECT * FROM user WHERE id=1", "mysql");
        mysqlQuery.tag("db.type", "mysql");
        try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        mysqlQuery.finish();

        userService.finish();

        // Gateway → OrderService
        Span orderService = tracer.startChildSpan(gateway, "getOrders", "order-service");
        orderService.tag("user.id", "1");

        // OrderService → Redis
        Span redisCache = tracer.startChildSpan(orderService, "GET user:1:orders", "redis");
        redisCache.tag("cache.hit", "true");
        try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        redisCache.finish();

        orderService.finish();
        gateway.finish();

        // 还原调用链
        System.out.println("\n  调用链还原：");
        java.util.List<Span> trace = tracer.getTrace(gateway.traceId);
        for (Span span : trace) {
            int depth = getDepth(span, trace);
            String indent = "  ".repeat(depth);
            System.out.printf("    %s%s%n", indent, span);
        }

        // HTTP 头传递
        System.out.println("\n  跨服务 HTTP 头传递：");
        System.out.printf("    X-B3-TraceId: %s%n", gateway.traceId);
        System.out.printf("    X-B3-SpanId: %s%n", userService.spanId);
        System.out.printf("    X-B3-ParentSpanId: %s%n", userService.parentSpanId);
        System.out.println("    X-B3-Sampled: 1");
        System.out.println();
    }

    static void demoMDCLogging() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：MDC 日志关联 — 按 TraceId 串联日志");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  模拟 Gateway → UserService 的日志输出：");

        // Gateway 层
        MDC.put("traceId", "abc123def456");
        MDC.put("spanId", "1");
        MDC.log("INFO", "[Gateway] 收到请求 GET /api/users/1");
        MDC.log("INFO", "[Gateway] 路由到 user-service");

        // UserService 层（新的 SpanId，TraceId 不变）
        MDC.put("spanId", "2");
        MDC.log("INFO", "[UserService] 处理 getUser(1)");
        MDC.log("DEBUG", "[UserService] 查询数据库 SELECT * FROM user WHERE id=1");
        MDC.log("INFO", "[UserService] 返回用户数据");

        // 回到 Gateway
        MDC.put("spanId", "1");
        MDC.log("INFO", "[Gateway] 响应 200 OK");

        MDC.clear();

        System.out.println("\n  logback 配置（添加 traceId 到日志格式）：");
        System.out.println("    <pattern>%d{HH:mm:ss} [%level] [traceId=%X{traceId}] %msg%n</pattern>");
        System.out.println("\n  ELK 中按 traceId 搜索即可串联完整调用链日志");
        System.out.println();
    }

    static void demoSamplingStrategy() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：采样策略");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  模拟不同采样率下的采样结果（1000 次请求）：");
        double[] rates = {1.0, 0.5, 0.1, 0.01};

        for (double rate : rates) {
            Tracer tracer = new Tracer(rate);
            int sampled = 0;
            for (int i = 0; i < 1000; i++) {
                if (tracer.shouldSample()) sampled++;
            }
            System.out.printf("    采样率 %.0f%%: 采样 %d / 1000 条%n", rate * 100, sampled);
        }

        System.out.println("\n  采样策略类型：");
        System.out.println("    全量采样（rate=1.0）— 开发/测试环境");
        System.out.println("    概率采样（rate=0.1）— 生产环境常用");
        System.out.println("    限速采样（每秒最多 N 条）— 高流量场景");
        System.out.println("    自适应采样 — 根据流量自动调整采样率");
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    static int getDepth(Span span, java.util.List<Span> trace) {
        if ("0".equals(span.parentSpanId)) return 0;
        for (Span parent : trace) {
            if (parent.spanId.equals(span.parentSpanId)) {
                return 1 + getDepth(parent, trace);
            }
        }
        return 0;
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  分布式链路追踪演示（纯内存模拟）                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoTraceAndSpan();
        demoMDCLogging();
        demoSamplingStrategy();
    }
}

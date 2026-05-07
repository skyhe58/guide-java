package com.example.monitoring;

/**
 * 监控指标采集演示 — 用 Micrometer SimpleMeterRegistry 模拟指标采集
 *
 * <p>本示例用纯 Java 模拟应用监控的核心指标类型：
 * <ul>
 *   <li>Counter（计数器）— 只增不减，如请求总数、错误总数</li>
 *   <li>Gauge（仪表盘）— 可增可减，如当前连接数、队列大小</li>
 *   <li>Timer（计时器）— 记录耗时分布，如接口响应时间</li>
 *   <li>Histogram（直方图）— 数据分布统计，如请求大小分布</li>
 *   <li>Prometheus 指标格式</li>
 * </ul>
 *
 * <h3>监控体系架构：</h3>
 * <pre>
 *  应用（Micrometer）→ Prometheus（采集存储）→ Grafana（可视化）→ AlertManager（告警）
 *
 *  Spring Boot Actuator 暴露 /actuator/prometheus 端点
 *  Prometheus 定期拉取（Pull 模式）
 *  Grafana 配置 Dashboard 展示
 * </pre>
 */
public class MonitoringDemo {

    // ==================== 指标模型 ====================

    /** Counter：只增不减的计数器 */
    static class Counter {
        private final String name;
        private final java.util.Map<String, String> tags;
        private final java.util.concurrent.atomic.AtomicLong count = new java.util.concurrent.atomic.AtomicLong(0);

        Counter(String name, java.util.Map<String, String> tags) {
            this.name = name;
            this.tags = tags != null ? tags : java.util.Collections.emptyMap();
        }

        void increment() { count.incrementAndGet(); }
        void increment(long amount) { count.addAndGet(amount); }
        long getCount() { return count.get(); }

        String toPrometheus() {
            String tagStr = tags.isEmpty() ? "" : tags.entrySet().stream()
                    .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
            return String.format("%s%s %d", name.replace(".", "_"), tagStr, count.get());
        }
    }

    /** Gauge：可增可减的仪表盘 */
    static class Gauge {
        private final String name;
        private final java.util.function.Supplier<Double> valueSupplier;

        Gauge(String name, java.util.function.Supplier<Double> valueSupplier) {
            this.name = name;
            this.valueSupplier = valueSupplier;
        }

        double getValue() { return valueSupplier.get(); }

        String toPrometheus() {
            return String.format("%s %.1f", name.replace(".", "_"), getValue());
        }
    }

    /** Timer：计时器（记录耗时分布） */
    static class Timer {
        private final String name;
        private final java.util.List<Long> recordings = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        Timer(String name) { this.name = name; }

        /** 记录一次耗时（纳秒） */
        void record(long durationNanos) { recordings.add(durationNanos); }

        /** 记录一个操作的耗时 */
        <T> T recordCallable(java.util.concurrent.Callable<T> callable) throws Exception {
            long start = System.nanoTime();
            try {
                return callable.call();
            } finally {
                record(System.nanoTime() - start);
            }
        }

        long count() { return recordings.size(); }
        double totalTimeMs() { return recordings.stream().mapToLong(Long::longValue).sum() / 1_000_000.0; }
        double meanMs() { return count() > 0 ? totalTimeMs() / count() : 0; }
        double maxMs() { return recordings.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000.0; }

        /** P99 百分位 */
        double percentile(double p) {
            if (recordings.isEmpty()) return 0;
            java.util.List<Long> sorted = new java.util.ArrayList<>(recordings);
            java.util.Collections.sort(sorted);
            int index = (int) Math.ceil(p * sorted.size()) - 1;
            return sorted.get(Math.max(0, index)) / 1_000_000.0;
        }

        String toPrometheus() {
            return String.format("%s_count %d\n%s_sum %.3f\n%s_max %.3f",
                    name.replace(".", "_"), count(),
                    name.replace(".", "_"), totalTimeMs(),
                    name.replace(".", "_"), maxMs());
        }
    }

    /** 模拟 MeterRegistry */
    static class SimpleMeterRegistry {
        private final java.util.Map<String, Object> meters = new java.util.LinkedHashMap<>();

        Counter counter(String name, java.util.Map<String, String> tags) {
            Counter c = new Counter(name, tags);
            meters.put(name + (tags != null ? tags.toString() : ""), c);
            return c;
        }

        Gauge gauge(String name, java.util.function.Supplier<Double> supplier) {
            Gauge g = new Gauge(name, supplier);
            meters.put(name, g);
            return g;
        }

        Timer timer(String name) {
            Timer t = new Timer(name);
            meters.put(name, t);
            return t;
        }

        /** 输出 Prometheus 格式 */
        String scrape() {
            StringBuilder sb = new StringBuilder();
            for (Object meter : meters.values()) {
                if (meter instanceof Counter) sb.append(((Counter) meter).toPrometheus()).append("\n");
                else if (meter instanceof Gauge) sb.append(((Gauge) meter).toPrometheus()).append("\n");
                else if (meter instanceof Timer) sb.append(((Timer) meter).toPrometheus()).append("\n");
            }
            return sb.toString();
        }
    }

    // ==================== 演示方法 ====================

    static void demoCounter() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Counter（计数器）— 请求总数统计");
        System.out.println("═══════════════════════════════════════════════════");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        java.util.Map<String, String> tags200 = new java.util.LinkedHashMap<>();
        tags200.put("method", "GET");
        tags200.put("status", "200");
        Counter requestsOk = registry.counter("http.requests.total", tags200);

        java.util.Map<String, String> tags500 = new java.util.LinkedHashMap<>();
        tags500.put("method", "GET");
        tags500.put("status", "500");
        Counter requestsErr = registry.counter("http.requests.total", tags500);

        // 模拟请求
        System.out.println("\n  模拟 100 次请求（90 成功 + 10 失败）：");
        for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) requestsErr.increment();
            else requestsOk.increment();
        }

        System.out.printf("    成功请求: %d%n", requestsOk.getCount());
        System.out.printf("    失败请求: %d%n", requestsErr.getCount());
        System.out.println("\n  Prometheus 格式：");
        System.out.println("    " + requestsOk.toPrometheus());
        System.out.println("    " + requestsErr.toPrometheus());
        System.out.println();
    }

    static void demoGauge() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Gauge（仪表盘）— 实时状态监控");
        System.out.println("═══════════════════════════════════════════════════");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        // 模拟连接池
        java.util.concurrent.atomic.AtomicInteger activeConnections = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.LinkedBlockingQueue<String> taskQueue = new java.util.concurrent.LinkedBlockingQueue<>();

        Gauge connGauge = registry.gauge("db.connections.active", () -> (double) activeConnections.get());
        Gauge queueGauge = registry.gauge("task.queue.size", () -> (double) taskQueue.size());

        System.out.println("\n  模拟连接池和任务队列变化：");
        activeConnections.set(5);
        taskQueue.addAll(java.util.Arrays.asList("task1", "task2", "task3"));
        System.out.printf("    活跃连接: %.0f, 队列大小: %.0f%n", connGauge.getValue(), queueGauge.getValue());

        activeConnections.set(8);
        taskQueue.poll();
        System.out.printf("    活跃连接: %.0f, 队列大小: %.0f%n", connGauge.getValue(), queueGauge.getValue());

        System.out.println("\n  Prometheus 格式：");
        System.out.println("    " + connGauge.toPrometheus());
        System.out.println("    " + queueGauge.toPrometheus());
        System.out.println();
    }

    static void demoTimer() throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Timer（计时器）— 接口响应时间");
        System.out.println("═══════════════════════════════════════════════════");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Timer apiTimer = registry.timer("http.request.duration");

        // 模拟不同耗时的请求
        System.out.println("\n  模拟 20 次 API 调用（随机耗时）：");
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < 20; i++) {
            int sleepMs = 5 + random.nextInt(50);
            apiTimer.recordCallable(() -> {
                Thread.sleep(sleepMs);
                return null;
            });
        }

        System.out.printf("    请求总数: %d%n", apiTimer.count());
        System.out.printf("    总耗时: %.1f ms%n", apiTimer.totalTimeMs());
        System.out.printf("    平均耗时: %.1f ms%n", apiTimer.meanMs());
        System.out.printf("    最大耗时: %.1f ms%n", apiTimer.maxMs());
        System.out.printf("    P50: %.1f ms%n", apiTimer.percentile(0.5));
        System.out.printf("    P95: %.1f ms%n", apiTimer.percentile(0.95));
        System.out.printf("    P99: %.1f ms%n", apiTimer.percentile(0.99));

        System.out.println("\n  Prometheus 格式：");
        for (String line : apiTimer.toPrometheus().split("\n")) {
            System.out.println("    " + line);
        }
        System.out.println();
    }

    static void demoPrometheusExport() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Prometheus 指标导出（/actuator/prometheus）");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  Spring Boot Actuator 配置：");
        System.out.println("    management:");
        System.out.println("      endpoints:");
        System.out.println("        web:");
        System.out.println("          exposure:");
        System.out.println("            include: health,info,prometheus");
        System.out.println("      metrics:");
        System.out.println("        tags:");
        System.out.println("          application: ${spring.application.name}");

        System.out.println("\n  Prometheus 配置（prometheus.yml）：");
        System.out.println("    scrape_configs:");
        System.out.println("      - job_name: 'spring-cloud-demo'");
        System.out.println("        metrics_path: '/actuator/prometheus'");
        System.out.println("        static_configs:");
        System.out.println("          - targets: ['localhost:8090']");

        System.out.println("\n  常用监控指标：");
        System.out.println("    http_server_requests_seconds_count  — 请求总数");
        System.out.println("    http_server_requests_seconds_sum    — 请求总耗时");
        System.out.println("    jvm_memory_used_bytes               — JVM 内存使用");
        System.out.println("    jvm_gc_pause_seconds_count          — GC 暂停次数");
        System.out.println("    hikaricp_connections_active          — 连接池活跃连接");
        System.out.println("    rabbitmq_consumed_total              — MQ 消费总数");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  监控指标采集演示（纯内存模拟）                         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoCounter();
        demoGauge();
        demoTimer();
        demoPrometheusExport();
    }
}

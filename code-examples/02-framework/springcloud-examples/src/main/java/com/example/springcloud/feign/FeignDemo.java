package com.example.springcloud.feign;

/**
 * 声明式 HTTP 调用演示 — 用 JDK 动态代理实现 Feign 核心机制
 *
 * <p>本示例用纯 Java 模拟 OpenFeign 的核心功能：
 * <ul>
 *   <li>动态代理：接口方法 → HTTP 请求的映射</li>
 *   <li>负载均衡：轮询 / 随机 / 加权轮询</li>
 *   <li>服务降级（Fallback）：调用失败时的兜底逻辑</li>
 *   <li>请求拦截器（RequestInterceptor）：统一添加请求头</li>
 *   <li>重试机制（Retryer）</li>
 * </ul>
 *
 * <h3>Feign 工作原理：</h3>
 * <pre>
 *  @FeignClient("user-service")
 *  public interface UserClient {
 *      @GetMapping("/users/{id}")
 *      User getUser(@PathVariable Long id);
 *  }
 *
 *  调用 userClient.getUser(1) 时的内部流程：
 *  1. JDK 动态代理拦截方法调用
 *  2. 解析注解，构建 HTTP 请求（GET http://user-service/users/1）
 *  3. 通过 LoadBalancer 选择服务实例（如 192.168.1.10:8080）
 *  4. RequestInterceptor 添加请求头（如 Authorization）
 *  5. 发送 HTTP 请求，获取响应
 *  6. 解码响应体为 User 对象
 *  7. 失败时触发 Fallback 或 Retryer
 * </pre>
 */
public class FeignDemo {

    // ==================== 服务接口定义 ====================

    /** 模拟 @FeignClient 接口 */
    interface UserService {
        String getUser(long id);
        String createUser(String name, int age);
        String listUsers();
    }

    /** 模拟 @FeignClient 的 Fallback */
    static class UserServiceFallback implements UserService {
        @Override
        public String getUser(long id) {
            return "{\"error\":\"用户服务不可用，返回降级数据\",\"id\":" + id + ",\"name\":\"默认用户\"}";
        }

        @Override
        public String createUser(String name, int age) {
            return "{\"error\":\"用户服务不可用，创建失败\"}";
        }

        @Override
        public String listUsers() {
            return "{\"error\":\"用户服务不可用\",\"data\":[]}";
        }
    }

    // ==================== 负载均衡 ====================

    /** 服务实例 */
    static class Instance {
        final String host;
        final int port;
        final int weight;

        Instance(String host, int port, int weight) {
            this.host = host; this.port = port; this.weight = weight;
        }

        String getUrl() { return "http://" + host + ":" + port; }

        @Override
        public String toString() { return host + ":" + port + "(w=" + weight + ")"; }
    }

    /** 负载均衡策略接口 */
    interface LoadBalancer {
        Instance choose(java.util.List<Instance> instances);
        String name();
    }

    /** 轮询策略 */
    static class RoundRobinBalancer implements LoadBalancer {
        private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

        @Override
        public Instance choose(java.util.List<Instance> instances) {
            int idx = Math.abs(counter.getAndIncrement()) % instances.size();
            return instances.get(idx);
        }

        @Override
        public String name() { return "RoundRobin"; }
    }

    /** 随机策略 */
    static class RandomBalancer implements LoadBalancer {
        private final java.util.Random random = new java.util.Random();

        @Override
        public Instance choose(java.util.List<Instance> instances) {
            return instances.get(random.nextInt(instances.size()));
        }

        @Override
        public String name() { return "Random"; }
    }

    /** 加权轮询策略 */
    static class WeightedRoundRobinBalancer implements LoadBalancer {
        @Override
        public Instance choose(java.util.List<Instance> instances) {
            // 按权重展开后轮询
            int totalWeight = instances.stream().mapToInt(i -> i.weight).sum();
            int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            for (Instance instance : instances) {
                cumulative += instance.weight;
                if (random < cumulative) return instance;
            }
            return instances.get(0);
        }

        @Override
        public String name() { return "WeightedRoundRobin"; }
    }

    // ==================== 请求拦截器 ====================

    /** 请求拦截器（模拟 RequestInterceptor） */
    interface RequestInterceptor {
        void apply(java.util.Map<String, String> headers);
    }

    /** 添加认证头 */
    static class AuthInterceptor implements RequestInterceptor {
        private final String token;

        AuthInterceptor(String token) { this.token = token; }

        @Override
        public void apply(java.util.Map<String, String> headers) {
            headers.put("Authorization", "Bearer " + token);
        }
    }

    // ==================== Feign 代理工厂 ====================

    /** 模拟 Feign 代理工厂：用 JDK 动态代理创建接口实现 */
    @SuppressWarnings("unchecked")
    static <T> T createFeignClient(Class<T> serviceInterface, T fallback,
                                    java.util.List<Instance> instances,
                                    LoadBalancer loadBalancer,
                                    java.util.List<RequestInterceptor> interceptors,
                                    int maxRetries) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class[]{serviceInterface},
                (proxy, method, args) -> {
                    // 构建请求信息
                    String methodName = method.getName();
                    java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();

                    // 应用拦截器
                    for (RequestInterceptor interceptor : interceptors) {
                        interceptor.apply(headers);
                    }

                    // 重试逻辑
                    Exception lastException = null;
                    for (int attempt = 0; attempt <= maxRetries; attempt++) {
                        try {
                            // 负载均衡选择实例
                            Instance instance = loadBalancer.choose(instances);
                            String url = instance.getUrl() + "/" + methodName;

                            // 模拟 HTTP 调用
                            System.out.printf("    [Feign] %s → %s (attempt=%d, headers=%s)%n",
                                    methodName, url, attempt + 1, headers);

                            // 模拟响应（实际会发 HTTP 请求）
                            if (args != null && args.length > 0) {
                                return String.format("{\"method\":\"%s\",\"instance\":\"%s\",\"args\":\"%s\"}",
                                        methodName, instance, java.util.Arrays.toString(args));
                            }
                            return String.format("{\"method\":\"%s\",\"instance\":\"%s\"}", methodName, instance);

                        } catch (Exception e) {
                            lastException = e;
                            if (attempt < maxRetries) {
                                System.out.printf("    [Feign] 重试 %d/%d: %s%n", attempt + 1, maxRetries, e.getMessage());
                            }
                        }
                    }

                    // 所有重试失败，触发 Fallback
                    if (fallback != null) {
                        System.out.printf("    [Feign] 触发 Fallback: %s%n", methodName);
                        return method.invoke(fallback, args);
                    }
                    throw lastException != null ? lastException : new RuntimeException("调用失败");
                });
    }

    // ==================== 演示方法 ====================

    /** 演示1：动态代理 + 负载均衡 */
    static void demoDynamicProxy() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：JDK 动态代理 + 负载均衡");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Instance> instances = java.util.Arrays.asList(
                new Instance("192.168.1.10", 8080, 3),
                new Instance("192.168.1.11", 8080, 2),
                new Instance("192.168.1.12", 8080, 1));

        System.out.println("\n  服务实例: " + instances);

        // 轮询负载均衡
        UserService client = createFeignClient(UserService.class, null, instances,
                new RoundRobinBalancer(), java.util.Collections.emptyList(), 0);

        System.out.println("\n  【轮询策略】连续调用 3 次：");
        client.getUser(1);
        client.getUser(2);
        client.getUser(3);

        // 加权轮询
        UserService weightedClient = createFeignClient(UserService.class, null, instances,
                new WeightedRoundRobinBalancer(), java.util.Collections.emptyList(), 0);

        System.out.println("\n  【加权轮询】连续调用 6 次（权重 3:2:1）：");
        java.util.Map<String, Integer> distribution = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 600; i++) {
            String result = weightedClient.getUser(i);
            String instance = result.split("\"instance\":\"")[1].split("\"")[0];
            distribution.merge(instance, 1, Integer::sum);
        }
        System.out.println("    分布统计：");
        distribution.forEach((k, v) -> System.out.printf("      %s: %d 次 (%.1f%%)%n", k, v, v * 100.0 / 600));
        System.out.println();
    }

    /** 演示2：请求拦截器 + Fallback 降级 */
    static void demoInterceptorAndFallback() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：请求拦截器 + Fallback 降级");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Instance> instances = java.util.Arrays.asList(
                new Instance("192.168.1.10", 8080, 1));

        // 带拦截器的客户端
        java.util.List<RequestInterceptor> interceptors = java.util.Arrays.asList(
                new AuthInterceptor("eyJhbGciOiJIUzI1NiJ9"));

        UserService client = createFeignClient(UserService.class,
                new UserServiceFallback(), instances,
                new RoundRobinBalancer(), interceptors, 2);

        System.out.println("\n  【带拦截器调用】自动添加 Authorization 头：");
        String result = client.getUser(1);
        System.out.printf("    结果: %s%n", result);

        // Fallback 演示
        System.out.println("\n  【Fallback 降级】当服务不可用时：");
        UserServiceFallback fallback = new UserServiceFallback();
        System.out.printf("    getUser(1) → %s%n", fallback.getUser(1));
        System.out.printf("    listUsers() → %s%n", fallback.listUsers());
        System.out.println();
    }

    /** 演示3：Feign 配置最佳实践 */
    static void demoConfiguration() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：OpenFeign 配置最佳实践");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  接口定义：");
        System.out.println("    @FeignClient(name = \"user-service\",");
        System.out.println("                 fallbackFactory = UserServiceFallbackFactory.class)");
        System.out.println("    public interface UserClient {");
        System.out.println("        @GetMapping(\"/users/{id}\")");
        System.out.println("        User getUser(@PathVariable Long id);");
        System.out.println("    }");

        System.out.println("\n  application.yml 推荐配置：");
        System.out.println("    feign:");
        System.out.println("      client:");
        System.out.println("        config:");
        System.out.println("          default:");
        System.out.println("            connectTimeout: 5000");
        System.out.println("            readTimeout: 10000");
        System.out.println("            loggerLevel: BASIC");
        System.out.println("      circuitbreaker:");
        System.out.println("        enabled: true          # 开启熔断");
        System.out.println("      compression:");
        System.out.println("        request:");
        System.out.println("          enabled: true         # 请求压缩");
        System.out.println("        response:");
        System.out.println("          enabled: true         # 响应压缩");

        System.out.println("\n  最佳实践：");
        System.out.println("    1. 使用 fallbackFactory 而非 fallback（可获取异常信息）");
        System.out.println("    2. 配合 Resilience4j 熔断器使用");
        System.out.println("    3. 开启请求/响应压缩减少网络传输");
        System.out.println("    4. 合理设置超时时间，避免级联故障");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  声明式 HTTP 调用演示 — JDK 动态代理模拟 Feign（纯内存）║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoDynamicProxy();
        demoInterceptorAndFallback();
        demoConfiguration();
    }
}

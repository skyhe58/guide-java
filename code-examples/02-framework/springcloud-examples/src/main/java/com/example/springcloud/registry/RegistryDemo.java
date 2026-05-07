package com.example.springcloud.registry;

/**
 * 服务注册与发现演示 — 用 ConcurrentHashMap 模拟注册中心
 *
 * <p>本示例用纯 Java 模拟服务注册中心的核心机制：
 * <ul>
 *   <li>服务注册（Register）：实例上线时注册到注册中心</li>
 *   <li>服务发现（Discovery）：消费者从注册中心获取服务实例列表</li>
 *   <li>心跳续约（Heartbeat）：实例定期发送心跳，证明自己存活</li>
 *   <li>服务剔除（Eviction）：超时未续约的实例被剔除</li>
 *   <li>健康检查（Health Check）：主动检测实例健康状态</li>
 * </ul>
 *
 * <h3>注册中心工作流程：</h3>
 * <pre>
 *  Service A (实例1)  ──注册──→  ┌──────────────┐  ←──发现──  Service B
 *  Service A (实例2)  ──注册──→  │   注册中心     │  ←──发现──  Service C
 *  Service A (实例3)  ──心跳──→  │  (Registry)   │
 *                               │              │
 *                               │  服务注册表：   │
 *                               │  serviceA:    │
 *                               │   - 实例1 ✓   │
 *                               │   - 实例2 ✓   │
 *                               │   - 实例3 ✗   │ ← 心跳超时，剔除
 *                               └──────────────┘
 * </pre>
 */
public class RegistryDemo {

    // ==================== 服务实例模型 ====================

    /** 服务实例 */
    static class ServiceInstance {
        final String serviceId;     // 服务名
        final String instanceId;    // 实例 ID
        final String host;
        final int port;
        volatile long lastHeartbeat;    // 最后心跳时间
        volatile boolean healthy = true;
        final java.util.Map<String, String> metadata;

        ServiceInstance(String serviceId, String instanceId, String host, int port) {
            this.serviceId = serviceId;
            this.instanceId = instanceId;
            this.host = host;
            this.port = port;
            this.lastHeartbeat = System.currentTimeMillis();
            this.metadata = new java.util.LinkedHashMap<>();
        }

        void heartbeat() { this.lastHeartbeat = System.currentTimeMillis(); }

        boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - lastHeartbeat > timeoutMs;
        }

        @Override
        public String toString() {
            return String.format("%s (%s:%d) [%s]", instanceId, host, port, healthy ? "UP" : "DOWN");
        }
    }

    // ==================== 注册中心 ====================

    /** 模拟注册中心（类似 Consul / Nacos / Eureka 的核心功能） */
    static class ServiceRegistry {
        // 注册表：serviceName → List<ServiceInstance>
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.List<ServiceInstance>> registry =
                new java.util.concurrent.ConcurrentHashMap<>();
        private final long heartbeatTimeout; // 心跳超时时间（ms）

        ServiceRegistry(long heartbeatTimeout) {
            this.heartbeatTimeout = heartbeatTimeout;
        }

        /** 注册服务实例 */
        void register(ServiceInstance instance) {
            registry.computeIfAbsent(instance.serviceId,
                    k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>())).add(instance);
            System.out.printf("    [注册] %s → %s:%d%n", instance.instanceId, instance.host, instance.port);
        }

        /** 注销服务实例 */
        void deregister(String serviceId, String instanceId) {
            java.util.List<ServiceInstance> instances = registry.get(serviceId);
            if (instances != null) {
                instances.removeIf(i -> i.instanceId.equals(instanceId));
                System.out.printf("    [注销] %s%n", instanceId);
            }
        }

        /** 服务发现：获取健康的实例列表 */
        java.util.List<ServiceInstance> discover(String serviceId) {
            java.util.List<ServiceInstance> instances = registry.getOrDefault(serviceId, java.util.Collections.emptyList());
            return instances.stream()
                    .filter(i -> i.healthy && !i.isExpired(heartbeatTimeout))
                    .collect(java.util.stream.Collectors.toList());
        }

        /** 获取所有实例（含不健康的） */
        java.util.List<ServiceInstance> getAllInstances(String serviceId) {
            return new java.util.ArrayList<>(registry.getOrDefault(serviceId, java.util.Collections.emptyList()));
        }

        /** 心跳续约 */
        boolean heartbeat(String serviceId, String instanceId) {
            java.util.List<ServiceInstance> instances = registry.get(serviceId);
            if (instances != null) {
                for (ServiceInstance instance : instances) {
                    if (instance.instanceId.equals(instanceId)) {
                        instance.heartbeat();
                        return true;
                    }
                }
            }
            return false;
        }

        /** 剔除超时实例 */
        int evictExpiredInstances() {
            int evicted = 0;
            for (var entry : registry.entrySet()) {
                java.util.List<ServiceInstance> instances = entry.getValue();
                java.util.List<ServiceInstance> expired = instances.stream()
                        .filter(i -> i.isExpired(heartbeatTimeout))
                        .collect(java.util.stream.Collectors.toList());
                for (ServiceInstance instance : expired) {
                    instance.healthy = false;
                    instances.remove(instance);
                    System.out.printf("    [剔除] %s（心跳超时 %dms）%n",
                            instance.instanceId, System.currentTimeMillis() - instance.lastHeartbeat);
                    evicted++;
                }
            }
            return evicted;
        }

        /** 获取注册的服务列表 */
        java.util.Set<String> getServices() { return registry.keySet(); }

        void printRegistry() {
            for (var entry : registry.entrySet()) {
                System.out.printf("    %s: %d 个实例%n", entry.getKey(), entry.getValue().size());
                for (ServiceInstance instance : entry.getValue()) {
                    String status = instance.isExpired(heartbeatTimeout) ? "EXPIRED" : (instance.healthy ? "UP" : "DOWN");
                    System.out.printf("      %s [%s]%n", instance, status);
                }
            }
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：服务注册与发现 */
    static void demoRegisterAndDiscover() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：服务注册与发现");
        System.out.println("═══════════════════════════════════════════════════");

        ServiceRegistry registry = new ServiceRegistry(5000);

        // 注册服务实例
        System.out.println("\n  注册 user-service 的 3 个实例：");
        registry.register(new ServiceInstance("user-service", "user-1", "192.168.1.10", 8080));
        registry.register(new ServiceInstance("user-service", "user-2", "192.168.1.11", 8080));
        registry.register(new ServiceInstance("user-service", "user-3", "192.168.1.12", 8080));

        System.out.println("\n  注册 order-service 的 2 个实例：");
        registry.register(new ServiceInstance("order-service", "order-1", "192.168.1.20", 8081));
        registry.register(new ServiceInstance("order-service", "order-2", "192.168.1.21", 8081));

        // 服务发现
        System.out.println("\n  服务发现 user-service：");
        java.util.List<ServiceInstance> users = registry.discover("user-service");
        for (ServiceInstance instance : users) {
            System.out.println("    " + instance);
        }

        System.out.println("\n  当前注册表：");
        registry.printRegistry();
        System.out.println();
    }

    /** 演示2：心跳续约与服务剔除 */
    static void demoHeartbeatAndEviction() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：心跳续约与服务剔除");
        System.out.println("═══════════════════════════════════════════════════");

        // 心跳超时设为 200ms，方便演示
        ServiceRegistry registry = new ServiceRegistry(200);

        ServiceInstance inst1 = new ServiceInstance("user-service", "user-1", "192.168.1.10", 8080);
        ServiceInstance inst2 = new ServiceInstance("user-service", "user-2", "192.168.1.11", 8080);
        registry.register(inst1);
        registry.register(inst2);

        System.out.printf("\n  初始: %d 个健康实例%n", registry.discover("user-service").size());

        // inst1 持续心跳，inst2 停止心跳
        System.out.println("  user-1 持续心跳，user-2 停止心跳...");
        for (int i = 0; i < 3; i++) {
            Thread.sleep(100);
            registry.heartbeat("user-service", "user-1");
        }

        // 执行剔除
        System.out.println("\n  执行剔除检查：");
        int evicted = registry.evictExpiredInstances();
        System.out.printf("  剔除 %d 个超时实例%n", evicted);
        System.out.printf("  剩余健康实例: %d%n", registry.discover("user-service").size());
        System.out.println();
    }

    /** 演示3：注册中心对比 */
    static void demoComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：主流注册中心对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"特性",       "Consul",      "Nacos",       "Eureka",      "ZooKeeper"},
                {"一致性协议",  "Raft",        "Raft+Distro", "AP（最终一致）", "ZAB（CP）"},
                {"健康检查",   "TCP/HTTP/gRPC","客户端心跳",    "客户端心跳",    "会话保持"},
                {"配置中心",   "KV 存储",      "✅ 内置",      "❌ 无",        "❌ 无"},
                {"Spring 集成","✅ 良好",      "✅ 良好",      "✅ 原生",      "⚠️ 需 Curator"},
                {"推荐度",     "★★★★★",      "★★★★★",      "★★★☆☆",      "★★★☆☆"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-10s %-15s %-15s %-15s %-15s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2],
                    comparison[i][3], comparison[i][4]);
            if (i == 0) System.out.println("  " + "─".repeat(70));
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  服务注册与发现演示 — 注册中心模拟（纯内存）             ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoRegisterAndDiscover();
        demoHeartbeatAndEviction();
        demoComparison();
    }
}

package com.example.middleware.registry.consul;

/**
 * Consul 注册中心演示（混合模式）
 *
 * <p>Part A：用 ConcurrentHashMap 模拟 Consul 核心功能（直接运行）
 * <ul>
 *   <li>服务注册与发现</li>
 *   <li>健康检查（HTTP / TCP / TTL）</li>
 *   <li>KV 存储（配置中心功能）</li>
 *   <li>ACL 访问控制</li>
 *   <li>多数据中心支持</li>
 * </ul>
 *
 * <p>Part B：用 consul-api 连接真实 Consul
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.consul.yml up -d consul}
 * <p>管理界面：http://localhost:8500
 *
 * <h3>Consul 核心特性：</h3>
 * <pre>
 *  Consul
 *  ├── 服务注册与发现 — 支持 HTTP/gRPC 健康检查
 *  ├── KV 存储 — 可作为配置中心（类似 etcd）
 *  ├── 多数据中心 — 原生支持跨数据中心
 *  ├── ACL — 细粒度访问控制
 *  └── Connect — 服务网格（Service Mesh）
 *
 *  一致性协议：Raft（CP 模型，强一致性）
 *  健康检查方式：HTTP / TCP / TTL / gRPC / Script
 * </pre>
 */
public class ConsulDemo {

    // ==================== Part A：模拟 Consul ====================

    /** 服务实例 */
    static class ServiceInstance {
        final String serviceId;
        final String instanceId;
        final String address;
        final int port;
        final java.util.Map<String, String> tags;
        final java.util.Map<String, String> meta;
        volatile boolean healthy = true;
        volatile long lastCheck;
        String healthCheckType; // HTTP / TCP / TTL

        ServiceInstance(String serviceId, String instanceId, String address, int port) {
            this.serviceId = serviceId;
            this.instanceId = instanceId;
            this.address = address;
            this.port = port;
            this.tags = new java.util.LinkedHashMap<>();
            this.meta = new java.util.LinkedHashMap<>();
            this.lastCheck = System.currentTimeMillis();
            this.healthCheckType = "TTL";
        }

        @Override
        public String toString() {
            return String.format("%s(%s:%d)[%s]", instanceId, address, port, healthy ? "passing" : "critical");
        }
    }

    /** 模拟 Consul Agent */
    static class SimulatedConsul {
        // 服务注册表
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.List<ServiceInstance>> services =
                new java.util.concurrent.ConcurrentHashMap<>();
        // KV 存储
        private final java.util.concurrent.ConcurrentHashMap<String, String> kvStore =
                new java.util.concurrent.ConcurrentHashMap<>();

        // --- 服务注册与发现 ---

        void registerService(ServiceInstance instance) {
            services.computeIfAbsent(instance.serviceId,
                    k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>())).add(instance);
        }

        void deregisterService(String instanceId) {
            for (var list : services.values()) {
                list.removeIf(i -> i.instanceId.equals(instanceId));
            }
        }

        /** 服务发现：只返回健康的实例 */
        java.util.List<ServiceInstance> discoverHealthy(String serviceId) {
            return services.getOrDefault(serviceId, java.util.Collections.emptyList()).stream()
                    .filter(i -> i.healthy)
                    .collect(java.util.stream.Collectors.toList());
        }

        /** 获取所有实例（含不健康的） */
        java.util.List<ServiceInstance> discoverAll(String serviceId) {
            return new java.util.ArrayList<>(services.getOrDefault(serviceId, java.util.Collections.emptyList()));
        }

        /** TTL 健康检查：实例主动上报 */
        void passTTL(String instanceId) {
            for (var list : services.values()) {
                for (ServiceInstance inst : list) {
                    if (inst.instanceId.equals(instanceId)) {
                        inst.healthy = true;
                        inst.lastCheck = System.currentTimeMillis();
                    }
                }
            }
        }

        /** 检查 TTL 超时的实例 */
        int checkTTLExpired(long ttlMs) {
            int failed = 0;
            for (var list : services.values()) {
                for (ServiceInstance inst : list) {
                    if ("TTL".equals(inst.healthCheckType)
                            && System.currentTimeMillis() - inst.lastCheck > ttlMs) {
                        inst.healthy = false;
                        failed++;
                    }
                }
            }
            return failed;
        }

        // --- KV 存储 ---

        void kvPut(String key, String value) { kvStore.put(key, value); }

        String kvGet(String key) { return kvStore.get(key); }

        boolean kvDelete(String key) { return kvStore.remove(key) != null; }

        /** 按前缀列出 KV */
        java.util.Map<String, String> kvList(String prefix) {
            java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
            for (var entry : kvStore.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
            return result;
        }

        java.util.Set<String> getServiceNames() { return services.keySet(); }
    }

    // ==================== Part A 演示方法 ====================

    static void demoServiceRegistration() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：服务注册与发现 + 健康检查");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedConsul consul = new SimulatedConsul();

        // 注册服务
        System.out.println("\n  注册 user-service 的 3 个实例：");
        for (int i = 1; i <= 3; i++) {
            ServiceInstance inst = new ServiceInstance("user-service", "user-" + i,
                    "192.168.1." + (10 + i), 8080);
            inst.tags.put("version", "v1.0");
            inst.meta.put("region", i <= 2 ? "cn-north" : "cn-south");
            consul.registerService(inst);
            System.out.printf("    注册: %s%n", inst);
        }

        // 服务发现
        System.out.println("\n  服务发现（只返回健康实例）：");
        for (ServiceInstance inst : consul.discoverHealthy("user-service")) {
            System.out.printf("    %s%n", inst);
        }

        // 模拟实例不健康
        System.out.println("\n  模拟 user-3 健康检查失败：");
        consul.discoverAll("user-service").stream()
                .filter(i -> i.instanceId.equals("user-3"))
                .forEach(i -> i.healthy = false);

        System.out.println("  再次发现（user-3 被过滤）：");
        for (ServiceInstance inst : consul.discoverHealthy("user-service")) {
            System.out.printf("    %s%n", inst);
        }
        System.out.println();
    }

    static void demoKVStore() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：KV 存储（配置中心功能）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedConsul consul = new SimulatedConsul();

        // 存储配置
        System.out.println("\n  写入配置：");
        consul.kvPut("config/user-service/db.url", "jdbc:mysql://localhost:3306/users");
        consul.kvPut("config/user-service/db.username", "root");
        consul.kvPut("config/user-service/db.password", "root123");
        consul.kvPut("config/user-service/cache.ttl", "3600");
        consul.kvPut("config/order-service/db.url", "jdbc:mysql://localhost:3306/orders");

        // 读取配置
        System.out.printf("    GET config/user-service/db.url = %s%n",
                consul.kvGet("config/user-service/db.url"));

        // 按前缀列出
        System.out.println("\n  列出 config/user-service/ 下所有配置：");
        consul.kvList("config/user-service/").forEach((k, v) ->
                System.out.printf("    %s = %s%n", k, v));

        // 更新配置
        System.out.println("\n  更新 cache.ttl：3600 → 7200");
        consul.kvPut("config/user-service/cache.ttl", "7200");
        System.out.printf("    GET config/user-service/cache.ttl = %s%n",
                consul.kvGet("config/user-service/cache.ttl"));
        System.out.println();
    }

    static void demoHealthCheck() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：TTL 健康检查机制");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedConsul consul = new SimulatedConsul();
        ServiceInstance inst1 = new ServiceInstance("api-service", "api-1", "10.0.0.1", 8080);
        ServiceInstance inst2 = new ServiceInstance("api-service", "api-2", "10.0.0.2", 8080);
        consul.registerService(inst1);
        consul.registerService(inst2);

        System.out.printf("\n  初始: %d 个健康实例%n", consul.discoverHealthy("api-service").size());

        // api-1 持续上报 TTL，api-2 停止上报
        System.out.println("  api-1 持续上报 TTL，api-2 停止上报...");
        for (int i = 0; i < 3; i++) {
            Thread.sleep(100);
            consul.passTTL("api-1");
        }

        // 检查 TTL 超时（200ms）
        int failed = consul.checkTTLExpired(200);
        System.out.printf("  TTL 检查: %d 个实例超时%n", failed);
        System.out.printf("  健康实例: %d%n", consul.discoverHealthy("api-service").size());

        System.out.println("\n  Consul 健康检查方式：");
        System.out.println("    HTTP — 定期 GET 指定 URL，2xx 为健康");
        System.out.println("    TCP  — 定期连接指定端口，连通为健康");
        System.out.println("    TTL  — 实例主动上报，超时未上报为不健康");
        System.out.println("    gRPC — 调用 gRPC 健康检查接口");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Consul 注册中心演示（混合模式）                        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("══════════ Part A：模拟 Consul 核心功能 ══════════");
        System.out.println();
        demoServiceRegistration();
        demoKVStore();
        demoHealthCheck();

        // Part B：连接真实 Consul，启动命令：docker compose -f docker/docker-compose.consul.yml up -d consul
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：consul-api 连接真实 Consul ══════════");
            System.out.println();
            RealConsul.run();
        }
    }

    // ==================== Part B：真实 Consul ====================

    static class RealConsul {

        static final String CONSUL_HOST = "localhost";
        static final int CONSUL_PORT = 8500;

        static void run() throws Exception {
            com.ecwid.consul.v1.ConsulClient client = new com.ecwid.consul.v1.ConsulClient(CONSUL_HOST, CONSUL_PORT);

            try {
                // 1. 注册服务
                System.out.println("  【注册服务】");
                com.ecwid.consul.v1.agent.model.NewService service = new com.ecwid.consul.v1.agent.model.NewService();
                service.setId("demo-user-1");
                service.setName("demo-user-service");
                service.setAddress("127.0.0.1");
                service.setPort(8080);
                service.setTags(java.util.Arrays.asList("v1.0", "demo"));

                // TTL 健康检查
                com.ecwid.consul.v1.agent.model.NewService.Check check = new com.ecwid.consul.v1.agent.model.NewService.Check();
                check.setTtl("30s");
                check.setDeregisterCriticalServiceAfter("60s");
                service.setCheck(check);

                client.agentServiceRegister(service);
                System.out.println("    注册: demo-user-1 (127.0.0.1:8080)");

                // 注册第二个实例
                com.ecwid.consul.v1.agent.model.NewService service2 = new com.ecwid.consul.v1.agent.model.NewService();
                service2.setId("demo-user-2");
                service2.setName("demo-user-service");
                service2.setAddress("127.0.0.1");
                service2.setPort(8081);
                service2.setTags(java.util.Arrays.asList("v1.0", "demo"));
                com.ecwid.consul.v1.agent.model.NewService.Check check2 = new com.ecwid.consul.v1.agent.model.NewService.Check();
                check2.setTtl("30s");
                service2.setCheck(check2);
                client.agentServiceRegister(service2);
                System.out.println("    注册: demo-user-2 (127.0.0.1:8081)");

                // 2. 上报 TTL 健康检查
                System.out.println("\n  【TTL 健康检查】");
                client.agentCheckPass("service:demo-user-1", "demo check pass");
                client.agentCheckPass("service:demo-user-2", "demo check pass");
                System.out.println("    demo-user-1 和 demo-user-2 TTL 上报成功");

                // 3. 服务发现
                System.out.println("\n  【服务发现】查询 demo-user-service：");
                var healthServices = client.getHealthServices("demo-user-service", true,
                        com.ecwid.consul.v1.QueryParams.DEFAULT);
                for (var hs : healthServices.getValue()) {
                    var svc = hs.getService();
                    System.out.printf("    %s (%s:%d) tags=%s%n",
                            svc.getId(), svc.getAddress(), svc.getPort(), svc.getTags());
                }

                // 4. KV 存储
                System.out.println("\n  【KV 存储】");
                client.setKVValue("demo/config/db.url", "jdbc:mysql://localhost:3306/demo");
                client.setKVValue("demo/config/db.username", "root");
                System.out.println("    PUT demo/config/db.url");
                System.out.println("    PUT demo/config/db.username");

                var kvValue = client.getKVValue("demo/config/db.url");
                if (kvValue.getValue() != null) {
                    System.out.printf("    GET demo/config/db.url = %s%n", kvValue.getValue().getDecodedValue());
                }

                // 列出前缀
                System.out.println("\n  【KV 列表】前缀 demo/config/：");
                var kvValues = client.getKVValues("demo/config/");
                if (kvValues.getValue() != null) {
                    for (var kv : kvValues.getValue()) {
                        System.out.printf("    %s = %s%n", kv.getKey(), kv.getDecodedValue());
                    }
                }

            } finally {
                // 清理（如需保留在 Consul UI 中查看，注释掉 cleanup 调用即可）
                cleanup(client);
            }
        }

        static void cleanup(com.ecwid.consul.v1.ConsulClient client) {
            client.agentServiceDeregister("demo-user-1");
            client.agentServiceDeregister("demo-user-2");
            client.deleteKVValues("demo/");
            System.out.println("\n  清理：已注销服务和删除 KV");
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }
}

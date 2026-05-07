package com.example.middleware.registry.nacos;

/**
 * Nacos 注册中心演示 — 用纯 Java 模拟 Nacos 核心功能
 *
 * <p>Part A：模拟 Nacos 服务注册发现（直接运行）
 * <ul>
 *   <li>AP/CP 模式切换（临时实例 vs 永久实例）</li>
 *   <li>心跳检测机制</li>
 *   <li>服务分组（Group）和命名空间（Namespace）</li>
 *   <li>权重路由</li>
 * </ul>
 *
 * <p>Nacos 需要单独部署，本项目优先使用 Consul，Nacos 作为对比学习。
 *
 * <h3>Nacos vs Consul 核心区别：</h3>
 * <pre>
 *  特性          Nacos                    Consul
 *  一致性协议    Raft(CP) + Distro(AP)    Raft(CP)
 *  临时实例      AP 模式（Distro 协议）    不区分
 *  永久实例      CP 模式（Raft 协议）      不区分
 *  配置中心      ✅ 内置（长轮询推送）      KV 存储（需自己实现监听）
 *  服务分组      ✅ Group + Namespace      ❌ 无（用 Tag 模拟）
 * </pre>
 */
public class NacosDemo {

    // ==================== 实例类型 ====================

    enum InstanceType {
        EPHEMERAL,  // 临时实例（AP 模式，客户端心跳，适合微服务）
        PERSISTENT  // 永久实例（CP 模式，服务端健康检查，适合数据库等基础设施）
    }

    /** Nacos 服务实例 */
    static class NacosInstance {
        final String serviceId;
        final String instanceId;
        final String ip;
        final int port;
        final InstanceType type;
        final String group;
        final String namespace;
        double weight;
        volatile boolean healthy = true;
        volatile long lastHeartbeat;

        NacosInstance(String serviceId, String instanceId, String ip, int port,
                      InstanceType type, String group, String namespace, double weight) {
            this.serviceId = serviceId;
            this.instanceId = instanceId;
            this.ip = ip;
            this.port = port;
            this.type = type;
            this.group = group;
            this.namespace = namespace;
            this.weight = weight;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("%s(%s:%d) [%s, %s, w=%.1f, %s]",
                    instanceId, ip, port, type, healthy ? "UP" : "DOWN", weight, group);
        }
    }

    /** 模拟 Nacos Naming Service */
    static class SimulatedNacos {
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.List<NacosInstance>> registry =
                new java.util.concurrent.ConcurrentHashMap<>();

        /** 注册实例 */
        void registerInstance(NacosInstance instance) {
            String key = instance.namespace + "@@" + instance.group + "@@" + instance.serviceId;
            registry.computeIfAbsent(key,
                    k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>())).add(instance);
        }

        /** 服务发现（按 group 和 namespace 过滤） */
        java.util.List<NacosInstance> getInstances(String serviceId, String group, String namespace) {
            String key = namespace + "@@" + group + "@@" + serviceId;
            return registry.getOrDefault(key, java.util.Collections.emptyList()).stream()
                    .filter(i -> i.healthy)
                    .collect(java.util.stream.Collectors.toList());
        }

        /** 心跳（仅临时实例需要） */
        void heartbeat(String instanceId) {
            for (var list : registry.values()) {
                for (NacosInstance inst : list) {
                    if (inst.instanceId.equals(instanceId) && inst.type == InstanceType.EPHEMERAL) {
                        inst.lastHeartbeat = System.currentTimeMillis();
                        inst.healthy = true;
                    }
                }
            }
        }

        /** 检查临时实例心跳超时 */
        int checkHeartbeatTimeout(long timeoutMs) {
            int failed = 0;
            for (var list : registry.values()) {
                for (NacosInstance inst : list) {
                    if (inst.type == InstanceType.EPHEMERAL
                            && System.currentTimeMillis() - inst.lastHeartbeat > timeoutMs) {
                        inst.healthy = false;
                        failed++;
                    }
                }
            }
            return failed;
        }

        /** 按权重选择实例（加权随机） */
        NacosInstance selectByWeight(java.util.List<NacosInstance> instances) {
            double totalWeight = instances.stream().mapToDouble(i -> i.weight).sum();
            double random = Math.random() * totalWeight;
            double cumulative = 0;
            for (NacosInstance inst : instances) {
                cumulative += inst.weight;
                if (random < cumulative) return inst;
            }
            return instances.get(0);
        }
    }

    // ==================== 演示方法 ====================

    static void demoAPvsCPMode() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：AP 模式（临时实例）vs CP 模式（永久实例）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedNacos nacos = new SimulatedNacos();

        // 临时实例（微服务）
        System.out.println("\n  【临时实例 — AP 模式】适合微服务");
        nacos.registerInstance(new NacosInstance("user-service", "user-1",
                "10.0.0.1", 8080, InstanceType.EPHEMERAL, "DEFAULT_GROUP", "public", 1.0));
        nacos.registerInstance(new NacosInstance("user-service", "user-2",
                "10.0.0.2", 8080, InstanceType.EPHEMERAL, "DEFAULT_GROUP", "public", 1.0));
        System.out.println("    特点：客户端心跳维持，超时自动剔除");
        System.out.println("    协议：Distro（AP，最终一致性，高可用优先）");

        // 永久实例（数据库）
        System.out.println("\n  【永久实例 — CP 模式】适合基础设施");
        nacos.registerInstance(new NacosInstance("mysql-service", "mysql-1",
                "10.0.1.1", 3306, InstanceType.PERSISTENT, "DEFAULT_GROUP", "public", 1.0));
        System.out.println("    特点：服务端主动健康检查，不会自动剔除");
        System.out.println("    协议：Raft（CP，强一致性，数据可靠优先）");

        System.out.println("\n  发现 user-service：");
        for (NacosInstance inst : nacos.getInstances("user-service", "DEFAULT_GROUP", "public")) {
            System.out.println("    " + inst);
        }
        System.out.println();
    }

    static void demoGroupAndNamespace() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：服务分组（Group）和命名空间（Namespace）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedNacos nacos = new SimulatedNacos();

        // 不同环境用不同 Namespace
        nacos.registerInstance(new NacosInstance("user-service", "user-dev-1",
                "10.0.0.1", 8080, InstanceType.EPHEMERAL, "DEFAULT_GROUP", "dev", 1.0));
        nacos.registerInstance(new NacosInstance("user-service", "user-prod-1",
                "10.0.1.1", 8080, InstanceType.EPHEMERAL, "DEFAULT_GROUP", "prod", 1.0));

        // 不同版本用不同 Group
        nacos.registerInstance(new NacosInstance("user-service", "user-v1",
                "10.0.0.1", 8080, InstanceType.EPHEMERAL, "v1", "prod", 1.0));
        nacos.registerInstance(new NacosInstance("user-service", "user-v2",
                "10.0.0.2", 8080, InstanceType.EPHEMERAL, "v2", "prod", 1.0));

        System.out.println("\n  查询 dev 环境的 user-service：");
        for (NacosInstance inst : nacos.getInstances("user-service", "DEFAULT_GROUP", "dev")) {
            System.out.println("    " + inst);
        }

        System.out.println("\n  查询 prod 环境 v2 版本的 user-service：");
        for (NacosInstance inst : nacos.getInstances("user-service", "v2", "prod")) {
            System.out.println("    " + inst);
        }

        System.out.println("\n  隔离层级：Namespace（环境隔离）> Group（版本/业务隔离）> Service");
        System.out.println();
    }

    static void demoWeightRouting() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：权重路由（灰度发布）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedNacos nacos = new SimulatedNacos();

        // v1 权重 90%，v2 权重 10%（灰度发布）
        NacosInstance v1 = new NacosInstance("user-service", "user-v1",
                "10.0.0.1", 8080, InstanceType.EPHEMERAL, "DEFAULT_GROUP", "public", 9.0);
        NacosInstance v2 = new NacosInstance("user-service", "user-v2",
                "10.0.0.2", 8080, InstanceType.EPHEMERAL, "DEFAULT_GROUP", "public", 1.0);
        nacos.registerInstance(v1);
        nacos.registerInstance(v2);

        System.out.println("\n  灰度发布：v1 权重=9.0（90%），v2 权重=1.0（10%）");

        // 模拟 1000 次路由
        java.util.Map<String, Integer> distribution = new java.util.LinkedHashMap<>();
        java.util.List<NacosInstance> instances = nacos.getInstances("user-service", "DEFAULT_GROUP", "public");
        for (int i = 0; i < 1000; i++) {
            NacosInstance selected = nacos.selectByWeight(instances);
            distribution.merge(selected.instanceId, 1, Integer::sum);
        }

        System.out.println("  1000 次路由分布：");
        distribution.forEach((id, count) ->
                System.out.printf("    %s: %d 次 (%.1f%%)%n", id, count, count / 10.0));
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Nacos 注册中心演示（纯内存模拟）                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoAPvsCPMode();
        demoGroupAndNamespace();
        demoWeightRouting();
    }
}

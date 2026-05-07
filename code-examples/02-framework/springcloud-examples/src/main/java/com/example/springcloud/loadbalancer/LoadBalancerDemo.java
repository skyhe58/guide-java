package com.example.springcloud.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Cloud 负载均衡演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>常见负载均衡策略的实现原理</li>
 *   <li>自定义负载均衡策略</li>
 *   <li>@LoadBalanced + RestTemplate 的使用说明</li>
 *   <li>一致性哈希算法</li>
 * </ul>
 *
 * <p>注意：本示例演示负载均衡算法原理，不需要实际启动微服务集群。</p>
 */
public class LoadBalancerDemo {

    // ==================== 1. 服务实例模型 ====================

    /**
     * 模拟服务实例
     */
    public record ServiceInstance(String host, int port, int weight) {
        @Override
        public String toString() {
            return host + ":" + port + "(weight=" + weight + ")";
        }
    }

    // ==================== 2. 轮询策略 (Round Robin) ====================

    /**
     * 轮询负载均衡 — 依次分配请求到每个实例
     *
     * <p>原理：维护一个原子计数器，每次请求递增，对实例数取模</p>
     * <p>适用场景：实例性能相近的场景</p>
     */
    public static class RoundRobinLoadBalancer {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final List<ServiceInstance> instances;

        public RoundRobinLoadBalancer(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        public ServiceInstance choose() {
            if (instances.isEmpty()) {
                throw new IllegalStateException("没有可用的服务实例");
            }
            int index = Math.abs(counter.getAndIncrement() % instances.size());
            return instances.get(index);
        }
    }

    // ==================== 3. 随机策略 (Random) ====================

    /**
     * 随机负载均衡 — 随机选择一个实例
     *
     * <p>原理：使用随机数生成器选择实例</p>
     * <p>适用场景：简单场景，实例数量较多时趋近于均匀分布</p>
     */
    public static class RandomLoadBalancer {
        private final Random random = new Random();
        private final List<ServiceInstance> instances;

        public RandomLoadBalancer(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        public ServiceInstance choose() {
            if (instances.isEmpty()) {
                throw new IllegalStateException("没有可用的服务实例");
            }
            int index = random.nextInt(instances.size());
            return instances.get(index);
        }
    }

    // ==================== 4. 加权轮询策略 (Weighted Round Robin) ====================

    /**
     * 加权轮询负载均衡 — 按权重分配请求
     *
     * <p>原理：将实例按权重展开为列表，然后轮询</p>
     * <p>适用场景：实例性能不均的场景（高配机器权重高）</p>
     */
    public static class WeightedRoundRobinLoadBalancer {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final List<ServiceInstance> expandedInstances;

        public WeightedRoundRobinLoadBalancer(List<ServiceInstance> instances) {
            // 按权重展开实例列表
            this.expandedInstances = new ArrayList<>();
            for (ServiceInstance instance : instances) {
                for (int i = 0; i < instance.weight(); i++) {
                    expandedInstances.add(instance);
                }
            }
        }

        public ServiceInstance choose() {
            if (expandedInstances.isEmpty()) {
                throw new IllegalStateException("没有可用的服务实例");
            }
            int index = Math.abs(counter.getAndIncrement() % expandedInstances.size());
            return expandedInstances.get(index);
        }
    }

    // ==================== 5. 一致性哈希策略 (Consistent Hash) ====================

    /**
     * 一致性哈希负载均衡 — 相同请求参数路由到同一实例
     *
     * <p>原理：将实例映射到哈希环上，请求根据 key 的哈希值顺时针找到最近的实例</p>
     * <p>适用场景：有状态服务、缓存场景（相同用户请求同一实例）</p>
     *
     * <p>虚拟节点解决数据倾斜问题：每个实际节点映射多个虚拟节点到哈希环上</p>
     */
    public static class ConsistentHashLoadBalancer {
        private final TreeMap<Long, ServiceInstance> hashRing = new TreeMap<>();
        private static final int VIRTUAL_NODES = 150; // 每个实例的虚拟节点数

        public ConsistentHashLoadBalancer(List<ServiceInstance> instances) {
            for (ServiceInstance instance : instances) {
                for (int i = 0; i < VIRTUAL_NODES; i++) {
                    long hash = hash(instance.toString() + "#" + i);
                    hashRing.put(hash, instance);
                }
            }
        }

        /**
         * 根据请求 key 选择实例
         *
         * @param key 请求标识（如用户 ID、商品 ID）
         * @return 选中的服务实例
         */
        public ServiceInstance choose(String key) {
            if (hashRing.isEmpty()) {
                throw new IllegalStateException("没有可用的服务实例");
            }
            long hash = hash(key);
            // 顺时针找到第一个大于等于 hash 的节点
            SortedMap<Long, ServiceInstance> tailMap = hashRing.tailMap(hash);
            Long targetHash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
            return hashRing.get(targetHash);
        }

        private long hash(String key) {
            // 简单的 FNV-1a 哈希算法
            long hash = 0xcbf29ce484222325L;
            for (int i = 0; i < key.length(); i++) {
                hash ^= key.charAt(i);
                hash *= 0x100000001b3L;
            }
            return hash;
        }
    }

    // ==================== 6. @LoadBalanced 使用说明 ====================

    /**
     * @LoadBalanced + RestTemplate 使用说明
     *
     * <pre>
     * // 配置类中声明 @LoadBalanced 的 RestTemplate
     * &#64;Configuration
     * public class LoadBalancerConfig {
     *     &#64;Bean
     *     &#64;LoadBalanced  // 关键注解：使 RestTemplate 具备负载均衡能力
     *     public RestTemplate restTemplate() {
     *         return new RestTemplate();
     *     }
     * }
     *
     * // 使用时直接用服务名调用
     * &#64;Service
     * public class OrderService {
     *     private final RestTemplate restTemplate;
     *
     *     public String getUser(Long userId) {
     *         // "user-service" 会被替换为实际的 IP:Port
     *         return restTemplate.getForObject(
     *             "http://user-service/api/users/" + userId, String.class);
     *     }
     * }
     * </pre>
     *
     * <p>原理：@LoadBalanced 为 RestTemplate 添加 LoadBalancerInterceptor 拦截器，
     * 拦截器从 URL 中提取服务名，通过注册中心获取实例列表，
     * 使用负载均衡算法选择实例后替换 URL。</p>
     */
    public static void loadBalancedUsage() {
        System.out.println("=== @LoadBalanced + RestTemplate 使用说明 ===");
        System.out.println();
        System.out.println("1. 在配置类中声明 @Bean @LoadBalanced RestTemplate");
        System.out.println("2. 使用时用服务名替代 IP:Port（如 http://user-service/api/users）");
        System.out.println("3. 底层通过 LoadBalancerInterceptor 拦截请求");
        System.out.println("4. 从注册中心获取实例列表 → 负载均衡选择 → 替换 URL → 发起请求");
        System.out.println();
        System.out.println("注意：不加 @LoadBalanced 的 RestTemplate 无法通过服务名调用！");
    }

    // ==================== main 方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Spring Cloud 负载均衡策略演示          ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // 模拟服务实例
        List<ServiceInstance> instances = List.of(
                new ServiceInstance("192.168.1.10", 8080, 3),
                new ServiceInstance("192.168.1.11", 8080, 2),
                new ServiceInstance("192.168.1.12", 8080, 1)
        );

        // 1. 轮询策略演示
        System.out.println("--- 轮询策略 (Round Robin) ---");
        RoundRobinLoadBalancer roundRobin = new RoundRobinLoadBalancer(instances);
        for (int i = 0; i < 6; i++) {
            System.out.println("  请求 " + (i + 1) + " → " + roundRobin.choose());
        }
        System.out.println();

        // 2. 随机策略演示
        System.out.println("--- 随机策略 (Random) ---");
        RandomLoadBalancer random = new RandomLoadBalancer(instances);
        Map<String, Integer> randomStats = new ConcurrentHashMap<>();
        for (int i = 0; i < 1000; i++) {
            ServiceInstance chosen = random.choose();
            randomStats.merge(chosen.toString(), 1, Integer::sum);
        }
        randomStats.forEach((k, v) -> System.out.println("  " + k + " → " + v + " 次"));
        System.out.println();

        // 3. 加权轮询策略演示
        System.out.println("--- 加权轮询策略 (Weighted Round Robin) ---");
        WeightedRoundRobinLoadBalancer weighted = new WeightedRoundRobinLoadBalancer(instances);
        Map<String, Integer> weightedStats = new ConcurrentHashMap<>();
        for (int i = 0; i < 6; i++) {
            ServiceInstance chosen = weighted.choose();
            weightedStats.merge(chosen.toString(), 1, Integer::sum);
            System.out.println("  请求 " + (i + 1) + " → " + chosen);
        }
        System.out.println();

        // 4. 一致性哈希策略演示
        System.out.println("--- 一致性哈希策略 (Consistent Hash) ---");
        ConsistentHashLoadBalancer consistentHash = new ConsistentHashLoadBalancer(instances);
        String[] userIds = {"user-001", "user-002", "user-003", "user-001", "user-002"};
        for (String userId : userIds) {
            System.out.println("  " + userId + " → " + consistentHash.choose(userId));
        }
        System.out.println("  (相同 userId 始终路由到同一实例)");
        System.out.println();

        // 5. @LoadBalanced 使用说明
        System.out.println("─".repeat(50));
        System.out.println();
        loadBalancedUsage();
    }
}

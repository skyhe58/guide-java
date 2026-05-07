package com.example.database.sharding;

/**
 * 分库分表演示 — 实现取模分片、范围分片、一致性哈希分片算法对比
 *
 * <p>本示例用纯 Java 实现三种常见的分片策略：
 * <ul>
 *   <li>取模分片（Hash Sharding）— 数据均匀分布，但扩容需要数据迁移</li>
 *   <li>范围分片（Range Sharding）— 支持范围查询，但可能数据倾斜</li>
 *   <li>一致性哈希（Consistent Hashing）— 扩容只需迁移少量数据</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>分库分表策略选择：</h3>
 * <pre>
 *  单表数据量 > 500万 或 单库连接数不够 → 考虑分库分表
 *
 *  水平分表：同一张表拆分到多个表（user_0, user_1, user_2）
 *  水平分库：同一张表拆分到多个数据库实例
 *  垂直分表：按列拆分（常用列和不常用列分开）
 *  垂直分库：按业务拆分（用户库、订单库、商品库）
 * </pre>
 */
public class ShardingDemo {

    // ==================== 分片策略接口 ====================

    /** 分片策略接口 */
    interface ShardingStrategy {
        String name();
        int route(long shardingKey, int shardCount);
    }

    // ==================== 取模分片 ====================

    /**
     * 取模分片：shardingKey % shardCount
     * 优点：数据分布均匀
     * 缺点：扩容时需要迁移大量数据（如 4 分片扩到 8 分片，约 50% 数据需迁移）
     */
    static class HashSharding implements ShardingStrategy {
        @Override
        public String name() { return "取模分片"; }

        @Override
        public int route(long shardingKey, int shardCount) {
            return (int) (Math.abs(shardingKey) % shardCount);
        }
    }

    // ==================== 范围分片 ====================

    /**
     * 范围分片：按 shardingKey 的范围划分
     * 优点：支持范围查询，扩容只需新增分片
     * 缺点：可能数据倾斜（热点数据集中在某个分片）
     */
    static class RangeSharding implements ShardingStrategy {
        private final long[] boundaries; // 分片边界

        RangeSharding(long[] boundaries) {
            this.boundaries = boundaries;
        }

        @Override
        public String name() { return "范围分片"; }

        @Override
        public int route(long shardingKey, int shardCount) {
            for (int i = 0; i < boundaries.length; i++) {
                if (shardingKey < boundaries[i]) {
                    return i;
                }
            }
            return boundaries.length; // 最后一个分片
        }
    }

    // ==================== 一致性哈希 ====================

    /**
     * 一致性哈希分片：将分片节点映射到哈希环上。
     * 优点：扩容/缩容只需迁移相邻节点的数据（约 1/N）
     * 缺点：实现复杂，需要虚拟节点解决数据倾斜
     *
     * <pre>
     *  哈希环（0 ~ 2^32-1）：
     *       0
     *      / \
     *   Node0  Node1
     *    |      |
     *   Node3  Node2
     *      \ /
     *     2^31
     * </pre>
     */
    static class ConsistentHashSharding implements ShardingStrategy {
        private final java.util.TreeMap<Long, Integer> ring = new java.util.TreeMap<>();
        private final int virtualNodes; // 每个物理节点的虚拟节点数

        ConsistentHashSharding(int shardCount, int virtualNodes) {
            this.virtualNodes = virtualNodes;
            for (int i = 0; i < shardCount; i++) {
                addNode(i);
            }
        }

        @Override
        public String name() { return "一致性哈希"; }

        @Override
        public int route(long shardingKey, int shardCount) {
            long hash = hash(String.valueOf(shardingKey));
            // 顺时针找到第一个 >= hash 的节点
            java.util.Map.Entry<Long, Integer> entry = ring.ceilingEntry(hash);
            if (entry == null) {
                // 超过最大值，回到环的起点
                entry = ring.firstEntry();
            }
            return entry.getValue();
        }

        /** 添加物理节点（含虚拟节点） */
        void addNode(int nodeId) {
            for (int i = 0; i < virtualNodes; i++) {
                long hash = hash("node-" + nodeId + "-vn-" + i);
                ring.put(hash, nodeId);
            }
        }

        /** 移除物理节点 */
        void removeNode(int nodeId) {
            for (int i = 0; i < virtualNodes; i++) {
                long hash = hash("node-" + nodeId + "-vn-" + i);
                ring.remove(hash);
            }
        }

        int getRingSize() {
            return ring.size();
        }

        /** FNV-1a 哈希算法 */
        private static long hash(String key) {
            long hash = 0xcbf29ce484222325L;
            for (byte b : key.getBytes()) {
                hash ^= b;
                hash *= 0x100000001b3L;
            }
            return hash & 0x7fffffffffffffffL; // 保证正数
        }
    }

    // ==================== 模拟分片表 ====================

    /** 模拟分片后的多个表 */
    static class ShardedTable {
        private final java.util.Map<Integer, java.util.List<long[]>> shards = new java.util.LinkedHashMap<>();
        private final ShardingStrategy strategy;
        private final int shardCount;

        ShardedTable(ShardingStrategy strategy, int shardCount) {
            this.strategy = strategy;
            this.shardCount = shardCount;
            for (int i = 0; i < shardCount; i++) {
                shards.put(i, new java.util.ArrayList<>());
            }
        }

        /** 插入数据 */
        void insert(long id, long data) {
            int shard = strategy.route(id, shardCount);
            shards.get(shard).add(new long[]{id, data});
        }

        /** 查询数据（需要路由到正确的分片） */
        Long query(long id) {
            int shard = strategy.route(id, shardCount);
            for (long[] row : shards.get(shard)) {
                if (row[0] == id) return row[1];
            }
            return null;
        }

        /** 获取各分片的数据量 */
        java.util.Map<Integer, Integer> getDistribution() {
            java.util.Map<Integer, Integer> dist = new java.util.LinkedHashMap<>();
            for (var entry : shards.entrySet()) {
                dist.put(entry.getKey(), entry.getValue().size());
            }
            return dist;
        }

        /** 计算数据分布的标准差（衡量均匀程度） */
        double getStdDev() {
            double avg = shards.values().stream().mapToInt(java.util.List::size).average().orElse(0);
            double variance = shards.values().stream()
                    .mapToDouble(l -> Math.pow(l.size() - avg, 2))
                    .average().orElse(0);
            return Math.sqrt(variance);
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：三种分片策略的数据分布对比 */
    static void demoShardingComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：三种分片策略数据分布对比");
        System.out.println("═══════════════════════════════════════════════════");

        int shardCount = 4;
        int dataCount = 10000;

        // 取模分片
        ShardedTable hashTable = new ShardedTable(new HashSharding(), shardCount);
        // 范围分片：[0,2500), [2500,5000), [5000,7500), [7500,+∞)
        ShardedTable rangeTable = new ShardedTable(
                new RangeSharding(new long[]{2500, 5000, 7500}), shardCount);
        // 一致性哈希（150 个虚拟节点）
        ShardedTable consistentTable = new ShardedTable(
                new ConsistentHashSharding(shardCount, 150), shardCount);

        // 插入数据
        for (long i = 0; i < dataCount; i++) {
            hashTable.insert(i, i * 10);
            rangeTable.insert(i, i * 10);
            consistentTable.insert(i, i * 10);
        }

        System.out.printf("\n  数据量: %d, 分片数: %d%n", dataCount, shardCount);

        // 对比分布
        System.out.println("\n  【取模分片】id % 4");
        printDistribution(hashTable);

        System.out.println("\n  【范围分片】[0,2500) [2500,5000) [5000,7500) [7500,+∞)");
        printDistribution(rangeTable);

        System.out.println("\n  【一致性哈希】150 虚拟节点/物理节点");
        printDistribution(consistentTable);
        System.out.println();
    }

    /** 演示2：一致性哈希扩容 — 数据迁移量对比 */
    static void demoConsistentHashExpansion() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：扩容时数据迁移量对比");
        System.out.println("═══════════════════════════════════════════════════");

        int dataCount = 10000;
        int oldShardCount = 4;
        int newShardCount = 5;

        // 取模分片扩容：4 → 5
        System.out.println("\n  场景：4 个分片扩容到 5 个分片");
        HashSharding hashOld = new HashSharding();
        HashSharding hashNew = new HashSharding();
        int hashMigrate = 0;
        for (long i = 0; i < dataCount; i++) {
            if (hashOld.route(i, oldShardCount) != hashNew.route(i, newShardCount)) {
                hashMigrate++;
            }
        }
        System.out.printf("  【取模分片】需迁移: %d / %d = %.1f%%%n",
                hashMigrate, dataCount, hashMigrate * 100.0 / dataCount);

        // 一致性哈希扩容：添加第 5 个节点
        ConsistentHashSharding chOld = new ConsistentHashSharding(oldShardCount, 150);
        ConsistentHashSharding chNew = new ConsistentHashSharding(oldShardCount, 150);
        chNew.addNode(4); // 添加第 5 个节点
        int chMigrate = 0;
        for (long i = 0; i < dataCount; i++) {
            if (chOld.route(i, oldShardCount) != chNew.route(i, newShardCount)) {
                chMigrate++;
            }
        }
        System.out.printf("  【一致性哈希】需迁移: %d / %d = %.1f%%%n",
                chMigrate, dataCount, chMigrate * 100.0 / dataCount);

        System.out.printf("\n  结论：一致性哈希迁移量约 1/N = %.1f%%，取模分片约 %.1f%%%n",
                100.0 / newShardCount, hashMigrate * 100.0 / dataCount);
        System.out.println("  一致性哈希在扩容场景下优势明显");
        System.out.println();
    }

    /** 演示3：虚拟节点对数据均匀性的影响 */
    static void demoVirtualNodes() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：虚拟节点数量对数据均匀性的影响");
        System.out.println("═══════════════════════════════════════════════════");

        int shardCount = 4;
        int dataCount = 10000;
        int[] vnCounts = {1, 10, 50, 100, 150, 300};

        System.out.printf("\n  %-12s %-40s %-10s%n", "虚拟节点数", "各分片数据量", "标准差");
        System.out.println("  " + "─".repeat(65));

        for (int vn : vnCounts) {
            ShardedTable table = new ShardedTable(
                    new ConsistentHashSharding(shardCount, vn), shardCount);
            for (long i = 0; i < dataCount; i++) {
                table.insert(i, i);
            }
            java.util.Map<Integer, Integer> dist = table.getDistribution();
            System.out.printf("  %-12d %-40s %-10.1f%n", vn, dist, table.getStdDev());
        }

        System.out.println("\n  结论：虚拟节点越多，数据分布越均匀（推荐 100~200）");
        System.out.println("  但虚拟节点过多会增加内存开销和路由计算时间");
        System.out.println();
    }

    /** 演示4：分库分表后的查询路由 */
    static void demoQueryRouting() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：分库分表后的查询路由");
        System.out.println("═══════════════════════════════════════════════════");

        int shardCount = 4;
        ShardedTable table = new ShardedTable(new HashSharding(), shardCount);

        // 插入数据
        for (long i = 1; i <= 20; i++) {
            table.insert(i, i * 100);
        }

        System.out.println("\n  分片规则：user_id % 4");
        System.out.println("  表名映射：user_0, user_1, user_2, user_3");

        // 精确查询：直接路由到目标分片
        System.out.println("\n  【精确查询】SELECT * FROM user WHERE user_id = 7");
        int shard = new HashSharding().route(7, shardCount);
        Long result = table.query(7);
        System.out.printf("    路由到: user_%d, 结果: %s%n", shard, result);

        // 范围查询：需要查所有分片（扇出查询）
        System.out.println("\n  【范围查询】SELECT * FROM user WHERE user_id BETWEEN 5 AND 10");
        System.out.println("    需要查询所有分片（无法确定数据在哪个分片）：");
        for (int i = 0; i < shardCount; i++) {
            System.out.printf("    → 查询 user_%d%n", i);
        }
        System.out.println("    然后合并结果（性能较差，应尽量避免）");

        // 非分片键查询
        System.out.println("\n  【非分片键查询】SELECT * FROM user WHERE name = '张三'");
        System.out.println("    name 不是分片键，无法确定路由 → 广播到所有分片");
        System.out.println("    优化方案：");
        System.out.println("    1. 建立 name → user_id 的映射表");
        System.out.println("    2. 使用 ES 做全文检索，再用 user_id 回查");
        System.out.println("    3. 基因法：将分片信息编码到 ID 中");

        System.out.println("\n  分库分表最佳实践：");
        System.out.println("    1. 选择合适的分片键（高频查询条件，如 user_id）");
        System.out.println("    2. 避免跨分片 JOIN（改为应用层关联）");
        System.out.println("    3. 分布式 ID 方案（雪花算法/号段模式）");
        System.out.println("    4. 考虑使用 ShardingSphere 等中间件简化开发");
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    static void printDistribution(ShardedTable table) {
        java.util.Map<Integer, Integer> dist = table.getDistribution();
        int total = dist.values().stream().mapToInt(Integer::intValue).sum();
        for (var entry : dist.entrySet()) {
            int count = entry.getValue();
            double pct = count * 100.0 / total;
            int barLen = (int) (pct / 2);
            String bar = "█".repeat(barLen);
            System.out.printf("    shard_%d: %5d (%5.1f%%) %s%n",
                    entry.getKey(), count, pct, bar);
        }
        System.out.printf("    标准差: %.1f（越小越均匀）%n", table.getStdDev());
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  分库分表演示 — 三种分片算法对比（纯内存模拟）          ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoShardingComparison();
        demoConsistentHashExpansion();
        demoVirtualNodes();
        demoQueryRouting();
    }
}

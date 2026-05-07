package com.example.distributed.lock;

/**
 * 分布式锁方案对比演示（混合模式）
 *
 * <p>Part A：用 Java 并发工具模拟三种分布式锁（直接运行）
 * <ul>
 *   <li>Redis 锁（SET NX EX + Lua 释放）</li>
 *   <li>ZooKeeper 锁（临时顺序节点）</li>
 *   <li>数据库锁（SELECT FOR UPDATE）</li>
 *   <li>可重入、续期（看门狗）、红锁</li>
 * </ul>
 *
 * <p>Part B：用 Jedis 实现真实 Redis 分布式锁
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d redis}
 *
 * <h3>三种分布式锁对比：</h3>
 * <pre>
 *  方案        实现原理                    优点              缺点
 *  Redis      SET key NX EX + Lua 释放    性能最高           主从切换可能丢锁
 *  ZooKeeper  临时顺序节点 + Watcher       可靠性最高          性能较低
 *  数据库      SELECT FOR UPDATE           实现简单           性能最差，死锁风险
 * </pre>
 */
public class DistributedLockCompare {

    // ==================== Part A：模拟三种分布式锁 ====================

    /** 模拟 Redis 分布式锁 */
    static class SimulatedRedisLock {
        private final java.util.concurrent.ConcurrentHashMap<String, String> store =
                new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.concurrent.ConcurrentHashMap<String, Integer> reentrantCount =
                new java.util.concurrent.ConcurrentHashMap<>();

        /**
         * 加锁：SET key value NX EX seconds
         * NX = 不存在才设置（互斥）
         * EX = 设置过期时间（防死锁）
         */
        boolean lock(String key, String requestId, int expireSeconds) {
            // 可重入检查
            String existing = store.get(key);
            if (requestId.equals(existing)) {
                reentrantCount.merge(key, 1, Integer::sum);
                return true;
            }
            // NX：不存在才设置
            String prev = store.putIfAbsent(key, requestId);
            if (prev == null) {
                reentrantCount.put(key, 1);
                return true;
            }
            return false;
        }

        /**
         * 释放锁：Lua 脚本保证原子性
         * if redis.call("get", key) == requestId then redis.call("del", key) end
         * 必须验证 requestId，防止误删其他线程的锁
         */
        boolean unlock(String key, String requestId) {
            String existing = store.get(key);
            if (!requestId.equals(existing)) return false;

            int count = reentrantCount.getOrDefault(key, 0);
            if (count > 1) {
                reentrantCount.put(key, count - 1);
                return true;
            }
            store.remove(key);
            reentrantCount.remove(key);
            return true;
        }

        boolean isLocked(String key) { return store.containsKey(key); }
    }

    /** 模拟 ZooKeeper 分布式锁（临时顺序节点） */
    static class SimulatedZKLock {
        private final java.util.TreeMap<Integer, String> nodes = new java.util.TreeMap<>();
        private int sequence = 0;

        /** 创建临时顺序节点 */
        int createNode(String owner) {
            int seq = ++sequence;
            nodes.put(seq, owner);
            return seq;
        }

        /** 检查是否获得锁（序号最小的获得锁） */
        boolean isLockHolder(int seq) {
            return !nodes.isEmpty() && nodes.firstKey() == seq;
        }

        /** 获取前一个节点（用于 Watcher 监听） */
        Integer getPreviousNode(int seq) {
            Integer lower = nodes.lowerKey(seq);
            return lower;
        }

        /** 释放锁（删除节点） */
        void deleteNode(int seq) {
            nodes.remove(seq);
        }

        int nodeCount() { return nodes.size(); }
    }

    /** 模拟数据库分布式锁 */
    static class SimulatedDBLock {
        private final java.util.concurrent.ConcurrentHashMap<String, String> lockTable =
                new java.util.concurrent.ConcurrentHashMap<>();

        /** SELECT ... FOR UPDATE（悲观锁） */
        boolean lock(String lockName, String owner) {
            return lockTable.putIfAbsent(lockName, owner) == null;
        }

        boolean unlock(String lockName, String owner) {
            return lockTable.remove(lockName, owner);
        }
    }

    // ==================== Part A 演示方法 ====================

    static void demoRedisLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Redis 分布式锁（SET NX EX）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedRedisLock redis = new SimulatedRedisLock();

        // 基本加锁/释放
        System.out.println("\n  【基本使用】");
        boolean locked = redis.lock("order:1001", "thread-A", 30);
        System.out.printf("    thread-A 加锁: %s%n", locked ? "✓ 成功" : "✗ 失败");

        boolean locked2 = redis.lock("order:1001", "thread-B", 30);
        System.out.printf("    thread-B 加锁: %s（被 thread-A 持有）%n", locked2 ? "✓ 成功" : "✗ 失败");

        redis.unlock("order:1001", "thread-A");
        System.out.println("    thread-A 释放锁");

        boolean locked3 = redis.lock("order:1001", "thread-B", 30);
        System.out.printf("    thread-B 再次加锁: %s%n", locked3 ? "✓ 成功" : "✗ 失败");
        redis.unlock("order:1001", "thread-B");

        // 可重入
        System.out.println("\n  【可重入锁】");
        redis.lock("order:1002", "thread-A", 30);
        boolean reentrant = redis.lock("order:1002", "thread-A", 30);
        System.out.printf("    thread-A 第二次加锁（可重入）: %s%n", reentrant ? "✓ 成功" : "✗ 失败");
        redis.unlock("order:1002", "thread-A"); // 重入计数 -1
        System.out.printf("    第一次 unlock 后仍持有锁: %s%n", redis.isLocked("order:1002"));
        redis.unlock("order:1002", "thread-A"); // 完全释放
        System.out.printf("    第二次 unlock 后释放: %s%n", !redis.isLocked("order:1002"));

        System.out.println("\n  Redis 锁核心命令：");
        System.out.println("    加锁: SET lock_key unique_id NX EX 30");
        System.out.println("    释放: Lua 脚本（GET + DEL 原子操作）");
        System.out.println("    续期: 看门狗线程每 10s 续期（Redisson 实现）");
        System.out.println();
    }

    static void demoZKLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：ZooKeeper 分布式锁（临时顺序节点）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedZKLock zk = new SimulatedZKLock();

        System.out.println("\n  三个客户端竞争锁：");
        int nodeA = zk.createNode("client-A");
        int nodeB = zk.createNode("client-B");
        int nodeC = zk.createNode("client-C");

        System.out.printf("    client-A 创建节点 seq=%d, 持有锁=%s%n", nodeA, zk.isLockHolder(nodeA));
        System.out.printf("    client-B 创建节点 seq=%d, 持有锁=%s, 监听节点=%d%n",
                nodeB, zk.isLockHolder(nodeB), zk.getPreviousNode(nodeB));
        System.out.printf("    client-C 创建节点 seq=%d, 持有锁=%s, 监听节点=%d%n",
                nodeC, zk.isLockHolder(nodeC), zk.getPreviousNode(nodeC));

        // client-A 释放锁
        System.out.println("\n    client-A 释放锁（删除节点）：");
        zk.deleteNode(nodeA);
        System.out.printf("    client-B 持有锁=%s（前一个节点被删除，获得锁）%n", zk.isLockHolder(nodeB));

        zk.deleteNode(nodeB);
        System.out.printf("    client-C 持有锁=%s%n", zk.isLockHolder(nodeC));
        zk.deleteNode(nodeC);
        System.out.println();
    }

    static void demoComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：三种方案对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"特性",     "Redis 锁",           "ZooKeeper 锁",        "数据库锁"},
                {"性能",     "★★★★★ 最高",        "★★★☆☆ 中等",         "★★☆☆☆ 最低"},
                {"可靠性",   "★★★★☆ 主从可能丢锁", "★★★★★ 最可靠",       "★★★☆☆ 死锁风险"},
                {"可重入",   "需自己实现/Redisson",  "Curator 内置",        "需自己实现"},
                {"续期",     "看门狗（Redisson）",   "会话保持",            "无"},
                {"公平性",   "非公平",              "公平（顺序节点）",      "非公平"},
                {"实现复杂度","中",                  "高（Curator 简化）",   "低"},
                {"推荐",     "✅ 大多数场景首选",    "✅ 高可靠场景",        "⚠️ 简单场景"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-10s %-22s %-22s %-18s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2], comparison[i][3]);
            if (i == 0) System.out.println("  " + "─".repeat(75));
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  分布式锁方案对比演示（混合模式）                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("══════════ Part A：模拟三种分布式锁 ══════════");
        System.out.println();
        demoRedisLock();
        demoZKLock();
        demoComparison();

        // Part B：连接真实 Redis，启动命令：docker compose -f docker/docker-compose.yml up -d redis
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：Jedis 真实 Redis 分布式锁 ══════════");
            System.out.println();
            RealRedisLock.run();
        }
    }

    // ==================== Part B：真实 Redis 分布式锁 ====================

    static class RealRedisLock {

        static final String REDIS_HOST = "localhost";
        static final int REDIS_PORT = 6379;

        static void run() throws Exception {
            redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis(REDIS_HOST, REDIS_PORT);

            try {
                String lockKey = "demo:distributed:lock";
                String requestId = java.util.UUID.randomUUID().toString();

                // 1. 加锁：SET NX EX
                System.out.println("  【加锁】SET lock_key requestId NX EX 30");
                String result = jedis.set(lockKey, requestId,
                        new redis.clients.jedis.params.SetParams().nx().ex(30));
                System.out.printf("    结果: %s%n", "OK".equals(result) ? "✓ 加锁成功" : "✗ 加锁失败");

                // 2. 验证锁
                System.out.printf("    GET %s = %s%n", lockKey, jedis.get(lockKey));
                System.out.printf("    TTL = %d 秒%n", jedis.ttl(lockKey));

                // 3. 其他线程尝试加锁（失败）
                String otherResult = jedis.set(lockKey, "other-thread",
                        new redis.clients.jedis.params.SetParams().nx().ex(30));
                System.out.printf("    其他线程加锁: %s%n", "OK".equals(otherResult) ? "✓ 成功" : "✗ 失败（已被持有）");

                // 4. 释放锁：Lua 脚本（原子操作）
                System.out.println("\n  【释放锁】Lua 脚本（验证 requestId 后删除）");
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) else return 0 end";
                Object unlockResult = jedis.eval(luaScript,
                        java.util.Collections.singletonList(lockKey),
                        java.util.Collections.singletonList(requestId));
                System.out.printf("    释放结果: %s%n", Long.valueOf(1).equals(unlockResult) ? "✓ 成功" : "✗ 失败");

                // 5. 并发竞争测试
                System.out.println("\n  【并发竞争】5 个线程同时抢锁：");
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(5);
                java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

                for (int i = 1; i <= 5; i++) {
                    final int threadId = i;
                    new Thread(() -> {
                        redis.clients.jedis.Jedis j = new redis.clients.jedis.Jedis(REDIS_HOST, REDIS_PORT);
                        String rid = "thread-" + threadId;
                        String r = j.set("demo:concurrent:lock", rid,
                                new redis.clients.jedis.params.SetParams().nx().ex(5));
                        boolean got = "OK".equals(r);
                        if (got) successCount.incrementAndGet();
                        System.out.printf("    thread-%d: %s%n", threadId, got ? "✓ 获得锁" : "✗ 未获得");
                        j.close();
                        latch.countDown();
                    }).start();
                }

                latch.await();
                System.out.printf("    获得锁的线程数: %d（应该只有 1 个）%n", successCount.get());

                // 清理
                jedis.del("demo:concurrent:lock");

            } finally {
                jedis.close();
                System.out.println("\n  提示：如需保留锁数据，注释掉 del 操作即可");
            }
        }
    }
}

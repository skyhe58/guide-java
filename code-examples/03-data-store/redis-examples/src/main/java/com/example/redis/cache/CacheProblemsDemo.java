package com.example.redis.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存穿透/击穿/雪崩解决方案代码示例
 *
 * <p>Part A：使用内存模拟缓存行为，重点展示解决方案的核心逻辑（无需 Redis）。</p>
 * <p>Part B：使用 Jedis 连接真实 Redis，演示缓存穿透/击穿/雪崩的真实场景。</p>
 * <p>Part B 启动命令：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
 *
 * <h3>三种缓存问题对比：</h3>
 * <ul>
 *   <li>穿透：查询不存在的数据 → 布隆过滤器 / 缓存空值</li>
 *   <li>击穿：热点 key 过期 → 互斥锁 / 逻辑过期</li>
 *   <li>雪崩：大量 key 同时过期 → 随机过期时间 / 多级缓存</li>
 * </ul>
 *
 * @see <a href="https://redis.io/docs/manual/patterns/">Redis Patterns</a>
 */
public class CacheProblemsDemo {

    // ==================== 模拟缓存和数据库 ====================

    /** 模拟缓存（实际使用 Redis） */
    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** 模拟数据库 */
    private static final Map<String, String> database = new ConcurrentHashMap<>();

    /** 缓存条目（包含值和逻辑过期时间） */
    record CacheEntry(String value, LocalDateTime expireTime) {
        boolean isExpired() {
            return expireTime != null && LocalDateTime.now().isAfter(expireTime);
        }
    }

    static {
        // 初始化模拟数据库
        database.put("user:1001", "{\"name\":\"张三\",\"age\":25}");
        database.put("user:1002", "{\"name\":\"李四\",\"age\":30}");
        database.put("user:1003", "{\"name\":\"王五\",\"age\":28}");
    }

    // ==================== 方案一：布隆过滤器（防缓存穿透） ====================

    /**
     * 使用布隆过滤器防止缓存穿透
     *
     * <p>布隆过滤器是一个概率型数据结构，用多个哈希函数将元素映射到 bit 数组：</p>
     * <ul>
     *   <li>判断不存在：100% 准确（不会漏判）</li>
     *   <li>判断存在：可能误判（假阳性，误判率可控）</li>
     * </ul>
     *
     * <p>使用 Guava 的 BloomFilter 实现，生产环境可使用 Redis 的 RedisBloom 模块。</p>
     */
    public static void bloomFilterDemo() {
        System.out.println("=== 方案一：布隆过滤器（防缓存穿透） ===");

        // 创建布隆过滤器：预期 100 万个元素，误判率 1%
        BloomFilter<Long> bloomFilter = BloomFilter.create(
                Funnels.longFunnel(),
                1_000_000,  // 预期元素数量
                0.01        // 误判率（1%）
        );

        // 预加载所有存在的 ID 到布隆过滤器
        for (long id = 1001; id <= 1003; id++) {
            bloomFilter.put(id);
        }

        // 测试：查询存在的 ID
        long existingId = 1001;
        boolean mightExist = bloomFilter.mightContain(existingId);
        System.out.println("ID " + existingId + " 可能存在: " + mightExist);  // true

        // 测试：查询不存在的 ID（恶意请求）
        long nonExistingId = 9999;
        boolean mightExist2 = bloomFilter.mightContain(nonExistingId);
        System.out.println("ID " + nonExistingId + " 可能存在: " + mightExist2);  // false（100% 准确）

        // 完整查询流程
        String result = queryWithBloomFilter(bloomFilter, existingId);
        System.out.println("查询 ID " + existingId + " 结果: " + result);

        String result2 = queryWithBloomFilter(bloomFilter, nonExistingId);
        System.out.println("查询 ID " + nonExistingId + " 结果: " + result2);
        System.out.println();
    }

    /**
     * 带布隆过滤器的查询流程
     *
     * @param bloomFilter 布隆过滤器
     * @param id          查询 ID
     * @return 查询结果，不存在返回 null
     */
    private static String queryWithBloomFilter(BloomFilter<Long> bloomFilter, long id) {
        // 1. 布隆过滤器判断
        if (!bloomFilter.mightContain(id)) {
            System.out.println("  布隆过滤器判断 ID " + id + " 不存在，直接返回");
            return null;  // 一定不存在，直接返回
        }

        // 2. 查缓存
        String key = "user:" + id;
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            System.out.println("  缓存命中: " + key);
            return entry.value();
        }

        // 3. 查数据库
        String dbResult = database.get(key);
        if (dbResult != null) {
            System.out.println("  数据库命中: " + key + "，写入缓存");
            cache.put(key, new CacheEntry(dbResult, LocalDateTime.now().plusHours(1)));
        } else {
            // 缓存空值（防止布隆过滤器误判后仍穿透）
            System.out.println("  数据库未命中: " + key + "，缓存空值");
            cache.put(key, new CacheEntry(null, LocalDateTime.now().plusMinutes(5)));
        }
        return dbResult;
    }

    // ==================== 方案二：互斥锁（防缓存击穿） ====================

    /** 模拟分布式锁（实际使用 Redis SETNX） */
    private static final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 使用互斥锁防止缓存击穿
     *
     * <p>当热点 key 过期时，只允许一个线程查询数据库并更新缓存，
     * 其他线程等待重试。保证强一致性，但可能阻塞。</p>
     */
    public static void mutexLockDemo() {
        System.out.println("=== 方案二：互斥锁（防缓存击穿） ===");

        // 模拟热点 key 过期
        String hotKey = "hot:product:1001";
        database.put(hotKey, "{\"name\":\"iPhone 15\",\"price\":7999}");

        // 模拟多线程并发请求
        System.out.println("模拟 3 个线程并发请求热点 key...");
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                String result = queryWithMutexLock(hotKey);
                System.out.println("  线程 " + threadId + " 获取结果: " + result);
            }, "Thread-" + i).start();
        }

        // 等待线程完成
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println();
    }

    /**
     * 带互斥锁的查询流程
     *
     * <p>核心逻辑：</p>
     * <ol>
     *   <li>查缓存，命中直接返回</li>
     *   <li>未命中，尝试获取锁</li>
     *   <li>获取锁成功：双重检查 → 查 DB → 写缓存 → 释放锁</li>
     *   <li>获取锁失败：等待重试</li>
     * </ol>
     */
    private static String queryWithMutexLock(String key) {
        // 1. 查缓存
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }

        // 2. 获取互斥锁
        ReentrantLock lock = locks.computeIfAbsent("lock:" + key, k -> new ReentrantLock());
        if (lock.tryLock()) {
            try {
                // 3. 双重检查（可能其他线程已经更新了缓存）
                entry = cache.get(key);
                if (entry != null && !entry.isExpired()) {
                    return entry.value();
                }

                // 4. 查数据库
                System.out.println("  [" + Thread.currentThread().getName() + "] 查询数据库: " + key);
                String dbResult = database.get(key);

                // 5. 写入缓存
                if (dbResult != null) {
                    cache.put(key, new CacheEntry(dbResult, LocalDateTime.now().plusHours(1)));
                }
                return dbResult;
            } finally {
                lock.unlock();
            }
        } else {
            // 6. 获取锁失败，等待重试
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return queryWithMutexLock(key);  // 递归重试
        }
    }

    // ==================== 方案三：逻辑过期（防缓存击穿） ====================

    /**
     * 使用逻辑过期防止缓存击穿
     *
     * <p>不设置 Redis 的 TTL，而是在 value 中存储逻辑过期时间。
     * 过期后异步更新缓存，当前请求返回旧数据。保证高可用，但短暂数据不一致。</p>
     */
    public static void logicalExpireDemo() {
        System.out.println("=== 方案三：逻辑过期（防缓存击穿） ===");

        String hotKey = "hot:ranking";
        String data = "{\"top1\":\"player-A\",\"top2\":\"player-B\"}";

        // 预热：写入缓存，设置逻辑过期时间为 1 秒前（模拟已过期）
        cache.put(hotKey, new CacheEntry(data, LocalDateTime.now().minusSeconds(1)));

        // 查询（逻辑过期后返回旧数据，异步更新）
        String result = queryWithLogicalExpire(hotKey);
        System.out.println("查询结果（可能是旧数据）: " + result);

        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println();
    }

    /**
     * 带逻辑过期的查询流程
     */
    private static String queryWithLogicalExpire(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // 未过期，直接返回
        if (!entry.isExpired()) {
            System.out.println("  缓存未过期，直接返回");
            return entry.value();
        }

        // 已过期，尝试获取锁异步更新
        ReentrantLock lock = locks.computeIfAbsent("lock:" + key, k -> new ReentrantLock());
        if (lock.tryLock()) {
            // 开启异步线程更新缓存
            new Thread(() -> {
                try {
                    System.out.println("  [异步线程] 更新缓存: " + key);
                    String dbResult = database.getOrDefault(key, entry.value());
                    cache.put(key, new CacheEntry(dbResult, LocalDateTime.now().plusHours(1)));
                } finally {
                    lock.unlock();
                }
            }).start();
        }

        // 返回旧数据（不等待）
        System.out.println("  缓存已逻辑过期，返回旧数据（异步更新中）");
        return entry.value();
    }

    // ==================== 方案四：随机过期时间（防缓存雪崩） ====================

    /**
     * 使用随机过期时间防止缓存雪崩
     *
     * <p>在基础过期时间上加一个随机值，避免大量 key 同时过期。</p>
     * <pre>
     * TTL = baseTime + random(0, maxRandomSeconds)
     * </pre>
     */
    public static void randomExpireDemo() {
        System.out.println("=== 方案四：随机过期时间（防缓存雪崩） ===");

        int baseExpireSeconds = 3600;  // 基础过期时间 1 小时
        int maxRandomSeconds = 300;    // 最大随机偏移 5 分钟

        // 模拟批量写入缓存
        for (int i = 1; i <= 5; i++) {
            int ttl = baseExpireSeconds + ThreadLocalRandom.current().nextInt(maxRandomSeconds);
            String key = "product:" + i;
            System.out.println("  " + key + " 过期时间: " + ttl + " 秒（" + ttl / 60 + " 分钟）");
            cache.put(key, new CacheEntry("data-" + i,
                    LocalDateTime.now().plusSeconds(ttl)));
        }
        System.out.println("  → 每个 key 的过期时间都不同，避免同时过期");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   缓存穿透/击穿/雪崩 — 解决方案代码示例          ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        bloomFilterDemo();
        mutexLockDemo();
        logicalExpireDemo();
        randomExpireDemo();

        System.out.println("=== 方案选择建议 ===");
        System.out.println("穿透 → 布隆过滤器（推荐）+ 缓存空值");
        System.out.println("击穿 → 互斥锁（强一致）或 逻辑过期（高可用）");
        System.out.println("雪崩 → 随机过期时间 + Redis 高可用 + 多级缓存 + 限流降级");

        // Part B：连接真实 Redis，需传入参数 'real'，启动命令：docker compose -f docker/docker-compose.yml up -d redis
        if (args.length > 0 && "real".equals(args[0])) {
            RealCacheProblems.run();
        }
    }

    // ==================== Part B：Jedis 真实 Redis 演示 ====================

    /**
     * Part B：使用 Jedis 连接真实 Redis，演示缓存穿透/击穿/雪崩的真实场景
     *
     * <p>启动 Redis：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
     * <p>运行方式：传入参数 {@code real}</p>
     */
    static class RealCacheProblems {

        /** 模拟数据库 */
        private static final Map<String, String> fakeDB = Map.of(
                "user:1001", "{\"name\":\"张三\",\"age\":25}",
                "user:1002", "{\"name\":\"李四\",\"age\":30}"
        );

        public static void run() {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║   Part B：Jedis 真实 Redis 缓存问题演示          ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println();

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                System.out.println("✅ 已连接 Redis: " + jedis.ping());
                System.out.println();

                demoCachePenetration(jedis);
                demoCacheNullValue(jedis);
                demoMutexLock(jedis);
                demoRandomExpire(jedis);

                System.out.println("=== Part B 演示完成 ===");
            }
        }

        /**
         * 演示缓存穿透：查询不存在的 key，请求直接打到"数据库"
         */
        private static void demoCachePenetration(Jedis jedis) {
            System.out.println("=== 缓存穿透演示 ===");

            String key = "cache:user:9999";  // 不存在的用户

            // 查询缓存
            String cached = jedis.get(key);
            System.out.println("查询缓存 " + key + ": " + cached);

            // 缓存未命中，查询"数据库"
            if (cached == null) {
                String dbResult = fakeDB.get("user:9999");
                System.out.println("查询数据库 user:9999: " + dbResult);
                System.out.println("→ 数据库也没有，请求穿透了！恶意请求会反复打到数据库");
            }

            // cleanup
            jedis.del(key);
            System.out.println();
        }

        /**
         * 演示缓存空值方案：SET key "" EX 300（缓存空值 5 分钟）
         */
        private static void demoCacheNullValue(Jedis jedis) {
            System.out.println("=== 缓存空值方案（防穿透） ===");

            String key = "cache:user:9999";

            // 查询数据库，结果为空
            String dbResult = fakeDB.get("user:9999");
            System.out.println("查询数据库 user:9999: " + dbResult);

            // 缓存空值，设置较短过期时间（5 分钟 = 300 秒）
            jedis.setex(key, 300, "");
            System.out.println("缓存空值: SET " + key + " \"\" EX 300");

            // 再次查询，命中缓存空值，不再穿透到数据库
            String cached = jedis.get(key);
            System.out.println("再次查询缓存: \"" + cached + "\"（空字符串，表示数据不存在）");
            System.out.println("→ 命中缓存空值，不再查询数据库，防止穿透");

            long ttl = jedis.ttl(key);
            System.out.println("剩余 TTL: " + ttl + " 秒");

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(key);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * 演示互斥锁方案：SET lock:key uuid NX EX 30（SETNX 加锁）
         */
        private static void demoMutexLock(Jedis jedis) {
            System.out.println("=== 互斥锁方案（防击穿） ===");

            String dataKey = "cache:hot:product";
            String lockKey = "lock:hot:product";
            String uuid = UUID.randomUUID().toString();

            // 模拟热点 key 过期（缓存中没有数据）
            jedis.del(dataKey);

            // 尝试获取互斥锁：SET lock:key uuid NX EX 30
            String lockResult = jedis.set(lockKey, uuid, new SetParams().nx().ex(30));
            System.out.println("获取互斥锁: SET " + lockKey + " " + uuid + " NX EX 30 → " + lockResult);

            if ("OK".equals(lockResult)) {
                // 获取锁成功，查询数据库并写入缓存
                String dbData = "{\"name\":\"iPhone 15\",\"price\":7999}";
                System.out.println("查询数据库: " + dbData);

                jedis.setex(dataKey, 3600, dbData);
                System.out.println("写入缓存: SET " + dataKey + " ... EX 3600");

                // 释放锁
                jedis.del(lockKey);
                System.out.println("释放互斥锁: DEL " + lockKey);
            }

            // 另一个"线程"尝试获取锁（应该失败，因为数据已缓存）
            String cached = jedis.get(dataKey);
            System.out.println("其他请求查询缓存: " + cached);
            System.out.println("→ 缓存命中，无需再查数据库");

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(dataKey, lockKey);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * 演示随机过期时间：SET key value EX (base + random)，防止缓存雪崩
         */
        private static void demoRandomExpire(Jedis jedis) {
            System.out.println("=== 随机过期时间（防雪崩） ===");

            int baseExpire = 3600;     // 基础过期时间 1 小时
            int maxRandom = 300;       // 最大随机偏移 5 分钟

            for (int i = 1; i <= 5; i++) {
                String key = "cache:product:" + i;
                int ttl = baseExpire + ThreadLocalRandom.current().nextInt(maxRandom);
                jedis.setex(key, ttl, "{\"id\":" + i + ",\"name\":\"商品" + i + "\"}");
                System.out.println("  SET " + key + " ... EX " + ttl + "（" + ttl / 60 + " 分钟）");
            }
            System.out.println("→ 每个 key 的过期时间都不同，避免同时过期导致雪崩");

            // cleanup（可以注释掉以保留数据观察）
            for (int i = 1; i <= 5; i++) {
                jedis.del("cache:product:" + i);
            }
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }
    }
}

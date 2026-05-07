package com.example.redis.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁实现示例
 *
 * <p>本示例演示 Redis 分布式锁的核心逻辑，包括：</p>
 * <ul>
 *   <li>SETNX + 过期时间加锁</li>
 *   <li>Lua 脚本原子释放锁（判断 owner 后删除）</li>
 *   <li>可重入锁实现（Hash 结构 + 计数器）</li>
 * </ul>
 *
 * <p>Part A：不依赖实际 Redis 连接，使用内存模拟 Redis 操作，
 * 重点展示分布式锁的核心算法逻辑。</p>
 * <p>Part B：使用 Jedis 连接真实 Redis，演示 SET NX EX 加锁和 Lua 脚本释放锁。</p>
 * <p>Part B 启动命令：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
 *
 * <h3>生产环境建议使用 Redisson：</h3>
 * <pre>
 * RLock lock = redissonClient.getLock("lock:order:1001");
 * lock.lock();  // 自动续期（看门狗机制）
 * try {
 *     // 业务逻辑
 * } finally {
 *     lock.unlock();
 * }
 * </pre>
 *
 * @see <a href="https://redis.io/docs/manual/patterns/distributed-locks/">Redis Distributed Locks</a>
 */
public class DistributedLockDemo {

    /**
     * 简单 Redis 分布式锁实现
     *
     * <p>模拟 Redis 的 SETNX + EX 加锁和 Lua 脚本释放锁。</p>
     */
    public static class SimpleRedisLock {

        /** 模拟 Redis 存储（实际使用 RedisTemplate） */
        private final Map<String, LockEntry> store = new ConcurrentHashMap<>();

        private static final String LOCK_PREFIX = "lock:";

        record LockEntry(String value, long expireAt) {
            boolean isExpired() {
                return System.currentTimeMillis() > expireAt;
            }
        }

        /**
         * 尝试加锁（模拟 SET key value NX EX seconds）
         *
         * <p>Redis 命令：{@code SET lock:key uuid NX EX 30}</p>
         * <ul>
         *   <li>NX — 仅当 key 不存在时设置（互斥）</li>
         *   <li>EX — 设置过期时间（防死锁）</li>
         *   <li>value 使用 UUID — 标识锁的持有者（防误删）</li>
         * </ul>
         *
         * @param key           锁的 key
         * @param value         锁的值（通常是 UUID，标识持有者）
         * @param expireSeconds 过期时间（秒）
         * @return true 加锁成功，false 加锁失败
         */
        public synchronized boolean tryLock(String key, String value, long expireSeconds) {
            String lockKey = LOCK_PREFIX + key;
            LockEntry existing = store.get(lockKey);

            // 检查是否已有未过期的锁
            if (existing != null && !existing.isExpired()) {
                return false;  // 锁已被持有
            }

            // 设置锁（模拟 SETNX + EX）
            long expireAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds);
            store.put(lockKey, new LockEntry(value, expireAt));
            return true;
        }

        /**
         * 释放锁（模拟 Lua 脚本：先判断 owner 再删除）
         *
         * <p>Lua 脚本（保证原子性）：</p>
         * <pre>
         * if redis.call('get', KEYS[1]) == ARGV[1] then
         *     return redis.call('del', KEYS[1])
         * else
         *     return 0
         * end
         * </pre>
         *
         * <p>为什么要用 Lua 脚本？因为 GET + DEL 不是原子操作，
         * 在 GET 和 DEL 之间锁可能过期被其他线程获取，
         * 导致误删别人的锁。</p>
         *
         * @param key   锁的 key
         * @param value 锁的值（必须与加锁时一致）
         * @return true 释放成功，false 释放失败（不是自己的锁）
         */
        public synchronized boolean unlock(String key, String value) {
            String lockKey = LOCK_PREFIX + key;
            LockEntry existing = store.get(lockKey);

            // 模拟 Lua 脚本：判断是否是自己的锁
            if (existing != null && Objects.equals(existing.value(), value)) {
                store.remove(lockKey);
                return true;  // 释放成功
            }
            return false;  // 不是自己的锁，释放失败
        }

        /**
         * 检查锁是否被持有（用于测试）
         */
        public boolean isLocked(String key) {
            LockEntry entry = store.get(LOCK_PREFIX + key);
            return entry != null && !entry.isExpired();
        }

        /**
         * 获取锁的持有者（用于测试）
         */
        public String getLockOwner(String key) {
            LockEntry entry = store.get(LOCK_PREFIX + key);
            if (entry != null && !entry.isExpired()) {
                return entry.value();
            }
            return null;
        }
    }

    /**
     * 可重入分布式锁实现
     *
     * <p>使用 Hash 结构实现可重入：</p>
     * <pre>
     * HSET lock:key "uuid:threadId" 1    # 首次加锁，计数 1
     * HINCRBY lock:key "uuid:threadId" 1 # 重入，计数 +1
     * HINCRBY lock:key "uuid:threadId" -1 # 释放，计数 -1
     * # 计数为 0 时删除 key
     * </pre>
     *
     * <p>这是 Redisson 可重入锁的简化版实现。</p>
     */
    public static class ReentrantRedisLock {

        /** 模拟 Redis Hash 存储 */
        private final Map<String, Map<String, Integer>> store = new ConcurrentHashMap<>();
        private final Map<String, Long> expireMap = new ConcurrentHashMap<>();

        private static final String LOCK_PREFIX = "lock:";

        /**
         * 加锁（支持重入）
         *
         * @param key           锁的 key
         * @param owner         锁的持有者标识（uuid:threadId）
         * @param expireSeconds 过期时间
         * @return true 加锁成功
         */
        public synchronized boolean tryLock(String key, String owner, long expireSeconds) {
            String lockKey = LOCK_PREFIX + key;
            long now = System.currentTimeMillis();

            // 检查锁是否过期
            Long expireAt = expireMap.get(lockKey);
            if (expireAt != null && now > expireAt) {
                // 锁已过期，清理
                store.remove(lockKey);
                expireMap.remove(lockKey);
            }

            Map<String, Integer> lockMap = store.get(lockKey);

            if (lockMap == null || lockMap.isEmpty()) {
                // 锁不存在，首次加锁
                lockMap = new ConcurrentHashMap<>();
                lockMap.put(owner, 1);
                store.put(lockKey, lockMap);
                expireMap.put(lockKey, now + TimeUnit.SECONDS.toMillis(expireSeconds));
                return true;
            }

            if (lockMap.containsKey(owner)) {
                // 重入：计数 +1
                lockMap.merge(owner, 1, Integer::sum);
                // 续期
                expireMap.put(lockKey, now + TimeUnit.SECONDS.toMillis(expireSeconds));
                return true;
            }

            // 锁被其他人持有
            return false;
        }

        /**
         * 释放锁（支持重入）
         *
         * @param key   锁的 key
         * @param owner 锁的持有者标识
         * @return true 释放成功
         */
        public synchronized boolean unlock(String key, String owner) {
            String lockKey = LOCK_PREFIX + key;
            Map<String, Integer> lockMap = store.get(lockKey);

            if (lockMap == null || !lockMap.containsKey(owner)) {
                return false;  // 不是自己的锁
            }

            int count = lockMap.get(owner) - 1;
            if (count <= 0) {
                // 计数为 0，完全释放
                lockMap.remove(owner);
                if (lockMap.isEmpty()) {
                    store.remove(lockKey);
                    expireMap.remove(lockKey);
                }
            } else {
                // 计数 -1，仍持有锁
                lockMap.put(owner, count);
            }
            return true;
        }

        /**
         * 获取重入次数（用于测试）
         */
        public int getHoldCount(String key, String owner) {
            Map<String, Integer> lockMap = store.get(LOCK_PREFIX + key);
            if (lockMap == null) return 0;
            return lockMap.getOrDefault(owner, 0);
        }
    }

    // ==================== 演示方法 ====================

    public static void simpleRedisLockDemo() {
        System.out.println("=== 简单分布式锁演示 ===");

        SimpleRedisLock lock = new SimpleRedisLock();
        String lockKey = "order:1001";
        String owner1 = UUID.randomUUID().toString();
        String owner2 = UUID.randomUUID().toString();

        // 线程1 加锁
        boolean locked1 = lock.tryLock(lockKey, owner1, 30);
        System.out.println("线程1 加锁: " + (locked1 ? "成功" : "失败"));

        // 线程2 尝试加锁（应该失败）
        boolean locked2 = lock.tryLock(lockKey, owner2, 30);
        System.out.println("线程2 加锁: " + (locked2 ? "成功" : "失败（锁已被持有）"));

        // 线程2 尝试释放线程1 的锁（应该失败）
        boolean unlocked2 = lock.unlock(lockKey, owner2);
        System.out.println("线程2 释放锁: " + (unlocked2 ? "成功" : "失败（不是自己的锁）"));

        // 线程1 释放锁
        boolean unlocked1 = lock.unlock(lockKey, owner1);
        System.out.println("线程1 释放锁: " + (unlocked1 ? "成功" : "失败"));

        // 线程2 再次尝试加锁（应该成功）
        boolean locked2Again = lock.tryLock(lockKey, owner2, 30);
        System.out.println("线程2 再次加锁: " + (locked2Again ? "成功" : "失败"));
        lock.unlock(lockKey, owner2);
        System.out.println();
    }

    public static void reentrantLockDemo() {
        System.out.println("=== 可重入分布式锁演示 ===");

        ReentrantRedisLock lock = new ReentrantRedisLock();
        String lockKey = "inventory:2001";
        String owner = "uuid-123:thread-1";

        // 首次加锁
        boolean locked1 = lock.tryLock(lockKey, owner, 30);
        System.out.println("首次加锁: " + locked1 + "，重入次数: " + lock.getHoldCount(lockKey, owner));

        // 重入加锁
        boolean locked2 = lock.tryLock(lockKey, owner, 30);
        System.out.println("重入加锁: " + locked2 + "，重入次数: " + lock.getHoldCount(lockKey, owner));

        // 第一次释放（计数 -1）
        lock.unlock(lockKey, owner);
        System.out.println("第一次释放，重入次数: " + lock.getHoldCount(lockKey, owner));

        // 第二次释放（完全释放）
        lock.unlock(lockKey, owner);
        System.out.println("第二次释放，重入次数: " + lock.getHoldCount(lockKey, owner));
        System.out.println();
    }

    public static void luaScriptExplanation() {
        System.out.println("=== Lua 脚本说明 ===");
        System.out.println();
        System.out.println("加锁 Lua 脚本（可重入版）：");
        System.out.println("""
                if redis.call('exists', KEYS[1]) == 0 then
                    redis.call('hset', KEYS[1], ARGV[1], 1)
                    redis.call('pexpire', KEYS[1], ARGV[2])
                    return 1
                elseif redis.call('hexists', KEYS[1], ARGV[1]) == 1 then
                    redis.call('hincrby', KEYS[1], ARGV[1], 1)
                    redis.call('pexpire', KEYS[1], ARGV[2])
                    return 1
                else
                    return 0
                end
                """);

        System.out.println("释放锁 Lua 脚本（可重入版）：");
        System.out.println("""
                if redis.call('hexists', KEYS[1], ARGV[1]) == 0 then
                    return 0
                end
                local count = redis.call('hincrby', KEYS[1], ARGV[1], -1)
                if count > 0 then
                    redis.call('pexpire', KEYS[1], ARGV[2])
                    return 1
                else
                    redis.call('del', KEYS[1])
                    return 1
                end
                """);
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Redis 分布式锁实现 — 核心逻辑演示          ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        simpleRedisLockDemo();
        reentrantLockDemo();
        luaScriptExplanation();

        System.out.println("=== 生产环境建议 ===");
        System.out.println("1. 使用 Redisson 框架（看门狗自动续期、可重入、公平锁）");
        System.out.println("2. 对一致性要求极高的场景考虑 ZooKeeper 分布式锁");
        System.out.println("3. RedLock 算法适用于多 Redis 实例的高可用场景");

        // Part B：连接真实 Redis，需传入参数 'real'，启动命令：docker compose -f docker/docker-compose.yml up -d redis
        if (args.length > 0 && "real".equals(args[0])) {
            RealDistributedLock.run();
        }
    }

    // ==================== Part B：Jedis 真实 Redis 分布式锁 ====================

    /**
     * Part B：使用 Jedis 连接真实 Redis，演示 SET NX EX 分布式锁
     *
     * <p>启动 Redis：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
     * <p>运行方式：传入参数 {@code real}</p>
     */
    static class RealDistributedLock {

        /** Lua 释放锁脚本：判断 owner 后删除 */
        private static final String UNLOCK_LUA_SCRIPT =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";

        public static void run() {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║   Part B：Jedis 真实 Redis 分布式锁演示          ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println();

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                System.out.println("✅ 已连接 Redis: " + jedis.ping());
                System.out.println();

                demoLockAndUnlock(jedis);
                demoLuaUnlock(jedis);
                demoLockContention();
                demoLockExpiry();

                System.out.println("=== Part B 演示完成 ===");
            }
        }

        /**
         * 演示 SET key uuid NX EX 30 加锁
         */
        private static void demoLockAndUnlock(Jedis jedis) {
            System.out.println("=== SET NX EX 加锁演示 ===");

            String lockKey = "lock:order:2001";
            String uuid = UUID.randomUUID().toString();

            // 加锁：SET lock:key uuid NX EX 30
            String result = jedis.set(lockKey, uuid, new SetParams().nx().ex(30));
            System.out.println("加锁: SET " + lockKey + " " + uuid + " NX EX 30 → " + result);

            // 查看锁状态
            String lockValue = jedis.get(lockKey);
            System.out.println("锁的值: " + lockValue);
            long ttl = jedis.ttl(lockKey);
            System.out.println("锁的 TTL: " + ttl + " 秒");

            // 再次加锁（应该失败，NX 保证互斥）
            String result2 = jedis.set(lockKey, "another-uuid", new SetParams().nx().ex(30));
            System.out.println("再次加锁（不同 uuid）: " + result2 + "（null 表示失败，锁已存在）");

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(lockKey);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * 演示 Lua 脚本释放锁（判断 owner 后删除）
         */
        private static void demoLuaUnlock(Jedis jedis) {
            System.out.println("=== Lua 脚本释放锁演示 ===");

            String lockKey = "lock:order:2002";
            String ownerUuid = UUID.randomUUID().toString();
            String otherUuid = UUID.randomUUID().toString();

            // 加锁
            jedis.set(lockKey, ownerUuid, new SetParams().nx().ex(30));
            System.out.println("加锁成功，owner: " + ownerUuid);

            // 用错误的 uuid 释放（应该失败）
            Object wrongResult = jedis.eval(UNLOCK_LUA_SCRIPT,
                    Collections.singletonList(lockKey),
                    Collections.singletonList(otherUuid));
            System.out.println("用错误 uuid 释放: " + wrongResult + "（0 表示失败，不是自己的锁）");

            // 锁仍然存在
            System.out.println("锁仍然存在: " + jedis.get(lockKey));

            // 用正确的 uuid 释放（应该成功）
            Object correctResult = jedis.eval(UNLOCK_LUA_SCRIPT,
                    Collections.singletonList(lockKey),
                    Collections.singletonList(ownerUuid));
            System.out.println("用正确 uuid 释放: " + correctResult + "（1 表示成功）");

            // 锁已删除
            System.out.println("锁已删除: " + jedis.get(lockKey));

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(lockKey);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * 演示锁竞争：两个线程争抢同一把锁
         */
        private static void demoLockContention() {
            System.out.println("=== 锁竞争演示 ===");

            String lockKey = "lock:order:2003";
            CountDownLatch latch = new CountDownLatch(2);

            for (int i = 1; i <= 2; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try (Jedis threadJedis = new Jedis("localhost", 6379)) {
                        String uuid = UUID.randomUUID().toString();
                        String result = threadJedis.set(lockKey, uuid, new SetParams().nx().ex(30));

                        if ("OK".equals(result)) {
                            System.out.println("  线程" + threadId + " 获取锁成功，uuid: " + uuid);
                            // 模拟业务处理
                            Thread.sleep(100);
                            // Lua 脚本释放锁
                            threadJedis.eval(UNLOCK_LUA_SCRIPT,
                                    Collections.singletonList(lockKey),
                                    Collections.singletonList(uuid));
                            System.out.println("  线程" + threadId + " 释放锁成功");
                        } else {
                            System.out.println("  线程" + threadId + " 获取锁失败（锁已被其他线程持有）");
                        }
                    } catch (Exception e) {
                        System.out.println("  线程" + threadId + " 异常: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }, "LockThread-" + i).start();
            }

            try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // cleanup（可以注释掉以保留数据观察）
            try (Jedis cleanJedis = new Jedis("localhost", 6379)) {
                cleanJedis.del(lockKey);
            }
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * 演示锁过期：设置短过期时间，等待过期后另一个线程获取
         */
        private static void demoLockExpiry() {
            System.out.println("=== 锁过期演示 ===");

            String lockKey = "lock:order:2004";

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                String uuid1 = UUID.randomUUID().toString();

                // 线程1 加锁，过期时间 2 秒
                String result1 = jedis.set(lockKey, uuid1, new SetParams().nx().ex(2));
                System.out.println("线程1 加锁（EX 2 秒）: " + result1 + "，uuid: " + uuid1);

                // 等待锁过期
                System.out.println("等待 3 秒，让锁过期...");
                Thread.sleep(3000);

                // 检查锁是否已过期
                String expired = jedis.get(lockKey);
                System.out.println("锁过期后查询: " + expired + "（null 表示已过期）");

                // 线程2 获取锁（应该成功）
                String uuid2 = UUID.randomUUID().toString();
                String result2 = jedis.set(lockKey, uuid2, new SetParams().nx().ex(30));
                System.out.println("线程2 加锁: " + result2 + "（锁过期后可以重新获取）");

                // cleanup（可以注释掉以保留数据观察）
                jedis.del(lockKey);
                System.out.println("[cleanup] 已清理测试数据");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println();
        }
    }
}

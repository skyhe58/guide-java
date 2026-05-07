package com.example.redis.spring;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.resps.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 与 Spring Boot 集成示例
 *
 * <p>Part A：演示 RedisTemplate 的配置和常用操作（需 Spring Boot 上下文）。</p>
 * <p>Part B：使用 Jedis 直接连接 Redis，演示五种数据类型的真实操作。</p>
 * <p>Part B 启动命令：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
 * <ul>
 *   <li>RedisTemplate 自定义序列化配置</li>
 *   <li>StringRedisTemplate 基本操作</li>
 *   <li>五种数据类型的 RedisTemplate 操作</li>
 *   <li>Spring Cache + Redis 注解使用</li>
 * </ul>
 *
 * <h3>Lettuce vs Jedis：</h3>
 * <table>
 *   <tr><th>特性</th><th>Lettuce（默认）</th><th>Jedis</th></tr>
 *   <tr><td>线程安全</td><td>✅ 基于 Netty</td><td>❌ 需连接池</td></tr>
 *   <tr><td>异步支持</td><td>✅</td><td>❌</td></tr>
 *   <tr><td>连接模型</td><td>单连接多线程共享</td><td>每线程一个连接</td></tr>
 * </table>
 *
 * <p>⚠️ 需要 Redis 环境：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
 *
 * @see <a href="https://docs.spring.io/spring-data/redis/docs/current/reference/html/">Spring Data Redis</a>
 */
public class RedisIntegrationDemo {

    // ==================== 1. RedisTemplate 配置 ====================

    /**
     * 自定义 RedisTemplate 配置（推荐）
     *
     * <p>默认的 RedisTemplate 使用 JDK 序列化，存储的是二进制数据，不可读。
     * 推荐自定义序列化器：key 用 String，value 用 JSON。</p>
     *
     * <p>在 Spring Boot 中，将此方法放在 @Configuration 类中，添加 @Bean 注解：</p>
     * <pre>
     * {@code @Configuration}
     * public class RedisConfig {
     *     {@code @Bean}
     *     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
     *         return RedisIntegrationDemo.createRedisTemplate(factory);
     *     }
     * }
     * </pre>
     */
    public static RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 序列化：StringRedisSerializer（可读的字符串）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化：GenericJackson2JsonRedisSerializer（JSON 格式，自动类型推断）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // ==================== 2. 常用操作示例 ====================

    /**
     * 演示 StringRedisTemplate 的常用操作
     *
     * <p>StringRedisTemplate 是 RedisTemplate&lt;String, String&gt; 的子类，
     * 默认使用 StringRedisSerializer，存储的数据在 Redis 中是可读的字符串。</p>
     */
    public static void stringRedisTemplateOps(StringRedisTemplate stringRedisTemplate) {
        // ---- String 操作 ----
        // SET key value EX seconds
        stringRedisTemplate.opsForValue().set("user:name:1001", "张三", 3600, TimeUnit.SECONDS);

        // GET key
        String name = stringRedisTemplate.opsForValue().get("user:name:1001");

        // INCR key（原子递增，适合计数器）
        stringRedisTemplate.opsForValue().increment("article:views:1001");

        // SETNX（分布式锁）
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent("lock:order:1001", "uuid-xxx", 30, TimeUnit.SECONDS);

        // ---- Hash 操作 ----
        stringRedisTemplate.opsForHash().put("user:1001", "name", "张三");
        stringRedisTemplate.opsForHash().put("user:1001", "age", "25");
        Object age = stringRedisTemplate.opsForHash().get("user:1001", "age");

        // ---- List 操作 ----
        stringRedisTemplate.opsForList().leftPush("news:latest", "新闻标题1");
        stringRedisTemplate.opsForList().range("news:latest", 0, 9);

        // ---- Set 操作 ----
        stringRedisTemplate.opsForSet().add("user:1001:tags", "Java", "Redis", "Spring");
        stringRedisTemplate.opsForSet().members("user:1001:tags");

        // ---- ZSet 操作 ----
        stringRedisTemplate.opsForZSet().add("leaderboard", "player1", 100);
        stringRedisTemplate.opsForZSet().reverseRangeWithScores("leaderboard", 0, 9);
    }

    /**
     * 演示 Spring Cache 注解的使用方式
     *
     * <p>Spring Cache 通过 AOP 代理实现声明式缓存，常用注解：</p>
     * <ul>
     *   <li>{@code @Cacheable} — 查询缓存，不存在则执行方法并缓存结果</li>
     *   <li>{@code @CachePut} — 执行方法并更新缓存</li>
     *   <li>{@code @CacheEvict} — 删除缓存</li>
     * </ul>
     *
     * <p>使用示例（需要在 Service 类中）：</p>
     * <pre>
     * {@code @Service}
     * public class UserService {
     *
     *     {@code @Cacheable(value = "user", key = "#id", unless = "#result == null")}
     *     public User findById(Long id) {
     *         return userMapper.selectById(id);
     *     }
     *
     *     {@code @CachePut(value = "user", key = "#user.id")}
     *     public User update(User user) {
     *         userMapper.updateById(user);
     *         return user;
     *     }
     *
     *     {@code @CacheEvict(value = "user", key = "#id")}
     *     public void delete(Long id) {
     *         userMapper.deleteById(id);
     *     }
     * }
     * </pre>
     *
     * <p>Spring Cache 的局限：</p>
     * <ul>
     *   <li>不支持单个 key 设置不同过期时间</li>
     *   <li>同类内部方法调用不走代理（AOP 失效）</li>
     *   <li>不支持缓存穿透/击穿防护</li>
     *   <li>复杂场景建议直接使用 RedisTemplate</li>
     * </ul>
     */
    public static void springCacheExample() {
        // Spring Cache 是声明式的，通过注解使用
        // 这里仅展示配置和说明，实际使用需要在 Spring 容器中
        System.out.println("Spring Cache + Redis 配置说明：");
        System.out.println();
        System.out.println("1. 添加依赖：spring-boot-starter-data-redis");
        System.out.println("2. 启用缓存：@EnableCaching");
        System.out.println("3. application.yml 配置：");
        System.out.println("""
                   spring:
                     data:
                       redis:
                         host: localhost
                         port: 6379
                         lettuce:
                           pool:
                             max-active: 8
                             max-idle: 8
                             min-idle: 2
                     cache:
                       type: redis
                       redis:
                         time-to-live: 3600000
                         key-prefix: "app:"
                         cache-null-values: true
                """);
        System.out.println("4. 在 Service 方法上使用 @Cacheable/@CachePut/@CacheEvict");
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Redis 与 Spring Boot 集成 — 配置与使用     ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        springCacheExample();

        System.out.println("=== RedisTemplate 操作说明 ===");
        System.out.println("opsForValue() — String 操作（SET/GET/INCR/SETNX）");
        System.out.println("opsForHash()  — Hash 操作（HSET/HGET/HGETALL）");
        System.out.println("opsForList()  — List 操作（LPUSH/RPOP/LRANGE）");
        System.out.println("opsForSet()   — Set 操作（SADD/SMEMBERS/SINTER）");
        System.out.println("opsForZSet()  — ZSet 操作（ZADD/ZRANGE/ZREVRANGE）");
        System.out.println();
        System.out.println("⚠️ 实际运行需要 Redis 环境和 Spring Boot 上下文：");
        System.out.println("   docker compose -f docker/docker-compose.yml up -d redis");

        // Part B：连接真实 Redis，需传入参数 'real'，启动命令：docker compose -f docker/docker-compose.yml up -d redis
        if (args.length > 0 && "real".equals(args[0])) {
            RealRedisOps.run();
        }
    }

    // ==================== Part B：Jedis 真实 Redis 操作 ====================

    /**
     * Part B：使用 Jedis 连接真实 Redis，演示五种数据类型操作 + Pipeline
     *
     * <p>启动 Redis：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
     * <p>运行方式：传入参数 {@code real}</p>
     */
    static class RealRedisOps {

        public static void run() {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║   Part B：Jedis 真实 Redis 五种数据类型操作       ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println();

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                System.out.println("✅ 已连接 Redis: " + jedis.ping());
                System.out.println();

                demoString(jedis);
                demoHash(jedis);
                demoList(jedis);
                demoSet(jedis);
                demoZSet(jedis);
                demoPipeline(jedis);

                System.out.println("=== Part B 演示完成 ===");
            }
        }

        /**
         * String 操作：SET/GET/INCR/SETNX/MSET/MGET
         */
        private static void demoString(Jedis jedis) {
            System.out.println("=== String 操作 ===");

            // SET / GET
            jedis.setex("str:name", 3600, "张三");
            System.out.println("SET str:name 张三 EX 3600");
            System.out.println("GET str:name → " + jedis.get("str:name"));

            // INCR（原子递增）
            jedis.set("str:counter", "0");
            jedis.incr("str:counter");
            jedis.incr("str:counter");
            jedis.incr("str:counter");
            System.out.println("INCR str:counter ×3 → " + jedis.get("str:counter"));

            // SETNX（仅当 key 不存在时设置）
            long setnx1 = jedis.setnx("str:lock", "uuid-123");
            long setnx2 = jedis.setnx("str:lock", "uuid-456");
            System.out.println("SETNX str:lock uuid-123 → " + setnx1 + "（1=成功）");
            System.out.println("SETNX str:lock uuid-456 → " + setnx2 + "（0=失败，key 已存在）");

            // MSET / MGET（批量操作）
            jedis.mset("str:a", "1", "str:b", "2", "str:c", "3");
            List<String> mgetResult = jedis.mget("str:a", "str:b", "str:c");
            System.out.println("MSET str:a=1, str:b=2, str:c=3");
            System.out.println("MGET str:a str:b str:c → " + mgetResult);

            // cleanup（可以注释掉以保留数据观察）
            jedis.del("str:name", "str:counter", "str:lock", "str:a", "str:b", "str:c");
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * Hash 操作：HSET/HGET/HGETALL/HINCRBY
         */
        private static void demoHash(Jedis jedis) {
            System.out.println("=== Hash 操作 ===");

            String key = "hash:user:1001";

            // HSET
            jedis.hset(key, "name", "张三");
            jedis.hset(key, "age", "25");
            jedis.hset(key, "city", "北京");
            System.out.println("HSET " + key + " name=张三, age=25, city=北京");

            // HGET
            String name = jedis.hget(key, "name");
            System.out.println("HGET " + key + " name → " + name);

            // HGETALL
            Map<String, String> all = jedis.hgetAll(key);
            System.out.println("HGETALL " + key + " → " + all);

            // HINCRBY（原子递增 Hash 字段）
            long newAge = jedis.hincrBy(key, "age", 1);
            System.out.println("HINCRBY " + key + " age 1 → " + newAge);

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(key);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * List 操作：LPUSH/RPOP/LRANGE/LLEN
         */
        private static void demoList(Jedis jedis) {
            System.out.println("=== List 操作 ===");

            String key = "list:queue";

            // LPUSH（左侧插入）
            jedis.lpush(key, "消息1", "消息2", "消息3");
            System.out.println("LPUSH " + key + " 消息1 消息2 消息3");

            // LLEN
            long len = jedis.llen(key);
            System.out.println("LLEN " + key + " → " + len);

            // LRANGE（获取范围元素）
            List<String> range = jedis.lrange(key, 0, -1);
            System.out.println("LRANGE " + key + " 0 -1 → " + range);

            // RPOP（右侧弹出，模拟队列消费）
            String popped = jedis.rpop(key);
            System.out.println("RPOP " + key + " → " + popped);

            System.out.println("剩余元素: " + jedis.lrange(key, 0, -1));

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(key);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * Set 操作：SADD/SMEMBERS/SINTER/SUNION
         */
        private static void demoSet(Jedis jedis) {
            System.out.println("=== Set 操作 ===");

            String key1 = "set:user:1001:tags";
            String key2 = "set:user:1002:tags";

            // SADD
            jedis.sadd(key1, "Java", "Redis", "Spring", "MySQL");
            jedis.sadd(key2, "Python", "Redis", "Docker", "MySQL");
            System.out.println("SADD " + key1 + " Java Redis Spring MySQL");
            System.out.println("SADD " + key2 + " Python Redis Docker MySQL");

            // SMEMBERS
            Set<String> members1 = jedis.smembers(key1);
            System.out.println("SMEMBERS " + key1 + " → " + members1);

            // SINTER（交集）
            Set<String> inter = jedis.sinter(key1, key2);
            System.out.println("SINTER " + key1 + " " + key2 + " → " + inter);

            // SUNION（并集）
            Set<String> union = jedis.sunion(key1, key2);
            System.out.println("SUNION " + key1 + " " + key2 + " → " + union);

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(key1, key2);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * ZSet 操作：ZADD/ZRANGE/ZREVRANGEBYSCORE/ZRANK
         */
        private static void demoZSet(Jedis jedis) {
            System.out.println("=== ZSet（有序集合）操作 ===");

            String key = "zset:leaderboard";

            // ZADD
            jedis.zadd(key, 100, "player-A");
            jedis.zadd(key, 250, "player-B");
            jedis.zadd(key, 180, "player-C");
            jedis.zadd(key, 320, "player-D");
            jedis.zadd(key, 90, "player-E");
            System.out.println("ZADD " + key + " player-A=100, B=250, C=180, D=320, E=90");

            // ZRANGE（按分数从低到高）
            List<String> range = jedis.zrange(key, 0, -1);
            System.out.println("ZRANGE " + key + " 0 -1 → " + range);

            // ZREVRANGEBYSCORE（按分数从高到低，范围查询）
            List<Tuple> topPlayers = jedis.zrevrangeByScoreWithScores(key, 400, 100);
            System.out.println("ZREVRANGEBYSCORE " + key + " 400 100（带分数）:");
            for (Tuple t : topPlayers) {
                System.out.println("  " + t.getElement() + " → " + (int) t.getScore() + " 分");
            }

            // ZRANK（获取排名，从 0 开始）
            Long rank = jedis.zrevrank(key, "player-D");
            System.out.println("ZREVRANK " + key + " player-D → 第 " + (rank + 1) + " 名");

            // cleanup（可以注释掉以保留数据观察）
            jedis.del(key);
            System.out.println("[cleanup] 已清理测试数据");
            System.out.println();
        }

        /**
         * Pipeline 批量操作演示
         */
        private static void demoPipeline(Jedis jedis) {
            System.out.println("=== Pipeline 批量操作 ===");

            // 使用 Pipeline 批量写入（减少网络往返）
            long start = System.currentTimeMillis();
            Pipeline pipeline = jedis.pipelined();
            for (int i = 1; i <= 100; i++) {
                pipeline.set("pipe:key:" + i, "value-" + i);
            }
            pipeline.sync();
            long writeTime = System.currentTimeMillis() - start;
            System.out.println("Pipeline 批量写入 100 个 key，耗时: " + writeTime + " ms");

            // 使用 Pipeline 批量读取
            start = System.currentTimeMillis();
            Pipeline readPipeline = jedis.pipelined();
            Response<String> first = readPipeline.get("pipe:key:1");
            Response<String> last = readPipeline.get("pipe:key:100");
            readPipeline.sync();
            long readTime = System.currentTimeMillis() - start;
            System.out.println("Pipeline 批量读取，耗时: " + readTime + " ms");
            System.out.println("  pipe:key:1 → " + first.get());
            System.out.println("  pipe:key:100 → " + last.get());

            // cleanup（可以注释掉以保留数据观察）
            Pipeline delPipeline = jedis.pipelined();
            for (int i = 1; i <= 100; i++) {
                delPipeline.del("pipe:key:" + i);
            }
            delPipeline.sync();
            System.out.println("[cleanup] 已清理 100 个 Pipeline 测试数据");
            System.out.println();
        }
    }
}

package com.example.springcloud.ratelimit;

import com.example.springcloud.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 10.A.24 分布式限流实战 Controller
 *
 * <p>演示三种经典限流算法的 Redis 实现：
 * <ul>
 *   <li>固定窗口限流（Redis INCR + EXPIRE）</li>
 *   <li>滑动窗口限流（Redis ZSET）</li>
 *   <li>令牌桶限流（Redis Lua 脚本）</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 固定窗口限流（key=api1，每 60 秒最多 10 次）
 * curl -X POST "http://localhost:8090/demo/ratelimit/fixed?key=api1&amp;limit=10&amp;window=60"
 *
 * # 滑动窗口限流
 * curl -X POST "http://localhost:8090/demo/ratelimit/sliding?key=api1&amp;limit=10&amp;window=60"
 *
 * # 令牌桶限流（每秒产生 10 个令牌，桶容量 20）
 * curl -X POST "http://localhost:8090/demo/ratelimit/token-bucket?key=api1&amp;rate=10&amp;capacity=20"
 *
 * # 限流方案对比
 * curl http://localhost:8090/demo/ratelimit/compare
 * </pre>
 */
@RestController
@RequestMapping("/demo/ratelimit")
public class RateLimitController {

    private static final Logger log = LoggerFactory.getLogger(RateLimitController.class);

    private final StringRedisTemplate redisTemplate;

    /** 令牌桶 Lua 脚本 */
    private static final String TOKEN_BUCKET_SCRIPT =
            "local key = KEYS[1] " +
            "local rate = tonumber(ARGV[1]) " +
            "local capacity = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local tokens_key = key .. ':tokens' " +
            "local timestamp_key = key .. ':ts' " +
            "local current_tokens = tonumber(redis.call('get', tokens_key) or capacity) " +
            "local last_refill = tonumber(redis.call('get', timestamp_key) or 0) " +
            "local elapsed = math.max(0, now - last_refill) " +
            "local new_tokens = math.min(capacity, current_tokens + elapsed * rate) " +
            "local allowed = 0 " +
            "if new_tokens >= 1 then " +
            "  new_tokens = new_tokens - 1 " +
            "  allowed = 1 " +
            "end " +
            "redis.call('set', tokens_key, tostring(new_tokens)) " +
            "redis.call('set', timestamp_key, tostring(now)) " +
            "redis.call('expire', tokens_key, math.ceil(capacity / rate) + 1) " +
            "redis.call('expire', timestamp_key, math.ceil(capacity / rate) + 1) " +
            "return {allowed, tostring(new_tokens)}";

    public RateLimitController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 固定窗口限流 — Redis INCR + EXPIRE
     *
     * @param key    限流 key
     * @param limit  窗口内最大请求数
     * @param window 窗口大小（秒）
     */
    @PostMapping("/fixed")
    public Result<Map<String, Object>> fixedWindow(
            @RequestParam String key,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "60") int window) {

        String redisKey = "ratelimit:fixed:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, window, TimeUnit.SECONDS);
        }

        boolean allowed = count != null && count <= limit;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        log.info("[RateLimit-Fixed] key={}, count={}, limit={}, allowed={}", key, count, limit, allowed);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("算法", "固定窗口");
        data.put("key", key);
        data.put("是否允许", allowed);
        data.put("当前计数", count);
        data.put("窗口限制", limit);
        data.put("窗口大小秒", window);
        data.put("窗口剩余秒", ttl);
        return Result.ok(data);
    }

    /**
     * 滑动窗口限流 — Redis ZSET（score=时间戳，member=UUID）
     *
     * @param key    限流 key
     * @param limit  窗口内最大请求数
     * @param window 窗口大小（秒）
     */
    @PostMapping("/sliding")
    public Result<Map<String, Object>> slidingWindow(
            @RequestParam String key,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "60") int window) {

        String redisKey = "ratelimit:sliding:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - window * 1000L;

        // 清理过期数据
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

        // 当前窗口内请求数
        Long count = redisTemplate.opsForZSet().zCard(redisKey);
        boolean allowed = count != null && count < limit;

        if (allowed) {
            // 添加当前请求
            redisTemplate.opsForZSet().add(redisKey, UUID.randomUUID().toString(), now);
            redisTemplate.expire(redisKey, window, TimeUnit.SECONDS);
            count = count + 1;
        }

        log.info("[RateLimit-Sliding] key={}, count={}, limit={}, allowed={}", key, count, limit, allowed);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("算法", "滑动窗口");
        data.put("key", key);
        data.put("是否允许", allowed);
        data.put("当前计数", count);
        data.put("窗口限制", limit);
        data.put("窗口大小秒", window);
        return Result.ok(data);
    }

    /**
     * 令牌桶限流 — Redis Lua 脚本
     *
     * @param key      限流 key
     * @param rate     令牌产生速率（个/秒）
     * @param capacity 桶容量
     */
    @PostMapping("/token-bucket")
    public Result<Map<String, Object>> tokenBucket(
            @RequestParam String key,
            @RequestParam(defaultValue = "10") int rate,
            @RequestParam(defaultValue = "20") int capacity) {

        String redisKey = "ratelimit:bucket:" + key;
        long nowSeconds = System.currentTimeMillis() / 1000;

        DefaultRedisScript<List> script = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, List.class);
        List<?> result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                String.valueOf(rate),
                String.valueOf(capacity),
                String.valueOf(nowSeconds));

        boolean allowed = false;
        String remainingTokens = "0";
        if (result != null && result.size() >= 2) {
            allowed = "1".equals(String.valueOf(result.get(0)));
            remainingTokens = String.valueOf(result.get(1));
        }

        log.info("[RateLimit-TokenBucket] key={}, allowed={}, remaining={}", key, allowed, remainingTokens);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("算法", "令牌桶");
        data.put("key", key);
        data.put("是否允许", allowed);
        data.put("剩余令牌", remainingTokens);
        data.put("令牌速率", rate + " 个/秒");
        data.put("桶容量", capacity);
        return Result.ok(data);
    }

    /**
     * 限流方案对比
     */
    @GetMapping("/compare")
    public Result<List<Map<String, Object>>> compare() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> fixed = new LinkedHashMap<>();
        fixed.put("方案", "固定窗口");
        fixed.put("实现方式", "Redis INCR + EXPIRE");
        fixed.put("优点", "实现简单，性能高，原子操作");
        fixed.put("缺点", "存在窗口边界突刺问题（两个窗口交界处可能通过 2 倍流量）");
        fixed.put("适用场景", "对精度要求不高的简单限流");
        list.add(fixed);

        Map<String, Object> sliding = new LinkedHashMap<>();
        sliding.put("方案", "滑动窗口");
        sliding.put("实现方式", "Redis ZSET（score=时间戳，member=UUID）");
        sliding.put("优点", "精度高，无窗口边界突刺问题");
        sliding.put("缺点", "内存占用较大（每个请求存一条记录），高并发下 ZSET 操作较重");
        sliding.put("适用场景", "需要精确限流的 API 接口");
        list.add(sliding);

        Map<String, Object> bucket = new LinkedHashMap<>();
        bucket.put("方案", "令牌桶");
        bucket.put("实现方式", "Redis Lua 脚本（存储 tokens + last_refill_time）");
        bucket.put("优点", "支持突发流量（桶中有令牌即可通过），限流平滑");
        bucket.put("缺点", "实现复杂，需要 Lua 脚本保证原子性");
        bucket.put("适用场景", "需要允许突发流量的场景，如开放 API");
        list.add(bucket);

        return Result.ok(list);
    }
}

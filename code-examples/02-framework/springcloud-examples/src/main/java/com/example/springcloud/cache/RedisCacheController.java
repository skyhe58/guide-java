package com.example.springcloud.cache;

import com.example.springcloud.common.Result;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 10.A.18 Redis 缓存实战 Controller
 *
 * <p>演示 Redis 缓存的核心功能：
 * <ul>
 *   <li>StringRedisTemplate 手动读写缓存</li>
 *   <li>@Cacheable / @CacheEvict 注解式缓存</li>
 *   <li>Redisson 分布式锁</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 写入缓存
 * curl -X POST "http://localhost:8090/demo/cache/set?key=name&amp;value=kiro&amp;ttl=60"
 *
 * # 读取缓存
 * curl http://localhost:8090/demo/cache/get/name
 *
 * # 删除缓存
 * curl -X DELETE http://localhost:8090/demo/cache/del/name
 *
 * # @Cacheable 查询用户（自动缓存）
 * curl http://localhost:8090/demo/cache/user/1
 *
 * # @CacheEvict 清除用户缓存
 * curl -X DELETE http://localhost:8090/demo/cache/user/1
 *
 * # 获取分布式锁
 * curl -X POST "http://localhost:8090/demo/cache/lock?key=order-lock&amp;ttl=10"
 *
 * # 释放分布式锁
 * curl -X DELETE http://localhost:8090/demo/cache/lock/order-lock
 * </pre>
 */
@RestController
@RequestMapping("/demo/cache")
public class RedisCacheController {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheController.class);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    public RedisCacheController(StringRedisTemplate redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    // ==================== 手动缓存操作 ====================

    /**
     * 读取缓存
     *
     * <pre>
     * curl http://localhost:8090/demo/cache/get/name
     * </pre>
     *
     * @param key 缓存 Key
     * @return 缓存值
     */
    @GetMapping("/get/{key}")
    public Result<Map<String, Object>> get(@PathVariable String key) {
        String value = redisTemplate.opsForValue().get(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("value", value);
        data.put("剩余TTL(秒)", ttl);
        data.put("是否存在", value != null);
        return Result.ok(data);
    }

    /**
     * 写入缓存
     *
     * <pre>
     * curl -X POST "http://localhost:8090/demo/cache/set?key=name&amp;value=kiro&amp;ttl=60"
     * </pre>
     *
     * @param key   缓存 Key
     * @param value 缓存 Value
     * @param ttl   过期时间（秒），可选，默认不过期
     * @return 写入结果
     */
    @PostMapping("/set")
    public Result<Map<String, Object>> set(@RequestParam String key,
                                           @RequestParam String value,
                                           @RequestParam(required = false) Long ttl) {
        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
        log.info("[Redis] 缓存写入: key={}, value={}, ttl={}", key, value, ttl);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "缓存写入成功");
        data.put("key", key);
        data.put("value", value);
        data.put("TTL(秒)", ttl != null ? ttl : "永不过期");
        return Result.ok(data);
    }

    /**
     * 删除缓存
     *
     * <pre>
     * curl -X DELETE http://localhost:8090/demo/cache/del/name
     * </pre>
     *
     * @param key 缓存 Key
     * @return 删除结果
     */
    @DeleteMapping("/del/{key}")
    public Result<Map<String, Object>> del(@PathVariable String key) {
        Boolean deleted = redisTemplate.delete(key);
        log.info("[Redis] 缓存删除: key={}, deleted={}", key, deleted);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "缓存删除操作完成");
        data.put("key", key);
        data.put("是否删除成功", Boolean.TRUE.equals(deleted));
        return Result.ok(data);
    }

    // ==================== @Cacheable 注解式缓存 ====================

    /**
     * 查询用户（@Cacheable 自动缓存）
     *
     * <p>首次查询会模拟数据库查询，后续请求直接从 Redis 缓存返回。
     *
     * <pre>
     * curl http://localhost:8090/demo/cache/user/1
     * </pre>
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/user/{id}")
    @Cacheable(cacheNames = "users", key = "#id")
    public Map<String, Object> getUser(@PathVariable Long id) {
        // 模拟数据库查询（只有缓存未命中时才会执行）
        log.info("[Redis] @Cacheable 缓存未命中，模拟查询数据库: userId={}", id);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("用户名", "user_" + id);
        user.put("邮箱", "user_" + id + "@example.com");
        user.put("查询时间", LocalDateTime.now().format(FMT));
        user.put("说明", "此数据来自模拟数据库查询，后续请求将从 Redis 缓存返回");
        return user;
    }

    /**
     * 清除用户缓存（@CacheEvict）
     *
     * <pre>
     * curl -X DELETE http://localhost:8090/demo/cache/user/1
     * </pre>
     *
     * @param id 用户 ID
     * @return 清除结果
     */
    @DeleteMapping("/user/{id}")
    @CacheEvict(cacheNames = "users", key = "#id")
    public Result<Map<String, Object>> evictUser(@PathVariable Long id) {
        log.info("[Redis] @CacheEvict 清除用户缓存: userId={}", id);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "用户缓存已清除");
        data.put("userId", id);
        data.put("cacheName", "users");
        data.put("操作时间", LocalDateTime.now().format(FMT));
        return Result.ok(data);
    }

    // ==================== Redisson 分布式锁 ====================

    /**
     * 获取分布式锁
     *
     * <p>尝试获取 Redisson 分布式锁，持有指定时间后自动释放。
     *
     * <pre>
     * curl -X POST "http://localhost:8090/demo/cache/lock?key=order-lock&amp;ttl=10"
     * </pre>
     *
     * @param key 锁的 Key
     * @param ttl 持有时间（秒），默认 10 秒
     * @return 加锁结果
     */
    @PostMapping("/lock")
    public Result<Map<String, Object>> lock(@RequestParam String key,
                                            @RequestParam(defaultValue = "10") long ttl) {
        String lockKey = "lock:" + key;
        RLock lock = redissonClient.getLock(lockKey);

        Map<String, Object> data = new LinkedHashMap<>();
        try {
            // 尝试获取锁，等待 3 秒，持有 ttl 秒后自动释放
            boolean acquired = lock.tryLock(3, ttl, TimeUnit.SECONDS);
            if (acquired) {
                log.info("[Redisson] 分布式锁获取成功: key={}, ttl={}s", lockKey, ttl);
                data.put("说明", "分布式锁获取成功");
                data.put("lockKey", lockKey);
                data.put("持有时间(秒)", ttl);
                data.put("获取时间", LocalDateTime.now().format(FMT));
                data.put("提示", "锁将在 " + ttl + " 秒后自动释放，或调用 DELETE /demo/cache/lock/" + key + " 手动释放");
                return Result.ok(data);
            } else {
                log.warn("[Redisson] 分布式锁获取失败（已被占用）: key={}", lockKey);
                data.put("说明", "分布式锁获取失败，锁已被其他线程/实例持有");
                data.put("lockKey", lockKey);
                return Result.fail(409, "锁已被占用");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Redisson] 获取分布式锁被中断: key={}", lockKey, e);
            return Result.fail(500, "获取锁被中断: " + e.getMessage());
        }
    }

    /**
     * 释放分布式锁
     *
     * <pre>
     * curl -X DELETE http://localhost:8090/demo/cache/lock/order-lock
     * </pre>
     *
     * @param key 锁的 Key
     * @return 释放结果
     */
    @DeleteMapping("/lock/{key}")
    public Result<Map<String, Object>> unlock(@PathVariable String key) {
        String lockKey = "lock:" + key;
        RLock lock = redissonClient.getLock(lockKey);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lockKey", lockKey);

        if (lock.isLocked()) {
            try {
                lock.forceUnlock();
                log.info("[Redisson] 分布式锁已强制释放: key={}", lockKey);
                data.put("说明", "分布式锁已释放");
            } catch (Exception e) {
                log.error("[Redisson] 释放分布式锁失败: key={}", lockKey, e);
                return Result.fail(500, "释放锁失败: " + e.getMessage());
            }
        } else {
            data.put("说明", "锁不存在或已过期");
        }

        data.put("操作时间", LocalDateTime.now().format(FMT));
        return Result.ok(data);
    }
}

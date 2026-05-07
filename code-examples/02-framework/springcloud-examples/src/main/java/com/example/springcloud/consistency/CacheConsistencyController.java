package com.example.springcloud.consistency;

import com.example.springcloud.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 10.A.26 缓存一致性实战 Controller
 *
 * <p>演示三种经典的缓存与数据库一致性方案：
 * <ul>
 *   <li>Cache Aside（旁路缓存）：先更新 DB，再删除缓存</li>
 *   <li>Write Through（写穿透）：同时更新 DB 和缓存</li>
 *   <li>延迟双删：删缓存 → 更新 DB → 延迟再删缓存</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 初始化测试数据
 * curl http://localhost:8090/demo/consistency/init
 *
 * # Cache Aside 模式更新
 * curl -X PUT "http://localhost:8090/demo/consistency/cache-aside?id=1&amp;name=alice_updated"
 *
 * # Write Through 模式更新
 * curl -X PUT "http://localhost:8090/demo/consistency/write-through?id=1&amp;name=alice_wt"
 *
 * # 延迟双删模式更新
 * curl -X PUT "http://localhost:8090/demo/consistency/delay-double-delete?id=1&amp;name=alice_ddd"
 *
 * # 验证 DB 和缓存是否一致
 * curl "http://localhost:8090/demo/consistency/verify?id=1"
 *
 * # 缓存一致性方案对比
 * curl http://localhost:8090/demo/consistency/compare
 * </pre>
 */
@RestController
@RequestMapping("/demo/consistency")
public class CacheConsistencyController {

    private static final Logger log = LoggerFactory.getLogger(CacheConsistencyController.class);

    private static final String CACHE_PREFIX = "user:";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public CacheConsistencyController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 初始化测试数据 — 复用 users 表，向 Redis 写入对应缓存
     */
    @GetMapping("/init")
    public Result<Map<String, Object>> init() {
        // 查询 users 表中的数据
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id, name FROM users LIMIT 10");

        int cacheCount = 0;
        for (Map<String, Object> user : users) {
            Object id = user.get("id");
            Object name = user.get("name");
            String cacheKey = CACHE_PREFIX + id;
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(name), 30, TimeUnit.MINUTES);
            cacheCount++;
        }

        log.info("[CacheConsistency] 初始化完成: 同步 {} 条用户数据到 Redis 缓存", cacheCount);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "测试数据初始化完成");
        data.put("DB记录数", users.size());
        data.put("缓存写入数", cacheCount);
        data.put("缓存Key格式", CACHE_PREFIX + "{id}");
        data.put("缓存TTL", "30 分钟");
        data.put("用户列表", users);
        return Result.ok(data);
    }

    /**
     * Cache Aside 模式更新 — 先更新 DB，再删除缓存
     *
     * @param id   用户 ID
     * @param name 新用户名
     */
    @PutMapping("/cache-aside")
    public Result<Map<String, Object>> cacheAside(@RequestParam Long id, @RequestParam String name) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. 先更新 DB
        int rows = jdbcTemplate.update("UPDATE users SET name = ? WHERE id = ?", name, id);
        log.info("[CacheAside] 步骤1: 更新 DB, id={}, name={}, rows={}", id, name, rows);

        // 2. 再删除缓存
        Boolean deleted = redisTemplate.delete(cacheKey);
        log.info("[CacheAside] 步骤2: 删除缓存, key={}, deleted={}", cacheKey, deleted);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("模式", "Cache Aside（旁路缓存）");
        data.put("步骤", "1.更新DB → 2.删除缓存");
        data.put("DB更新行数", rows);
        data.put("缓存已删除", Boolean.TRUE.equals(deleted));
        data.put("说明", "下次读取时缓存未命中，会从 DB 加载并回填缓存");
        return Result.ok(data);
    }

    /**
     * Write Through 模式更新 — 同时更新 DB 和缓存
     *
     * @param id   用户 ID
     * @param name 新用户名
     */
    @PutMapping("/write-through")
    public Result<Map<String, Object>> writeThrough(@RequestParam Long id, @RequestParam String name) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. 更新 DB
        int rows = jdbcTemplate.update("UPDATE users SET name = ? WHERE id = ?", name, id);
        log.info("[WriteThrough] 步骤1: 更新 DB, id={}, name={}, rows={}", id, name, rows);

        // 2. 同时更新缓存
        redisTemplate.opsForValue().set(cacheKey, name, 30, TimeUnit.MINUTES);
        log.info("[WriteThrough] 步骤2: 更新缓存, key={}, value={}", cacheKey, name);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("模式", "Write Through（写穿透）");
        data.put("步骤", "1.更新DB → 2.更新缓存");
        data.put("DB更新行数", rows);
        data.put("缓存已更新", true);
        data.put("说明", "DB 和缓存同步更新，保证数据一致");
        return Result.ok(data);
    }

    /**
     * 延迟双删模式更新 — 删缓存 → 更新 DB → 延迟 500ms 再删缓存
     *
     * @param id   用户 ID
     * @param name 新用户名
     */
    @PutMapping("/delay-double-delete")
    public Result<Map<String, Object>> delayDoubleDelete(@RequestParam Long id, @RequestParam String name) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. 第一次删除缓存
        Boolean firstDelete = redisTemplate.delete(cacheKey);
        log.info("[DelayDoubleDelete] 步骤1: 第一次删除缓存, key={}, deleted={}", cacheKey, firstDelete);

        // 2. 更新 DB
        int rows = jdbcTemplate.update("UPDATE users SET name = ? WHERE id = ?", name, id);
        log.info("[DelayDoubleDelete] 步骤2: 更新 DB, id={}, name={}, rows={}", id, name, rows);

        // 3. 延迟 500ms 后第二次删除缓存
        CompletableFuture.runAsync(() -> {
            Boolean secondDelete = redisTemplate.delete(cacheKey);
            log.info("[DelayDoubleDelete] 步骤3: 延迟双删完成, key={}, deleted={}", cacheKey, secondDelete);
        }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("模式", "延迟双删");
        data.put("步骤", "1.删缓存 → 2.更新DB → 3.延迟500ms再删缓存");
        data.put("第一次删除", Boolean.TRUE.equals(firstDelete));
        data.put("DB更新行数", rows);
        data.put("延迟双删", "500ms 后异步执行第二次删除");
        data.put("说明", "通过两次删除缓存，降低并发场景下缓存不一致的概率");
        return Result.ok(data);
    }

    /**
     * 验证 DB 和缓存是否一致
     *
     * @param id 用户 ID
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestParam Long id) {
        String cacheKey = CACHE_PREFIX + id;

        // 从 DB 读取
        String dbName = null;
        try {
            dbName = jdbcTemplate.queryForObject("SELECT name FROM users WHERE id = ?", String.class, id);
        } catch (Exception e) {
            log.warn("[CacheConsistency] DB 查询失败: id={}, error={}", id, e.getMessage());
        }

        // 从 Redis 读取
        String cacheName = redisTemplate.opsForValue().get(cacheKey);

        boolean consistent = Objects.equals(dbName, cacheName);
        log.info("[CacheConsistency] 一致性验证: id={}, db={}, cache={}, consistent={}", id, dbName, cacheName, consistent);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("用户ID", id);
        data.put("DB值", dbName);
        data.put("缓存值", cacheName);
        data.put("是否一致", consistent);
        if (!consistent) {
            data.put("说明", cacheName == null ? "缓存不存在（可能已被删除，下次读取会回填）" : "DB 和缓存数据不一致！");
        } else {
            data.put("说明", "DB 和缓存数据一致");
        }
        return Result.ok(data);
    }

    /**
     * 缓存一致性方案对比
     */
    @GetMapping("/compare")
    public Result<List<Map<String, Object>>> compare() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> cacheAside = new LinkedHashMap<>();
        cacheAside.put("方案", "Cache Aside（旁路缓存）");
        cacheAside.put("策略", "先更新 DB，再删除缓存");
        cacheAside.put("优点", "实现简单，是最常用的方案；缓存按需加载，不浪费资源");
        cacheAside.put("缺点", "极端并发下仍可能短暂不一致（读请求在删缓存前回填了旧值）");
        cacheAside.put("一致性", "最终一致");
        list.add(cacheAside);

        Map<String, Object> writeThrough = new LinkedHashMap<>();
        writeThrough.put("方案", "Write Through（写穿透）");
        writeThrough.put("策略", "同时更新 DB 和缓存");
        writeThrough.put("优点", "缓存始终有最新数据，读性能好");
        writeThrough.put("缺点", "写操作较重（每次写都要更新缓存），可能写入不常读的数据");
        writeThrough.put("一致性", "强一致（单机），分布式下需配合分布式事务");
        list.add(writeThrough);

        Map<String, Object> delayDoubleDelete = new LinkedHashMap<>();
        delayDoubleDelete.put("方案", "延迟双删");
        delayDoubleDelete.put("策略", "删缓存 → 更新 DB → 延迟再删缓存");
        delayDoubleDelete.put("优点", "有效降低并发场景下缓存不一致的概率");
        delayDoubleDelete.put("缺点", "延迟时间难以精确控制，实现复杂度较高");
        delayDoubleDelete.put("一致性", "最终一致（比 Cache Aside 更可靠）");
        list.add(delayDoubleDelete);

        return Result.ok(list);
    }
}

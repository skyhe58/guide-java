package com.example.springcloud.task;

import com.example.springcloud.common.Result;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 10.A.23 定时任务 + 分布式锁实战 Controller
 *
 * <p>演示 Spring @Scheduled 定时任务配合 Redisson 分布式锁，防止多实例重复执行：
 * <ul>
 *   <li>每 30 秒自动执行定时任务（Redisson 分布式锁保护）</li>
 *   <li>查看任务执行状态和历史记录</li>
 *   <li>手动触发任务</li>
 *   <li>动态修改 cron 表达式</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 查看任务执行状态和历史
 * curl http://localhost:8090/demo/task/status
 *
 * # 手动触发一次任务
 * curl -X POST http://localhost:8090/demo/task/trigger
 *
 * # 动态修改 cron 表达式（每 10 秒执行一次）
 * curl -X POST "http://localhost:8090/demo/task/cron?expr=0/10 * * * * ?"
 * </pre>
 */
@RestController
@RequestMapping("/demo/task")
public class ScheduledTaskController {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskController.class);

    private static final String LOCK_KEY = "scheduled:demo-task";
    private static final int MAX_RECORDS = 20;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RedissonClient redissonClient;

    /** 执行记录列表（线程安全，最多保留 20 条） */
    private final CopyOnWriteArrayList<Map<String, Object>> executionRecords = new CopyOnWriteArrayList<>();

    /** 当前 cron 表达式（volatile 保证可见性） */
    private volatile String currentCron = "0/30 * * * * ?";

    /** 是否启用定时任务 */
    private volatile boolean enabled = true;

    public ScheduledTaskController(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 定时任务 — 每 30 秒执行一次，使用 Redisson 分布式锁防止多实例重复执行
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void scheduledTask() {
        if (!enabled) {
            return;
        }
        executeTask("scheduled");
    }

    /**
     * 查看任务执行状态和历史记录
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("任务名称", "demo-task");
        data.put("当前cron", currentCron);
        data.put("是否启用", enabled);
        data.put("锁名称", LOCK_KEY);
        data.put("历史记录数", executionRecords.size());
        data.put("最近执行记录", new ArrayList<>(executionRecords));
        return Result.ok(data);
    }

    /**
     * 手动触发一次任务
     */
    @PostMapping("/trigger")
    public Result<Map<String, Object>> trigger() {
        log.info("[ScheduledTask] 手动触发任务");
        Map<String, Object> record = executeTask("manual");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "手动触发任务完成");
        data.put("执行结果", record);
        return Result.ok(data);
    }

    /**
     * 动态修改 cron 表达式
     *
     * <p>注意：由于 @Scheduled 注解的 cron 是编译期固定的，这里通过 volatile 标志位
     * 控制任务是否跳过执行，实际生产中建议使用 SchedulingConfigurer 实现真正的动态调度。
     *
     * @param expr 新的 cron 表达式
     */
    @PostMapping("/cron")
    public Result<Map<String, Object>> updateCron(@RequestParam String expr) {
        String oldCron = this.currentCron;
        this.currentCron = expr;
        log.info("[ScheduledTask] cron 表达式已更新: {} -> {}", oldCron, expr);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "cron 表达式已更新（标志位模式）");
        data.put("旧cron", oldCron);
        data.put("新cron", expr);
        data.put("提示", "@Scheduled 固定 cron 不变，生产环境建议使用 SchedulingConfigurer 实现动态调度");
        return Result.ok(data);
    }

    /**
     * 执行任务核心逻辑：尝试获取分布式锁，成功则执行任务
     *
     * @param triggerType 触发类型（scheduled / manual）
     * @return 执行记录
     */
    private Map<String, Object> executeTask(String triggerType) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("执行时间", LocalDateTime.now().format(FMT));
        record.put("触发方式", triggerType);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            // 尝试获取锁，等待 0 秒，持有 25 秒（小于调度间隔）
            acquired = lock.tryLock(0, 25, TimeUnit.SECONDS);
            record.put("获取锁", acquired);

            if (acquired) {
                // 模拟业务逻辑
                log.info("[ScheduledTask] 获取分布式锁成功，开始执行任务 ({})", triggerType);
                Thread.sleep(100); // 模拟耗时操作
                record.put("执行结果", "成功");
                log.info("[ScheduledTask] 任务执行完成");
            } else {
                record.put("执行结果", "跳过（其他实例正在执行）");
                log.info("[ScheduledTask] 未获取到锁，跳过本次执行");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record.put("执行结果", "中断: " + e.getMessage());
            log.error("[ScheduledTask] 任务执行被中断", e);
        } catch (Exception e) {
            record.put("执行结果", "异常: " + e.getMessage());
            log.error("[ScheduledTask] 任务执行异常", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        record.put("耗时ms", elapsed);

        // 保留最近 20 条记录
        executionRecords.add(record);
        while (executionRecords.size() > MAX_RECORDS) {
            executionRecords.remove(0);
        }

        return record;
    }
}

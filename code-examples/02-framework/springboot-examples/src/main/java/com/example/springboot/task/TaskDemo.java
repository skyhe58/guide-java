package com.example.springboot.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时任务演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>@Scheduled(fixedRate) — 固定频率执行</li>
 *   <li>@Scheduled(fixedDelay) — 固定延迟执行</li>
 *   <li>@Scheduled(cron) — Cron 表达式</li>
 *   <li>fixedRate 和 fixedDelay 的区别</li>
 * </ul>
 *
 * <p>注意：需要在主启动类上添加 @EnableScheduling 注解。</p>
 */
@Component
public class TaskDemo {

    private static final Logger log = LoggerFactory.getLogger(TaskDemo.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final AtomicInteger fixedRateCount = new AtomicInteger(0);
    private final AtomicInteger fixedDelayCount = new AtomicInteger(0);

    /**
     * fixedRate — 固定频率执行
     *
     * <p>从上次任务<b>开始时间</b>算起，每隔指定时间执行一次。
     * 如果任务执行时间超过间隔，任务会排队等待。</p>
     *
     * <p>设置为 60 秒避免日志过多。</p>
     */
    @Scheduled(fixedRate = 60000, initialDelay = 5000)
    public void fixedRateTask() {
        int count = fixedRateCount.incrementAndGet();
        log.info("[fixedRate] 第 {} 次执行, 时间: {}", count, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * fixedDelay — 固定延迟执行
     *
     * <p>从上次任务<b>结束时间</b>算起，延迟指定时间后执行。
     * 保证两次执行之间有固定的间隔。</p>
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void fixedDelayTask() {
        int count = fixedDelayCount.incrementAndGet();
        log.info("[fixedDelay] 第 {} 次执行, 时间: {}", count, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Cron 表达式 — 灵活的时间调度
     *
     * <p>格式：秒 分 时 日 月 周</p>
     * <p>示例：每分钟的第 0 秒执行</p>
     *
     * <p>常用 Cron 表达式：</p>
     * <ul>
     *   <li>0 0 2 * * ? — 每天凌晨 2 点</li>
     *   <li>0 0/5 * * * ? — 每 5 分钟</li>
     *   <li>0 0 9-18 * * MON-FRI — 工作日 9-18 点每小时</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时整点执行
    public void cronTask() {
        log.info("[cron] 整点任务执行, 时间: {}", LocalDateTime.now().format(FORMATTER));
    }
}

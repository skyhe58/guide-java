package com.example.concurrent.threadpool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池原理演示
 * <p>
 * 演示内容：
 * 1. 自定义线程池（推荐方式）
 * 2. 4 种拒绝策略演示
 * 3. 线程池监控指标
 * 4. 动态调参
 * </p>
 */
public class ThreadPoolDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 线程池原理演示 ==========\n");

        demonstrateCustomThreadPool();
        System.out.println();

        demonstrateRejectionPolicies();
        System.out.println();

        demonstrateMonitoring();
        System.out.println();

        demonstrateDynamicResize();
    }

    // ==================== 1. 自定义线程池 ====================

    /**
     * 推荐的线程池创建方式（不使用 Executors 工厂方法）
     */
    private static void demonstrateCustomThreadPool() throws Exception {
        System.out.println("--- 1. 自定义线程池（推荐方式） ---");

        // 自定义线程工厂：设置有意义的线程名，便于排查问题
        ThreadFactory namedFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "biz-pool-" + counter.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };

        // 创建线程池：明确指定所有参数
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                                      // 核心线程数
                4,                                      // 最大线程数
                60, TimeUnit.SECONDS,                   // 非核心线程空闲存活时间
                new LinkedBlockingQueue<>(10),           // 有界队列（重要！）
                namedFactory,                            // 自定义线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );

        // 提交任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("任务 " + taskId + " 由 "
                        + Thread.currentThread().getName() + " 执行");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("线程池已关闭");
    }

    // ==================== 2. 拒绝策略演示 ====================

    /**
     * 演示 4 种拒绝策略的行为
     */
    private static void demonstrateRejectionPolicies() throws Exception {
        System.out.println("--- 2. 拒绝策略演示 ---");

        // AbortPolicy：抛出异常（默认）
        testRejectionPolicy("AbortPolicy",
                new ThreadPoolExecutor.AbortPolicy());

        // CallerRunsPolicy：由提交任务的线程执行
        testRejectionPolicy("CallerRunsPolicy",
                new ThreadPoolExecutor.CallerRunsPolicy());

        // DiscardPolicy：静默丢弃
        testRejectionPolicy("DiscardPolicy",
                new ThreadPoolExecutor.DiscardPolicy());

        // DiscardOldestPolicy：丢弃队列中最老的任务
        testRejectionPolicy("DiscardOldestPolicy",
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private static void testRejectionPolicy(String policyName,
                                             RejectedExecutionHandler handler) throws Exception {
        // 极小的线程池：1 核心线程 + 1 队列容量 + 1 最大线程 = 最多处理 3 个任务
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1),
                handler
        );

        AtomicInteger executed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    executed.incrementAndGet();
                    try { Thread.sleep(100); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                rejected.incrementAndGet();
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("[" + policyName + "] 执行: " + executed.get()
                + ", 拒绝: " + rejected.get());
    }

    // ==================== 3. 线程池监控 ====================

    /**
     * 演示线程池的关键监控指标
     */
    private static void demonstrateMonitoring() throws Exception {
        System.out.println("--- 3. 线程池监控指标 ---");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        // 提交一批任务
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                try { Thread.sleep(200); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 等待一些任务开始执行
        Thread.sleep(100);

        // 打印监控指标
        System.out.println("当前线程数 (poolSize): " + executor.getPoolSize());
        System.out.println("活跃线程数 (activeCount): " + executor.getActiveCount());
        System.out.println("队列等待数 (queueSize): " + executor.getQueue().size());
        System.out.println("已完成任务数 (completedTaskCount): " + executor.getCompletedTaskCount());
        System.out.println("历史最大线程数 (largestPoolSize): " + executor.getLargestPoolSize());
        System.out.println("总任务数 (taskCount): " + executor.getTaskCount());

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n关闭后 — 已完成任务数: " + executor.getCompletedTaskCount());
    }

    // ==================== 4. 动态调参 ====================

    /**
     * 演示运行时动态调整线程池参数
     * 生产中可配合配置中心（Apollo/Nacos）实现线上动态调参
     */
    private static void demonstrateDynamicResize() throws Exception {
        System.out.println("--- 4. 动态调参 ---");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        System.out.println("调整前 — 核心线程数: " + executor.getCorePoolSize()
                + ", 最大线程数: " + executor.getMaximumPoolSize());

        // 动态增大核心线程数
        executor.setCorePoolSize(4);
        executor.setMaximumPoolSize(8);

        System.out.println("调整后 — 核心线程数: " + executor.getCorePoolSize()
                + ", 最大线程数: " + executor.getMaximumPoolSize());

        // 提交任务验证
        CountDownLatch latch = new CountDownLatch(8);
        for (int i = 0; i < 8; i++) {
            executor.submit(() -> {
                System.out.println("  任务由 " + Thread.currentThread().getName() + " 执行");
                latch.countDown();
            });
        }
        latch.await();

        System.out.println("当前线程数: " + executor.getPoolSize());

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}

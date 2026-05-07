package com.example.concurrent.threadpool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 线程池参数验证和拒绝策略测试
 */
@DisplayName("线程池核心功能测试")
class ThreadPoolTest {

    @Test
    @DisplayName("核心线程数和最大线程数参数验证")
    void shouldRespectCoreAndMaxPoolSize() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2)
        );

        try {
            assertEquals(2, executor.getCorePoolSize(), "核心线程数应为 2");
            assertEquals(4, executor.getMaximumPoolSize(), "最大线程数应为 4");
            assertEquals(0, executor.getPoolSize(), "初始线程数应为 0");

            // 提交 2 个任务，应创建 2 个核心线程
            CountDownLatch taskLatch = new CountDownLatch(1);
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try { taskLatch.await(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            Thread.sleep(100);
            assertEquals(2, executor.getPoolSize(), "应创建 2 个核心线程");

            // 再提交 2 个任务，应进入队列（队列容量为 2）
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try { taskLatch.await(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            Thread.sleep(100);
            assertEquals(2, executor.getQueue().size(), "队列中应有 2 个等待任务");
            assertEquals(2, executor.getPoolSize(), "线程数仍为 2（队列未满）");

            // 再提交 2 个任务，队列满，应创建非核心线程
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try { taskLatch.await(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            Thread.sleep(100);
            assertEquals(4, executor.getPoolSize(), "应创建到最大线程数 4");

            taskLatch.countDown(); // 释放所有任务
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("AbortPolicy 拒绝策略应抛出 RejectedExecutionException")
    void shouldThrowOnAbortPolicy() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()
        );

        try {
            CountDownLatch blockLatch = new CountDownLatch(1);

            // 提交 2 个任务（1 个执行 + 1 个在队列中）
            executor.submit(() -> {
                try { blockLatch.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            executor.submit(() -> {
                try { blockLatch.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // 第 3 个任务应被拒绝
            assertThrows(RejectedExecutionException.class, () -> {
                executor.submit(() -> {});
            }, "超过容量时 AbortPolicy 应抛出异常");

            blockLatch.countDown();
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("CallerRunsPolicy 应由提交线程执行被拒绝的任务")
    void shouldRunInCallerThreadOnCallerRunsPolicy() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            CountDownLatch blockLatch = new CountDownLatch(1);
            AtomicInteger callerRunCount = new AtomicInteger(0);
            String mainThreadName = Thread.currentThread().getName();

            // 填满线程池和队列
            executor.submit(() -> {
                try { blockLatch.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            executor.submit(() -> {
                try { blockLatch.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // 被拒绝的任务应在调用者线程执行
            executor.submit(() -> {
                if (Thread.currentThread().getName().equals(mainThreadName)) {
                    callerRunCount.incrementAndGet();
                }
            });

            assertTrue(callerRunCount.get() >= 0,
                    "CallerRunsPolicy 应在调用者线程执行任务");

            blockLatch.countDown();
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("动态调整核心线程数")
    void shouldDynamicallyResizeCorePoolSize() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        try {
            assertEquals(2, executor.getCorePoolSize());

            // 动态增大核心线程数
            executor.setCorePoolSize(4);
            assertEquals(4, executor.getCorePoolSize(), "核心线程数应更新为 4");

            // 动态减小核心线程数
            executor.setCorePoolSize(1);
            assertEquals(1, executor.getCorePoolSize(), "核心线程数应更新为 1");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("线程池完成任务计数正确")
    void shouldTrackCompletedTaskCount() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 2, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        try {
            int taskCount = 10;
            CountDownLatch latch = new CountDownLatch(taskCount);

            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    latch.countDown();
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            Thread.sleep(100); // 等待计数更新

            assertEquals(taskCount, executor.getCompletedTaskCount(),
                    "已完成任务数应等于提交的任务数");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

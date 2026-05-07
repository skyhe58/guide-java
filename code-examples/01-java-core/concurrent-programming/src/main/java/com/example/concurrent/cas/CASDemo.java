package com.example.concurrent.cas;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * CAS 与原子类演示
 * <p>
 * 演示内容：
 * 1. CAS 原理演示（AtomicInteger 的 compareAndSet）
 * 2. ABA 问题演示与解决（AtomicStampedReference）
 * 3. AtomicInteger vs LongAdder 性能对比
 * </p>
 */
public class CASDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== CAS 与原子类演示 ==========\n");

        demonstrateCAS();
        System.out.println();

        demonstrateABAProblem();
        System.out.println();

        demonstratePerformanceComparison();
    }

    // ==================== 1. CAS 原理演示 ====================

    /**
     * CAS（Compare And Swap）：比较并交换
     * 包含三个操作数：内存值 V、预期值 A、新值 B
     * 只有 V == A 时，才将 V 更新为 B
     */
    private static void demonstrateCAS() throws Exception {
        System.out.println("--- 1. CAS 原理演示 ---");

        AtomicInteger atomicInt = new AtomicInteger(0);

        // 基本 CAS 操作
        boolean success1 = atomicInt.compareAndSet(0, 1); // 期望 0，设为 1
        System.out.println("CAS(0→1): " + success1 + ", 当前值: " + atomicInt.get());

        boolean success2 = atomicInt.compareAndSet(0, 2); // 期望 0，但当前是 1
        System.out.println("CAS(0→2): " + success2 + ", 当前值: " + atomicInt.get());

        // 自旋 CAS 实现原子递增
        System.out.println("\n自旋 CAS 实现原子递增：");
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        int incrementPerThread = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementPerThread; j++) {
                    // 自旋 CAS
                    int expected, newValue;
                    do {
                        expected = counter.get();
                        newValue = expected + 1;
                    } while (!counter.compareAndSet(expected, newValue));
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("期望值: " + (threadCount * incrementPerThread));
        System.out.println("实际值: " + counter.get());
        System.out.println("CAS 保证原子性: " +
                (counter.get() == threadCount * incrementPerThread ? "✓" : "✗"));
    }

    // ==================== 2. ABA 问题 ====================

    /**
     * ABA 问题：值从 A 变为 B 再变回 A，CAS 无法感知中间的变化
     * 解决方案：AtomicStampedReference（带版本号）
     */
    private static void demonstrateABAProblem() throws Exception {
        System.out.println("--- 2. ABA 问题演示与解决 ---");

        // 演示 ABA 问题
        System.out.println("\n[ABA 问题] 使用 AtomicReference：");
        AtomicReference<String> ref = new AtomicReference<>("A");

        // 线程 1：读取值 A，准备 CAS 更新为 C
        Thread t1 = new Thread(() -> {
            String expected = ref.get(); // 读取 A
            System.out.println("  线程1 读取: " + expected);
            sleep(200); // 模拟耗时操作

            boolean success = ref.compareAndSet(expected, "C");
            System.out.println("  线程1 CAS(A→C): " + success
                    + "（不知道值已经被改过了！）");
        }, "t1");

        // 线程 2：将 A→B→A（ABA 操作）
        Thread t2 = new Thread(() -> {
            sleep(50);
            ref.compareAndSet("A", "B");
            System.out.println("  线程2 CAS(A→B): 成功");
            ref.compareAndSet("B", "A");
            System.out.println("  线程2 CAS(B→A): 成功（ABA 完成）");
        }, "t2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // 使用 AtomicStampedReference 解决 ABA
        System.out.println("\n[解决 ABA] 使用 AtomicStampedReference：");
        AtomicStampedReference<String> stampedRef =
                new AtomicStampedReference<>("A", 0);

        Thread t3 = new Thread(() -> {
            int stamp = stampedRef.getStamp(); // 记录版本号
            String expected = stampedRef.getReference();
            System.out.println("  线程3 读取: " + expected + ", 版本: " + stamp);
            sleep(200);

            boolean success = stampedRef.compareAndSet(expected, "C", stamp, stamp + 1);
            System.out.println("  线程3 CAS(A→C): " + success
                    + "（版本号不匹配，检测到 ABA！）");
        }, "t3");

        Thread t4 = new Thread(() -> {
            sleep(50);
            int stamp = stampedRef.getStamp();
            stampedRef.compareAndSet("A", "B", stamp, stamp + 1);
            System.out.println("  线程4 CAS(A→B): 成功, 版本: " + stampedRef.getStamp());

            stamp = stampedRef.getStamp();
            stampedRef.compareAndSet("B", "A", stamp, stamp + 1);
            System.out.println("  线程4 CAS(B→A): 成功, 版本: " + stampedRef.getStamp());
        }, "t4");

        t3.start();
        t4.start();
        t3.join();
        t4.join();
        System.out.println("  最终值: " + stampedRef.getReference()
                + ", 版本: " + stampedRef.getStamp());
    }

    // ==================== 3. 性能对比 ====================

    /**
     * AtomicLong vs LongAdder 性能对比
     * 高并发写场景下，LongAdder 性能远优于 AtomicLong
     */
    private static void demonstratePerformanceComparison() throws Exception {
        System.out.println("--- 3. AtomicLong vs LongAdder 性能对比 ---");

        int threadCount = 10;
        int incrementPerThread = 1_000_000;

        // AtomicLong 测试
        AtomicLong atomicLong = new AtomicLong(0);
        long atomicTime = benchmark(threadCount, incrementPerThread, () -> {
            atomicLong.incrementAndGet();
        });
        System.out.println("AtomicLong  — 结果: " + atomicLong.get()
                + ", 耗时: " + atomicTime + "ms");

        // LongAdder 测试
        LongAdder longAdder = new LongAdder();
        long adderTime = benchmark(threadCount, incrementPerThread, () -> {
            longAdder.increment();
        });
        System.out.println("LongAdder   — 结果: " + longAdder.sum()
                + ", 耗时: " + adderTime + "ms");

        // 对比
        double speedup = (double) atomicTime / adderTime;
        System.out.printf("LongAdder 快 %.1f 倍%n", speedup);
        System.out.println("原因：LongAdder 将热点分散到多个 Cell，减少 CAS 竞争");
    }

    /**
     * 性能基准测试
     */
    private static long benchmark(int threadCount, int opsPerThread,
                                  Runnable operation) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int j = 0; j < opsPerThread; j++) {
                    operation.run();
                }
                endLatch.countDown();
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown(); // 所有线程同时开始
        endLatch.await();
        return System.currentTimeMillis() - start;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

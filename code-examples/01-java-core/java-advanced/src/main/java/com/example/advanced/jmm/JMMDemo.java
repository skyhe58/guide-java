package com.example.advanced.jmm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 内存模型（JMM）演示
 * <p>
 * 演示内容：
 * 1. 可见性问题演示（非 volatile 变量的可见性问题）
 * 2. 指令重排序演示
 * 3. happens-before 规则验证
 * </p>
 */
public class JMMDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== JMM 内存模型演示 ==========\n");

        demonstrateVisibility();
        System.out.println();

        demonstrateReordering();
        System.out.println();

        demonstrateHappensBefore();
    }

    // ==================== 1. 可见性问题 ====================

    // 非 volatile 变量 —— 可能存在可见性问题
    private static boolean running = true;
    // volatile 变量 —— 保证可见性
    private static volatile boolean volatileRunning = true;

    /**
     * 演示非 volatile 变量的可见性问题
     * 线程 A 修改 running = false，线程 B 可能永远看不到
     */
    private static void demonstrateVisibility() throws Exception {
        System.out.println("--- 1. 可见性问题演示 ---");

        // 场景 1：使用 volatile（保证可见性）
        System.out.println("场景 1：volatile 变量");
        volatileRunning = true;
        Thread volatileThread = new Thread(() -> {
            long count = 0;
            while (volatileRunning) {
                count++;
            }
            System.out.println("  [volatile] 线程退出，循环次数: " + count);
        }, "volatile-thread");
        volatileThread.start();

        Thread.sleep(100);
        volatileRunning = false; // volatile 写，对其他线程立即可见
        volatileThread.join(1000);
        System.out.println("  [volatile] 主线程设置 volatileRunning = false");
        System.out.println("  [volatile] 线程正常退出: " + !volatileThread.isAlive() + " ✓");

        // 场景 2：非 volatile（可能存在可见性问题）
        System.out.println("\n场景 2：非 volatile 变量");
        running = true;
        Thread normalThread = new Thread(() -> {
            long count = 0;
            while (running) {
                count++;
                // 注意：如果循环体内有 synchronized 或 IO 操作，
                // 也会刷新工作内存，可能不会出现可见性问题
                // 这里用空循环来增加出现问题的概率
                if (count % 10_000_000 == 0) {
                    // 偶尔让出 CPU，但不保证刷新缓存
                    Thread.yield();
                }
            }
            System.out.println("  [非 volatile] 线程退出，循环次数: " + count);
        }, "normal-thread");
        normalThread.start();

        Thread.sleep(100);
        running = false;
        normalThread.join(1000); // 最多等 1 秒

        if (normalThread.isAlive()) {
            System.out.println("  [非 volatile] 线程未退出！可见性问题发生 ✗");
            System.out.println("  原因：线程工作内存中的 running 副本未从主内存刷新");
            normalThread.interrupt(); // 强制中断
        } else {
            System.out.println("  [非 volatile] 线程正常退出（本次未出现可见性问题）");
            System.out.println("  注意：可见性问题不是必现的，取决于 JIT 优化和 CPU 缓存");
        }
    }

    // ==================== 2. 指令重排序 ====================

    private static int x = 0, y = 0;
    private static int a = 0, b = 0;

    /**
     * 演示指令重排序
     * 理论上 (x=0, y=0) 不应该出现，但由于重排序可能出现
     */
    private static void demonstrateReordering() throws Exception {
        System.out.println("--- 2. 指令重排序演示 ---");
        System.out.println("两个线程交叉赋值和读取，检测是否发生重排序\n");

        int reorderCount = 0;
        int totalIterations = 100000;

        for (int i = 0; i < totalIterations; i++) {
            x = 0; y = 0; a = 0; b = 0;

            CountDownLatch latch = new CountDownLatch(2);

            Thread t1 = new Thread(() -> {
                a = 1; // 操作 1
                x = b; // 操作 2
                latch.countDown();
            });

            Thread t2 = new Thread(() -> {
                b = 1; // 操作 3
                y = a; // 操作 4
                latch.countDown();
            });

            t1.start();
            t2.start();
            latch.await();

            // 如果没有重排序，不可能出现 x=0 且 y=0
            // 因为至少有一个线程的赋值操作在读取之前完成
            if (x == 0 && y == 0) {
                reorderCount++;
            }
        }

        System.out.println("  总迭代次数: " + totalIterations);
        System.out.println("  检测到重排序: " + reorderCount + " 次");
        if (reorderCount > 0) {
            System.out.println("  结论: 发生了指令重排序！(x=0, y=0) 出现了 " + reorderCount + " 次");
        } else {
            System.out.println("  结论: 本次未检测到重排序（重排序是概率性的，不一定每次都能观察到）");
        }
        System.out.println("  说明: 使用 volatile 可以禁止重排序");
    }

    // ==================== 3. happens-before 验证 ====================

    private static int sharedData = 0;

    /**
     * 验证 happens-before 规则
     */
    private static void demonstrateHappensBefore() throws Exception {
        System.out.println("--- 3. happens-before 规则验证 ---");

        // 规则 4：线程启动规则
        // Thread.start() happens-before 线程中的任何操作
        System.out.println("\n规则 4 - 线程启动规则：");
        sharedData = 42; // 主线程写入
        Thread t1 = new Thread(() -> {
            // start() 之前的写入对新线程可见
            System.out.println("  子线程读取 sharedData = " + sharedData + "（期望 42）");
        });
        t1.start();
        t1.join();

        // 规则 5：线程终止规则
        // 线程中的所有操作 happens-before Thread.join() 返回
        System.out.println("\n规则 5 - 线程终止规则：");
        Thread t2 = new Thread(() -> {
            sharedData = 100; // 子线程写入
        });
        t2.start();
        t2.join(); // join 返回后，子线程的写入对主线程可见
        System.out.println("  主线程读取 sharedData = " + sharedData + "（期望 100）");

        // 规则 2：监视器锁规则
        // unlock happens-before 后续的 lock
        System.out.println("\n规则 2 - 监视器锁规则：");
        Object lock = new Object();
        AtomicInteger result = new AtomicInteger(0);

        Thread t3 = new Thread(() -> {
            synchronized (lock) {
                sharedData = 200;
            }
        });
        t3.start();
        t3.join();

        Thread t4 = new Thread(() -> {
            synchronized (lock) {
                // t3 的 unlock happens-before t4 的 lock
                // 所以 sharedData = 200 对 t4 可见
                result.set(sharedData);
            }
        });
        t4.start();
        t4.join();
        System.out.println("  通过 synchronized 传递: sharedData = " + result.get() + "（期望 200）");

        // 规则 8：传递性
        System.out.println("\n规则 8 - 传递性：");
        System.out.println("  如果 A happens-before B，B happens-before C");
        System.out.println("  则 A happens-before C");
        System.out.println("  应用：volatile 写之前的所有普通写，对 volatile 读之后的操作都可见");

        // JMM vs JVM 内存区域
        System.out.println("\n--- JMM vs JVM 内存区域 ---");
        System.out.println("  JMM（Java Memory Model）：");
        System.out.println("    - 抽象规范，定义多线程变量访问规则");
        System.out.println("    - 主内存 + 工作内存");
        System.out.println("    - 关注：可见性、有序性、原子性");
        System.out.println("  JVM 内存区域：");
        System.out.println("    - 具体实现，运行时数据区划分");
        System.out.println("    - 堆、栈、方法区、PC 寄存器、本地方法栈");
        System.out.println("    - 关注：数据存储位置");
    }
}

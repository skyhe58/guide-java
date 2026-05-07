package com.example.concurrent.volatile_demo;

/**
 * volatile 关键字演示
 * <p>
 * 演示内容：
 * 1. 可见性问题演示（有无 volatile 的对比）
 * 2. DCL 双重检查锁定单例
 * 3. volatile 不保证原子性
 * </p>
 */
public class VolatileDemo {

    // ==================== 可见性演示 ====================

    /**
     * 使用 volatile 保证可见性
     * 如果去掉 volatile，线程可能永远看不到 running 的变化
     */
    private static volatile boolean running = true;

    /**
     * 不使用 volatile 的对比（可能导致死循环）
     */
    private static boolean runningNoVolatile = true;

    // ==================== 原子性演示 ====================

    /**
     * volatile 不保证原子性：count++ 不是线程安全的
     */
    private static volatile int volatileCount = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("========== volatile 关键字演示 ==========\n");

        demonstrateVisibility();
        System.out.println();

        demonstrateDCLSingleton();
        System.out.println();

        demonstrateNonAtomicity();
    }

    // ==================== 1. 可见性问题演示 ====================

    /**
     * 演示 volatile 保证可见性
     */
    private static void demonstrateVisibility() throws Exception {
        System.out.println("--- 1. volatile 可见性演示 ---");

        // 使用 volatile 的版本
        running = true;
        Thread worker = new Thread(() -> {
            int count = 0;
            while (running) {
                count++;
            }
            System.out.println("[volatile] 线程停止，循环次数: " + count);
        }, "volatile-worker");

        worker.start();
        Thread.sleep(100);
        running = false; // volatile 保证其他线程立即可见
        worker.join(1000);

        if (worker.isAlive()) {
            System.out.println("[volatile] 线程未停止（不应该发生）");
            worker.interrupt();
        } else {
            System.out.println("[volatile] 线程已正常停止 ✓");
        }

        // 说明：不使用 volatile 的版本在某些 JVM 实现下可能导致死循环
        // 因为 JIT 编译器可能将 while(runningNoVolatile) 优化为 while(true)
        System.out.println("提示：去掉 volatile 后，JIT 可能将循环条件优化为常量，导致死循环");
    }

    // ==================== 2. DCL 单例 ====================

    /**
     * 演示 DCL（Double-Checked Locking）单例模式
     */
    private static void demonstrateDCLSingleton() throws Exception {
        System.out.println("--- 2. DCL 双重检查锁定单例 ---");

        // 多线程并发获取单例
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Singleton instance = Singleton.getInstance();
                System.out.println(Thread.currentThread().getName()
                        + " 获取单例: " + instance.hashCode());
            }, "singleton-" + i);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("所有线程获取的是同一个实例: " +
                (Singleton.getInstance() == Singleton.getInstance()));
    }

    /**
     * DCL 单例 — volatile 防止指令重排
     * <p>
     * new Singleton() 分三步：
     * 1. 分配内存空间
     * 2. 初始化对象
     * 3. 将 instance 指向分配的内存
     * <p>
     * 如果不加 volatile，JVM 可能将 2 和 3 重排序为 1→3→2
     * 此时另一个线程看到 instance != null，但对象尚未初始化完成
     */
    static class Singleton {
        // 必须加 volatile！禁止指令重排序
        private static volatile Singleton instance;

        private Singleton() {
            System.out.println("Singleton 构造函数被调用");
        }

        public static Singleton getInstance() {
            if (instance == null) {                    // 第一次检查（无锁，快速路径）
                synchronized (Singleton.class) {
                    if (instance == null) {            // 第二次检查（有锁，防止重复创建）
                        instance = new Singleton();    // volatile 禁止这里的重排序
                    }
                }
            }
            return instance;
        }
    }

    // ==================== 3. volatile 不保证原子性 ====================

    /**
     * 演示 volatile 不保证原子性
     * count++ 是 读-改-写 三步操作，volatile 无法保证其原子性
     */
    private static void demonstrateNonAtomicity() throws Exception {
        System.out.println("--- 3. volatile 不保证原子性 ---");

        volatileCount = 0;
        int threadCount = 10;
        int incrementPerThread = 10000;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementPerThread; j++) {
                    volatileCount++; // 非原子操作！
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) t.join();

        int expected = threadCount * incrementPerThread;
        System.out.println("期望值: " + expected);
        System.out.println("实际值: " + volatileCount);
        System.out.println("volatile 保证原子性? " + (volatileCount == expected ? "是" : "否 ✗"));
        System.out.println("提示：volatile 只保证可见性和有序性，不保证原子性");
        System.out.println("解决方案：使用 AtomicInteger 或 synchronized");
    }
}

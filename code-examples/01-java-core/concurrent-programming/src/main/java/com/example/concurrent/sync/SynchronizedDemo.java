package com.example.concurrent.sync;

import java.util.concurrent.CountDownLatch;

/**
 * synchronized 用法演示
 * <p>
 * 演示内容：
 * 1. synchronized 的三种用法（实例方法、静态方法、代码块）
 * 2. 锁升级观察（偏向锁→轻量级锁→重量级锁）
 * 3. 可重入性演示
 * 4. 死锁演示
 * </p>
 */
public class SynchronizedDemo {

    private int count = 0;
    private final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        System.out.println("========== synchronized 用法演示 ==========\n");

        demonstrateThreeUsages();
        System.out.println();

        demonstrateReentrant();
        System.out.println();

        demonstrateLockEscalation();
        System.out.println();

        demonstrateDeadlock();
    }

    // ==================== 1. 三种用法 ====================

    /**
     * 演示 synchronized 的三种用法
     */
    private static void demonstrateThreeUsages() throws Exception {
        System.out.println("--- 1. synchronized 三种用法 ---");
        SynchronizedDemo demo = new SynchronizedDemo();
        int threadCount = 10;
        int incrementPerThread = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 使用 synchronized 实例方法保证线程安全
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementPerThread; j++) {
                    demo.incrementSync();
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("synchronized 实例方法 — 最终 count = " + demo.count
                + " (期望: " + (threadCount * incrementPerThread) + ")");

        // 重置
        demo.count = 0;
        CountDownLatch latch2 = new CountDownLatch(threadCount);

        // 使用 synchronized 代码块
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementPerThread; j++) {
                    demo.incrementWithBlock();
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.println("synchronized 代码块 — 最终 count = " + demo.count
                + " (期望: " + (threadCount * incrementPerThread) + ")");

        // 静态方法
        staticSyncMethod();
        System.out.println("synchronized 静态方法 — 锁的是 Class 对象");
    }

    /**
     * 用法一：修饰实例方法 — 锁的是 this
     */
    public synchronized void incrementSync() {
        count++;
    }

    /**
     * 用法二：修饰代码块 — 锁的是指定对象
     */
    public void incrementWithBlock() {
        synchronized (lock) {
            count++;
        }
    }

    /**
     * 用法三：修饰静态方法 — 锁的是 Class 对象
     */
    public static synchronized void staticSyncMethod() {
        // 锁的是 SynchronizedDemo.class
    }

    // ==================== 2. 可重入性演示 ====================

    /**
     * synchronized 是可重入锁：同一个线程可以多次获取同一把锁
     */
    private static void demonstrateReentrant() {
        System.out.println("--- 2. 可重入性演示 ---");
        SynchronizedDemo demo = new SynchronizedDemo();

        // 外层方法获取锁后，内层方法可以再次获取同一把锁
        synchronized (demo.lock) {
            System.out.println("外层获取锁");
            synchronized (demo.lock) {
                System.out.println("内层再次获取同一把锁 — 可重入！");
                synchronized (demo.lock) {
                    System.out.println("第三层再次获取 — 仍然可重入！");
                }
            }
        }

        // 继承场景的可重入
        new ChildClass().doSomething();
    }

    static class ParentClass {
        public synchronized void doSomething() {
            System.out.println("父类 synchronized 方法");
        }
    }

    static class ChildClass extends ParentClass {
        @Override
        public synchronized void doSomething() {
            System.out.println("子类 synchronized 方法，调用 super");
            super.doSomething(); // 可重入：子类锁和父类锁是同一个 this
        }
    }

    // ==================== 3. 锁升级观察 ====================

    /**
     * 锁升级过程演示（概念性演示）
     * <p>
     * 实际的锁升级发生在 JVM 内部，无法直接观察 Mark Word 变化。
     * 可以通过 JOL（Java Object Layout）工具查看对象头。
     * 添加 JVM 参数 -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 开启偏向锁。
     * </p>
     */
    private static void demonstrateLockEscalation() throws Exception {
        System.out.println("--- 3. 锁升级过程（概念演示） ---");
        Object lockObj = new Object();

        // 阶段 1：偏向锁 — 只有一个线程访问
        // 第一个线程获取锁时，通过 CAS 将 Mark Word 中的 ThreadID 设为当前线程
        // 之后同一线程再次进入时，只需检查 ThreadID，无需 CAS
        synchronized (lockObj) {
            System.out.println("阶段 1 — 偏向锁：单线程访问，ThreadID=" +
                    Thread.currentThread().threadId());
        }

        // 阶段 2：轻量级锁 — 两个线程交替访问（无竞争）
        // 在栈帧中创建 Lock Record，CAS 替换 Mark Word
        Thread t1 = new Thread(() -> {
            synchronized (lockObj) {
                System.out.println("阶段 2 — 轻量级锁：线程 " +
                        Thread.currentThread().getName() + " 获取锁");
            }
        }, "thread-1");
        t1.start();
        t1.join();

        // 阶段 3：重量级锁 — 多个线程同时竞争
        // 膨胀为 ObjectMonitor，未获取锁的线程进入 EntryList 被挂起
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 同时开始竞争
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockObj) {
                    System.out.println("阶段 3 — 重量级锁：线程 " + idx + " 获取锁");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                endLatch.countDown();
            }, "compete-" + i).start();
        }
        startLatch.countDown(); // 触发竞争
        endLatch.await();
    }

    // ==================== 4. 死锁演示 ====================

    /**
     * 演示死锁的产生
     */
    private static void demonstrateDeadlock() throws Exception {
        System.out.println("--- 4. 死锁演示（3 秒后自动退出） ---");
        Object lockA = new Object();
        Object lockB = new Object();

        Thread threadA = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("线程 A 持有锁 A，等待锁 B...");
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockB) {
                    System.out.println("线程 A 获取锁 B（不会执行到这里）");
                }
            }
        }, "deadlock-A");

        Thread threadB = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("线程 B 持有锁 B，等待锁 A...");
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockA) {
                    System.out.println("线程 B 获取锁 A（不会执行到这里）");
                }
            }
        }, "deadlock-B");

        threadA.setDaemon(true);
        threadB.setDaemon(true);
        threadA.start();
        threadB.start();

        // 等待 3 秒观察死锁
        Thread.sleep(3000);
        System.out.println("死锁已产生！线程 A 状态: " + threadA.getState()
                + ", 线程 B 状态: " + threadB.getState());
        System.out.println("提示：使用 jstack <PID> 可以检测到死锁信息");
    }
}

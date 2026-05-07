package com.example.concurrent.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁构造与检测演示
 * <p>
 * 演示内容：
 * 1. 构造一个经典死锁
 * 2. 使用 ThreadMXBean 编程检测死锁
 * 3. 使用 tryLock 避免死锁
 * 4. 固定加锁顺序避免死锁
 * </p>
 * <p>
 * jstack 使用说明：
 * 1. 运行本程序
 * 2. 打开终端执行: jps -l （找到进程 PID）
 * 3. 执行: jstack PID （查看线程堆栈，末尾会显示死锁信息）
 * </p>
 */
public class DeadlockDemo {

    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) throws Exception {
        System.out.println("========== 死锁构造与检测演示 ==========\n");

        demonstrateDeadlockDetection();
        System.out.println();

        demonstrateTryLockAvoidance();
        System.out.println();

        demonstrateOrderedLocking();
    }

    // ==================== 1. 死锁构造与检测 ====================

    /**
     * 构造死锁并使用 ThreadMXBean 检测
     */
    private static void demonstrateDeadlockDetection() throws Exception {
        System.out.println("--- 1. 死锁构造与 ThreadMXBean 检测 ---");

        // 构造死锁
        Thread threadA = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("[线程A] 持有锁 A，等待锁 B...");
                sleep(100);
                synchronized (lockB) {
                    System.out.println("[线程A] 获取锁 B（不会执行到这里）");
                }
            }
        }, "deadlock-thread-A");

        Thread threadB = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("[线程B] 持有锁 B，等待锁 A...");
                sleep(100);
                synchronized (lockA) {
                    System.out.println("[线程B] 获取锁 A（不会执行到这里）");
                }
            }
        }, "deadlock-thread-B");

        // 设为守护线程，避免程序无法退出
        threadA.setDaemon(true);
        threadB.setDaemon(true);
        threadA.start();
        threadB.start();

        // 等待死锁形成
        Thread.sleep(500);

        // 使用 ThreadMXBean 检测死锁
        System.out.println("\n使用 ThreadMXBean 检测死锁：");
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = mxBean.findDeadlockedThreads();

        if (deadlockedThreads != null) {
            System.out.println("检测到死锁！涉及 " + deadlockedThreads.length + " 个线程：");
            ThreadInfo[] infos = mxBean.getThreadInfo(deadlockedThreads, true, true);
            for (ThreadInfo info : infos) {
                System.out.println("  线程: " + info.getThreadName()
                        + " | 状态: " + info.getThreadState()
                        + " | 等待锁: " + info.getLockName()
                        + " | 持有者: " + info.getLockOwnerName());
            }
        } else {
            System.out.println("未检测到死锁");
        }

        System.out.println("\n提示：也可以使用以下命令检测死锁：");
        System.out.println("  jstack <PID>          — 查看线程堆栈（自动检测死锁）");
        System.out.println("  arthas: thread -b     — 直接定位死锁线程");
        System.out.println("  jconsole              — 图形化工具，线程 Tab 页检测死锁");
    }

    // ==================== 2. tryLock 避免死锁 ====================

    /**
     * 使用 ReentrantLock 的 tryLock 避免死锁
     * tryLock 可以设置超时时间，获取不到锁时不会永久阻塞
     */
    private static void demonstrateTryLockAvoidance() throws Exception {
        System.out.println("--- 2. tryLock 避免死锁 ---");

        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        Thread t1 = new Thread(() -> {
            boolean gotBothLocks = false;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("[线程1] 获取锁1成功");
                            sleep(50);
                            if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                                try {
                                    System.out.println("[线程1] 获取锁2成功 — 执行业务逻辑");
                                    gotBothLocks = true;
                                } finally {
                                    lock2.unlock();
                                }
                            } else {
                                System.out.println("[线程1] 获取锁2超时，释放锁1，重试...");
                            }
                        } finally {
                            lock1.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (gotBothLocks) break;
                sleep(50); // 退避后重试
            }
        }, "tryLock-1");

        Thread t2 = new Thread(() -> {
            boolean gotBothLocks = false;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("[线程2] 获取锁2成功");
                            sleep(50);
                            if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                                try {
                                    System.out.println("[线程2] 获取锁1成功 — 执行业务逻辑");
                                    gotBothLocks = true;
                                } finally {
                                    lock1.unlock();
                                }
                            } else {
                                System.out.println("[线程2] 获取锁1超时，释放锁2，重试...");
                            }
                        } finally {
                            lock2.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (gotBothLocks) break;
                sleep(50);
            }
        }, "tryLock-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("tryLock 方式：即使加锁顺序不同，也不会死锁");
    }

    // ==================== 3. 固定加锁顺序 ====================

    /**
     * 通过固定加锁顺序避免死锁
     * 按照对象的 hashCode 排序，始终先锁 hashCode 小的对象
     */
    private static void demonstrateOrderedLocking() throws Exception {
        System.out.println("--- 3. 固定加锁顺序避免死锁 ---");

        Object resource1 = new Object();
        Object resource2 = new Object();

        Thread t1 = new Thread(() -> {
            lockInOrder(resource1, resource2);
            System.out.println("[线程1] 安全获取两把锁，执行完毕");
        }, "ordered-1");

        Thread t2 = new Thread(() -> {
            lockInOrder(resource2, resource1); // 即使传入顺序不同
            System.out.println("[线程2] 安全获取两把锁，执行完毕");
        }, "ordered-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("固定加锁顺序：无论传入顺序如何，都按 hashCode 排序加锁");
    }

    /**
     * 按照对象 hashCode 排序加锁，保证全局一致的加锁顺序
     */
    private static void lockInOrder(Object obj1, Object obj2) {
        int hash1 = System.identityHashCode(obj1);
        int hash2 = System.identityHashCode(obj2);

        Object first, second;
        if (hash1 < hash2) {
            first = obj1;
            second = obj2;
        } else if (hash1 > hash2) {
            first = obj2;
            second = obj1;
        } else {
            // hash 相同时使用 tie-breaking 锁
            first = obj1;
            second = obj2;
        }

        synchronized (first) {
            synchronized (second) {
                // 安全地持有两把锁
                sleep(50);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

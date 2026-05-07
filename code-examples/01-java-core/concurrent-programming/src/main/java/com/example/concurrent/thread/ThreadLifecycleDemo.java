package com.example.concurrent.thread;

import java.util.concurrent.*;

/**
 * 线程生命周期演示
 * <p>
 * 演示内容：
 * 1. 线程的 6 种状态转换
 * 2. 创建线程的 4 种方式
 * 3. sleep/wait/join/interrupt 的使用
 * </p>
 *
 * @see java.lang.Thread.State
 */
public class ThreadLifecycleDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        System.out.println("========== 线程生命周期演示 ==========\n");

        demonstrateThreadStates();
        System.out.println();

        demonstrateFourWaysToCreateThread();
        System.out.println();

        demonstrateSleepVsWait();
        System.out.println();

        demonstrateInterrupt();
    }

    // ==================== 1. 线程 6 种状态演示 ====================

    /**
     * 演示线程的 6 种状态：NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED
     */
    private static void demonstrateThreadStates() throws Exception {
        System.out.println("--- 1. 线程 6 种状态演示 ---");

        // NEW 状态：线程创建但未启动
        Thread thread = new Thread(() -> {
            try {
                // 进入 TIMED_WAITING 状态
                Thread.sleep(100);

                // 尝试获取锁，可能进入 BLOCKED 状态
                synchronized (lock) {
                    // 进入 WAITING 状态
                    lock.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "demo-thread");

        // 状态：NEW
        System.out.println("创建后状态: " + thread.getState()); // NEW

        thread.start();
        // 状态：RUNNABLE
        System.out.println("启动后状态: " + thread.getState()); // RUNNABLE

        // 等待线程进入 sleep
        Thread.sleep(50);
        // 状态：TIMED_WAITING（因为 sleep）
        System.out.println("sleep 中状态: " + thread.getState()); // TIMED_WAITING

        // 先占住锁，让 demo-thread 进入 BLOCKED
        Thread blocker = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(200);
                    lock.notifyAll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "blocker-thread");

        // 等待 demo-thread 的 sleep 结束
        Thread.sleep(100);
        blocker.start();
        Thread.sleep(50);

        // demo-thread 可能在 BLOCKED（等待锁）或 WAITING（已获取锁并 wait）
        Thread.State state = thread.getState();
        System.out.println("等待锁/wait 状态: " + state); // BLOCKED 或 WAITING

        // 等待所有线程结束
        thread.join(1000);
        blocker.join(1000);

        // 状态：TERMINATED
        System.out.println("结束后状态: " + thread.getState()); // TERMINATED
    }

    // ==================== 2. 创建线程的 4 种方式 ====================

    /**
     * 演示创建线程的 4 种方式
     */
    private static void demonstrateFourWaysToCreateThread() throws Exception {
        System.out.println("--- 2. 创建线程的 4 种方式 ---");

        // 方式一：继承 Thread 类
        // 优点：简单直接
        // 缺点：Java 单继承限制，无法继承其他类
        Thread thread1 = new MyThread();
        thread1.start();
        thread1.join();

        // 方式二：实现 Runnable 接口
        // 优点：避免单继承限制，任务与线程分离
        // 缺点：无返回值
        Thread thread2 = new Thread(new MyRunnable(), "runnable-thread");
        thread2.start();
        thread2.join();

        // 方式三：实现 Callable 接口 + FutureTask
        // 优点：有返回值，可以抛出异常
        // 缺点：使用稍复杂
        FutureTask<String> futureTask = new FutureTask<>(new MyCallable());
        Thread thread3 = new Thread(futureTask, "callable-thread");
        thread3.start();
        String result = futureTask.get(); // 阻塞获取结果
        System.out.println("[Callable] 返回结果: " + result);

        // 方式四：线程池（生产环境推荐）
        // 优点：复用线程，统一管理，可控制并发数
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> future = executor.submit(() -> {
            System.out.println("[线程池] 线程: " + Thread.currentThread().getName());
            return "线程池任务完成";
        });
        System.out.println("[线程池] 返回结果: " + future.get());
        executor.shutdown();
    }

    /**
     * 方式一：继承 Thread 类
     */
    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("[Thread] 线程: " + getName());
        }
    }

    /**
     * 方式二：实现 Runnable 接口
     */
    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("[Runnable] 线程: " + Thread.currentThread().getName());
        }
    }

    /**
     * 方式三：实现 Callable 接口
     */
    static class MyCallable implements Callable<String> {
        @Override
        public String call() {
            System.out.println("[Callable] 线程: " + Thread.currentThread().getName());
            return "Callable 任务完成";
        }
    }

    // ==================== 3. sleep vs wait 对比 ====================

    /**
     * 演示 sleep() 和 wait() 的区别
     * - sleep: 不释放锁，到时间自动恢复
     * - wait: 释放锁，需要 notify 唤醒
     */
    private static void demonstrateSleepVsWait() throws Exception {
        System.out.println("--- 3. sleep vs wait 对比 ---");
        Object monitor = new Object();

        // sleep 不释放锁
        Thread sleepThread = new Thread(() -> {
            synchronized (monitor) {
                System.out.println("[sleep] 获取锁，开始 sleep");
                try {
                    Thread.sleep(200); // 不释放锁
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[sleep] sleep 结束，仍持有锁");
            }
        }, "sleep-thread");

        // wait 释放锁
        Thread waitThread = new Thread(() -> {
            synchronized (monitor) {
                System.out.println("[wait] 获取锁，开始 wait");
                try {
                    monitor.wait(200); // 释放锁
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[wait] wait 结束，重新获取锁");
            }
        }, "wait-thread");

        sleepThread.start();
        sleepThread.join();
        waitThread.start();
        waitThread.join();
    }

    // ==================== 4. 线程中断演示 ====================

    /**
     * 演示优雅停止线程的方式：interrupt + 检查中断标志
     */
    private static void demonstrateInterrupt() throws Exception {
        System.out.println("--- 4. 线程中断演示 ---");

        Thread worker = new Thread(() -> {
            int count = 0;
            // 通过检查中断标志来优雅退出
            while (!Thread.currentThread().isInterrupted()) {
                count++;
                if (count % 1000000 == 0) {
                    System.out.println("[interrupt] 已执行 " + count + " 次");
                }
                if (count >= 3000000) break; // 安全退出
            }
            System.out.println("[interrupt] 线程收到中断信号，优雅退出。共执行 " + count + " 次");
        }, "worker-thread");

        worker.start();
        Thread.sleep(10); // 让 worker 运行一会儿
        worker.interrupt(); // 发送中断信号
        worker.join();
        System.out.println("[interrupt] 线程已终止");
    }
}

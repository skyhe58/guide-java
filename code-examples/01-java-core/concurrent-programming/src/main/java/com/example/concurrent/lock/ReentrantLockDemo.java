package com.example.concurrent.lock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 用法演示
 * <p>
 * 演示内容：
 * 1. 公平锁 vs 非公平锁对比
 * 2. tryLock 超时获取锁
 * 3. Condition 实现生产者消费者
 * </p>
 */
public class ReentrantLockDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== ReentrantLock 用法演示 ==========\n");

        demonstrateFairVsNonFair();
        System.out.println();

        demonstrateTryLock();
        System.out.println();

        demonstrateProducerConsumer();
    }

    // ==================== 1. 公平锁 vs 非公平锁 ====================

    /**
     * 对比公平锁和非公平锁的行为差异
     * 公平锁：按照请求顺序获取锁，先到先得
     * 非公平锁：允许插队，吞吐量更高
     */
    private static void demonstrateFairVsNonFair() throws Exception {
        System.out.println("--- 1. 公平锁 vs 非公平锁 ---");

        // 公平锁测试
        System.out.println("\n[公平锁] 获取顺序：");
        testLockFairness(new ReentrantLock(true));

        // 非公平锁测试
        System.out.println("\n[非公平锁] 获取顺序：");
        testLockFairness(new ReentrantLock(false));
    }

    private static void testLockFairness(ReentrantLock lock) throws Exception {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                lock.lock();
                try {
                    System.out.println("  线程 " + idx + " 获取锁");
                } finally {
                    lock.unlock();
                }
                endLatch.countDown();
            }, "fair-test-" + i).start();
        }

        Thread.sleep(50); // 确保所有线程都在等待
        startLatch.countDown();
        endLatch.await();
    }

    // ==================== 2. tryLock 超时获取 ====================

    /**
     * tryLock 可以设置超时时间，避免死锁
     */
    private static void demonstrateTryLock() throws Exception {
        System.out.println("--- 2. tryLock 超时获取锁 ---");
        ReentrantLock lock = new ReentrantLock();

        // 线程 1 持有锁 2 秒
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("[holder] 持有锁，将持续 2 秒");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                System.out.println("[holder] 释放锁");
            }
        }, "holder");

        // 线程 2 尝试获取锁，超时 500ms
        Thread waiter = new Thread(() -> {
            try {
                Thread.sleep(100); // 确保 holder 先获取锁
                System.out.println("[waiter] 尝试获取锁（超时 500ms）...");
                boolean acquired = lock.tryLock(500, TimeUnit.MILLISECONDS);
                if (acquired) {
                    try {
                        System.out.println("[waiter] 获取锁成功");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("[waiter] 获取锁超时，执行降级逻辑");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiter");

        holder.start();
        waiter.start();
        holder.join();
        waiter.join();
    }

    // ==================== 3. Condition 生产者消费者 ====================

    /**
     * 使用 ReentrantLock + Condition 实现生产者消费者模式
     * <p>
     * 相比 wait/notify 的优势：
     * - 支持多个条件变量（notFull 和 notEmpty）
     * - 可以精确唤醒等待在特定条件上的线程
     * </p>
     */
    private static void demonstrateProducerConsumer() throws Exception {
        System.out.println("--- 3. Condition 生产者消费者 ---");

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);

        // 生产者
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    buffer.put(i);
                    System.out.println("[生产者] 生产: " + i + " | 队列大小: " + buffer.size());
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "producer");

        // 消费者
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(100); // 消费慢于生产，触发队列满
                    int item = buffer.take();
                    System.out.println("[消费者] 消费: " + item + " | 队列大小: " + buffer.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("生产者消费者演示完成");
    }

    /**
     * 基于 ReentrantLock + Condition 的有界缓冲区
     */
    static class BoundedBuffer<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();   // 队列未满条件
        private final Condition notEmpty = lock.newCondition();  // 队列非空条件

        BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        /**
         * 生产：队列满时等待 notFull 条件
         */
        void put(T item) throws InterruptedException {
            lock.lock();
            try {
                // 使用 while 而非 if，防止虚假唤醒
                while (queue.size() == capacity) {
                    notFull.await(); // 队列满，等待消费者消费
                }
                queue.offer(item);
                notEmpty.signal(); // 通知消费者：队列非空了
            } finally {
                lock.unlock();
            }
        }

        /**
         * 消费：队列空时等待 notEmpty 条件
         */
        T take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    notEmpty.await(); // 队列空，等待生产者生产
                }
                T item = queue.poll();
                notFull.signal(); // 通知生产者：队列未满了
                return item;
            } finally {
                lock.unlock();
            }
        }

        int size() {
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }
    }
}

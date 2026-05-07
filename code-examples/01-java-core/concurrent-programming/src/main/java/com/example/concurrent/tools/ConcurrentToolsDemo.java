package com.example.concurrent.tools;

import java.util.concurrent.*;

/**
 * 并发工具类演示
 * <p>
 * 演示内容：
 * 1. CountDownLatch — 等待多个任务完成
 * 2. CyclicBarrier — 多线程同步到达屏障点
 * 3. Semaphore — 控制并发访问数量
 * </p>
 */
public class ConcurrentToolsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 并发工具类演示 ==========\n");

        demonstrateCountDownLatch();
        System.out.println();

        demonstrateCyclicBarrier();
        System.out.println();

        demonstrateSemaphore();
    }

    // ==================== 1. CountDownLatch ====================

    /**
     * CountDownLatch 场景：模拟微服务启动时的健康检查
     * 主线程等待所有服务检查完成后再启动
     */
    private static void demonstrateCountDownLatch() throws Exception {
        System.out.println("--- 1. CountDownLatch — 微服务健康检查 ---");

        String[] services = {"用户服务", "订单服务", "支付服务", "库存服务"};
        CountDownLatch latch = new CountDownLatch(services.length);
        ExecutorService executor = Executors.newFixedThreadPool(services.length);

        long start = System.currentTimeMillis();

        for (String service : services) {
            executor.submit(() -> {
                try {
                    // 模拟不同服务的检查耗时
                    int checkTime = ThreadLocalRandom.current().nextInt(200, 800);
                    Thread.sleep(checkTime);
                    System.out.println("  ✓ " + service + " 健康检查通过 (" + checkTime + "ms)");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("  ✗ " + service + " 健康检查失败");
                } finally {
                    latch.countDown(); // 无论成功失败都要 countDown
                }
            });
        }

        // 主线程等待所有服务检查完成（带超时）
        boolean allReady = latch.await(5, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        if (allReady) {
            System.out.println("所有服务健康检查通过！总耗时: " + elapsed + "ms（并行执行）");
        } else {
            System.out.println("部分服务检查超时！");
        }

        executor.shutdown();
    }

    // ==================== 2. CyclicBarrier ====================

    /**
     * CyclicBarrier 场景：模拟多线程分阶段并行计算
     * 所有线程完成第一阶段后，一起进入第二阶段
     */
    private static void demonstrateCyclicBarrier() throws Exception {
        System.out.println("--- 2. CyclicBarrier — 分阶段并行计算 ---");

        int workerCount = 3;
        // barrierAction：所有线程到达屏障后执行的回调
        CyclicBarrier barrier = new CyclicBarrier(workerCount, () -> {
            System.out.println("  >>> 所有线程到达屏障点，进入下一阶段 <<<");
        });

        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch completionLatch = new CountDownLatch(workerCount);

        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    // 第一阶段
                    int time1 = ThreadLocalRandom.current().nextInt(100, 500);
                    Thread.sleep(time1);
                    System.out.println("  Worker-" + workerId + " 完成第一阶段 (" + time1 + "ms)");
                    barrier.await(); // 等待其他线程

                    // 第二阶段（CyclicBarrier 可重用）
                    int time2 = ThreadLocalRandom.current().nextInt(100, 500);
                    Thread.sleep(time2);
                    System.out.println("  Worker-" + workerId + " 完成第二阶段 (" + time2 + "ms)");
                    barrier.await(); // 再次等待

                    System.out.println("  Worker-" + workerId + " 全部完成");
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        completionLatch.await();
        System.out.println("所有阶段执行完毕（CyclicBarrier 可循环使用）");
        executor.shutdown();
    }

    // ==================== 3. Semaphore ====================

    /**
     * Semaphore 场景：模拟停车场（限制同时停车数量）
     */
    private static void demonstrateSemaphore() throws Exception {
        System.out.println("--- 3. Semaphore — 停车场限流 ---");

        int parkingSpaces = 3; // 停车位数量
        Semaphore semaphore = new Semaphore(parkingSpaces);
        int totalCars = 6;
        ExecutorService executor = Executors.newFixedThreadPool(totalCars);
        CountDownLatch latch = new CountDownLatch(totalCars);

        for (int i = 1; i <= totalCars; i++) {
            final int carId = i;
            executor.submit(() -> {
                try {
                    System.out.println("  🚗 车辆 " + carId + " 到达停车场"
                            + "（可用车位: " + semaphore.availablePermits() + "）");

                    semaphore.acquire(); // 获取停车位（许可）
                    System.out.println("  🅿️ 车辆 " + carId + " 停入车位"
                            + "（剩余车位: " + semaphore.availablePermits() + "）");

                    // 模拟停车时间
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));

                    System.out.println("  🚗 车辆 " + carId + " 驶离车位");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release(); // 释放停车位
                    latch.countDown();
                }
            });
        }

        latch.await();
        System.out.println("所有车辆已离开，可用车位: " + semaphore.availablePermits());
        executor.shutdown();
    }
}

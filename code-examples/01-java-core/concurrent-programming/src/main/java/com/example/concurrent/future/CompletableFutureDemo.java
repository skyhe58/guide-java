package com.example.concurrent.future;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * CompletableFuture 异步编程演示
 * <p>
 * 演示内容：
 * 1. 异步任务创建与链式调用
 * 2. 多任务组合（allOf / anyOf / thenCombine）
 * 3. 异常处理（exceptionally / handle / whenComplete）
 * </p>
 */
public class CompletableFutureDemo {

    // 自定义线程池（生产环境推荐，不要使用默认的 ForkJoinPool）
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws Exception {
        System.out.println("========== CompletableFuture 异步编程演示 ==========\n");

        try {
            demonstrateChaining();
            System.out.println();

            demonstrateMultiTaskCombination();
            System.out.println();

            demonstrateExceptionHandling();
            System.out.println();

            demonstrateRealWorldScenario();
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 1. 链式调用 ====================

    /**
     * 演示 thenApply / thenCompose / thenAccept / thenRun
     */
    private static void demonstrateChaining() throws Exception {
        System.out.println("--- 1. 链式调用 ---");

        // thenApply：同步转换结果（类似 Stream.map）
        String result1 = CompletableFuture
                .supplyAsync(() -> "Hello", executor)
                .thenApply(s -> s + " World")
                .thenApply(String::toUpperCase)
                .get();
        System.out.println("thenApply: " + result1);

        // thenCompose：异步转换（类似 Stream.flatMap）
        String result2 = CompletableFuture
                .supplyAsync(() -> 42, executor)
                .thenCompose(num -> CompletableFuture.supplyAsync(
                        () -> "数字是: " + num, executor))
                .get();
        System.out.println("thenCompose: " + result2);

        // thenAccept：消费结果（无返回值）
        CompletableFuture
                .supplyAsync(() -> "消费这个值", executor)
                .thenAccept(s -> System.out.println("thenAccept: " + s))
                .get();

        // thenRun：不关心结果，执行动作
        CompletableFuture
                .supplyAsync(() -> "忽略这个值", executor)
                .thenRun(() -> System.out.println("thenRun: 任务完成后的清理动作"))
                .get();
    }

    // ==================== 2. 多任务组合 ====================

    /**
     * 演示 allOf / anyOf / thenCombine
     */
    private static void demonstrateMultiTaskCombination() throws Exception {
        System.out.println("--- 2. 多任务组合 ---");

        // allOf：等待所有任务完成
        System.out.println("\n[allOf] 等待所有任务完成：");
        long start = System.currentTimeMillis();

        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "任务1完成";
        }, executor);

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "任务2完成";
        }, executor);

        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "任务3完成";
        }, executor);

        // 等待所有任务完成
        CompletableFuture.allOf(task1, task2, task3).join();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("  " + task1.get() + ", " + task2.get() + ", " + task3.get());
        System.out.println("  总耗时: " + elapsed + "ms（并行，取最长 ≈ 300ms）");

        // anyOf：任一任务完成即返回
        System.out.println("\n[anyOf] 任一任务完成即返回：");
        start = System.currentTimeMillis();

        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "慢任务"; }, executor),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "快任务"; }, executor),
                CompletableFuture.supplyAsync(() -> { sleep(200); return "中任务"; }, executor)
        );

        System.out.println("  最先完成: " + fastest.get()
                + "（耗时: " + (System.currentTimeMillis() - start) + "ms）");

        // thenCombine：合并两个任务的结果
        System.out.println("\n[thenCombine] 合并两个任务结果：");
        String combined = CompletableFuture
                .supplyAsync(() -> "用户信息", executor)
                .thenCombine(
                        CompletableFuture.supplyAsync(() -> "订单列表", executor),
                        (user, orders) -> user + " + " + orders
                ).get();
        System.out.println("  合并结果: " + combined);
    }

    // ==================== 3. 异常处理 ====================

    /**
     * 演示 exceptionally / handle / whenComplete
     */
    private static void demonstrateExceptionHandling() throws Exception {
        System.out.println("--- 3. 异常处理 ---");

        // exceptionally：异常时返回默认值
        String result1 = CompletableFuture
                .<String>supplyAsync(() -> {
                    throw new RuntimeException("模拟异常");
                }, executor)
                .exceptionally(ex -> {
                    System.out.println("[exceptionally] 捕获异常: " + ex.getMessage());
                    return "默认值";
                })
                .get();
        System.out.println("[exceptionally] 结果: " + result1);

        // handle：统一处理结果和异常
        String result2 = CompletableFuture
                .supplyAsync(() -> {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        throw new RuntimeException("随机异常");
                    }
                    return "正常结果";
                }, executor)
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[handle] 异常: " + ex.getMessage());
                        return "异常恢复值";
                    }
                    return result;
                })
                .get();
        System.out.println("[handle] 结果: " + result2);

        // whenComplete：完成时回调（不改变结果）
        CompletableFuture
                .supplyAsync(() -> "任务结果", executor)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[whenComplete] 异常: " + ex.getMessage());
                    } else {
                        System.out.println("[whenComplete] 成功: " + result);
                    }
                })
                .get();
    }

    // ==================== 4. 实战场景 ====================

    /**
     * 实战场景：电商商品详情页 — 并行查询多个服务
     */
    private static void demonstrateRealWorldScenario() throws Exception {
        System.out.println("--- 4. 实战场景：商品详情页并行查询 ---");

        long start = System.currentTimeMillis();

        // 并行查询多个服务
        CompletableFuture<String> productFuture = CompletableFuture
                .supplyAsync(() -> queryProduct("P001"), executor);

        CompletableFuture<String> priceFuture = CompletableFuture
                .supplyAsync(() -> queryPrice("P001"), executor);

        CompletableFuture<String> stockFuture = CompletableFuture
                .supplyAsync(() -> queryStock("P001"), executor);

        CompletableFuture<String> reviewFuture = CompletableFuture
                .supplyAsync(() -> queryReviews("P001"), executor)
                .exceptionally(ex -> "评价加载失败，显示默认"); // 评价服务降级

        // 等待所有查询完成
        CompletableFuture.allOf(productFuture, priceFuture, stockFuture, reviewFuture).join();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("商品详情页数据：");
        System.out.println("  商品: " + productFuture.get());
        System.out.println("  价格: " + priceFuture.get());
        System.out.println("  库存: " + stockFuture.get());
        System.out.println("  评价: " + reviewFuture.get());
        System.out.println("  总耗时: " + elapsed + "ms（并行查询，远小于串行的 1000ms）");
    }

    // ==================== 模拟服务调用 ====================

    private static String queryProduct(String id) {
        sleep(200);
        return "iPhone 15 Pro";
    }

    private static String queryPrice(String id) {
        sleep(150);
        return "¥7999";
    }

    private static String queryStock(String id) {
        sleep(300);
        return "库存充足 (128件)";
    }

    private static String queryReviews(String id) {
        sleep(250);
        return "4.8分 (2000+评价)";
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package com.example.concurrent.threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * ThreadLocal 原理演示
 * <p>
 * 演示内容：
 * 1. ThreadLocal 基本用法（用户上下文传递）
 * 2. ThreadLocal 解决 SimpleDateFormat 线程安全问题
 * 3. 内存泄漏风险演示
 * 4. InheritableThreadLocal 父子线程传递
 * </p>
 */
public class ThreadLocalDemo {

    // ==================== 用户上下文 ====================

    /**
     * 典型用法：线程级别的用户上下文
     */
    private static final ThreadLocal<String> USER_CONTEXT = new ThreadLocal<>();

    /**
     * 典型用法：线程安全的日期格式化
     * SimpleDateFormat 是线程不安全的，使用 ThreadLocal 为每个线程创建独立实例
     */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    /**
     * InheritableThreadLocal：父子线程传递
     */
    private static final InheritableThreadLocal<String> INHERITABLE =
            new InheritableThreadLocal<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========== ThreadLocal 原理演示 ==========\n");

        demonstrateBasicUsage();
        System.out.println();

        demonstrateDateFormat();
        System.out.println();

        demonstrateMemoryLeak();
        System.out.println();

        demonstrateInheritable();
    }

    // ==================== 1. 基本用法 ====================

    /**
     * 模拟 Web 请求中的用户上下文传递
     * 在 Filter 中设置用户信息，在 Service 层获取
     */
    private static void demonstrateBasicUsage() throws Exception {
        System.out.println("--- 1. ThreadLocal 基本用法 — 用户上下文 ---");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        String[] users = {"张三", "李四", "王五"};
        for (String user : users) {
            executor.submit(() -> {
                try {
                    // 模拟 Filter：设置用户上下文
                    USER_CONTEXT.set(user);
                    System.out.println("[" + Thread.currentThread().getName()
                            + "] 设置用户: " + user);

                    // 模拟 Service 层：获取用户上下文
                    processRequest();
                } finally {
                    // 必须在 finally 中清理！防止内存泄漏
                    USER_CONTEXT.remove();
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }

    private static void processRequest() {
        // 在任何地方都可以获取当前线程的用户信息，无需参数传递
        String user = USER_CONTEXT.get();
        System.out.println("[" + Thread.currentThread().getName()
                + "] Service 层获取用户: " + user);
    }

    // ==================== 2. 日期格式化 ====================

    /**
     * 使用 ThreadLocal 解决 SimpleDateFormat 线程安全问题
     */
    private static void demonstrateDateFormat() throws Exception {
        System.out.println("--- 2. ThreadLocal 解决 SimpleDateFormat 线程安全 ---");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    // 每个线程使用自己的 SimpleDateFormat 实例
                    String formatted = DATE_FORMAT.get().format(new Date());
                    System.out.println("[" + Thread.currentThread().getName()
                            + "] 格式化日期: " + formatted);
                } finally {
                    DATE_FORMAT.remove(); // 线程池场景下必须清理
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("提示：JDK 8+ 推荐使用 DateTimeFormatter（线程安全，无需 ThreadLocal）");
    }

    // ==================== 3. 内存泄漏演示 ====================

    /**
     * 演示 ThreadLocal 内存泄漏的风险
     * <p>
     * 内存泄漏原因：
     * - Entry 的 key 是 ThreadLocal 的弱引用
     * - ThreadLocal 被 GC 回收后，key 变为 null
     * - 但 value 仍被 Entry 强引用，无法回收
     * - 线程池中线程长期存活，这些 value 会越积越多
     * </p>
     */
    private static void demonstrateMemoryLeak() throws Exception {
        System.out.println("--- 3. 内存泄漏风险演示 ---");

        ExecutorService executor = Executors.newFixedThreadPool(1);

        // 错误用法：没有调用 remove()
        System.out.println("[错误用法] 不调用 remove()：");
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                ThreadLocal<byte[]> local = new ThreadLocal<>();
                local.set(new byte[1024]); // 分配 1KB
                System.out.println("  任务 " + taskId + " 设置了 ThreadLocal 值"
                        + "（未调用 remove，可能泄漏！）");
                // 没有调用 local.remove()！
            }).get(); // 等待完成
        }

        // 正确用法：在 finally 中调用 remove()
        System.out.println("[正确用法] 在 finally 中调用 remove()：");
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                ThreadLocal<byte[]> local = new ThreadLocal<>();
                try {
                    local.set(new byte[1024]);
                    System.out.println("  任务 " + taskId + " 设置了 ThreadLocal 值");
                } finally {
                    local.remove(); // 正确！防止内存泄漏
                    System.out.println("  任务 " + taskId + " 已清理 ThreadLocal ✓");
                }
            }).get();
        }

        executor.shutdown();
    }

    // ==================== 4. InheritableThreadLocal ====================

    /**
     * InheritableThreadLocal 可以将父线程的值传递给子线程
     * 注意：在线程池场景下会失效（线程复用，不会每次创建新线程）
     */
    private static void demonstrateInheritable() throws Exception {
        System.out.println("--- 4. InheritableThreadLocal 父子线程传递 ---");

        // 父线程设置值
        INHERITABLE.set("父线程的 TraceId: TRACE-001");
        System.out.println("[父线程] 设置: " + INHERITABLE.get());

        // 子线程可以获取父线程的值
        Thread child = new Thread(() -> {
            System.out.println("[子线程] 获取: " + INHERITABLE.get());

            // 子线程修改不影响父线程
            INHERITABLE.set("子线程修改的值");
            System.out.println("[子线程] 修改后: " + INHERITABLE.get());
        }, "child-thread");

        child.start();
        child.join();

        System.out.println("[父线程] 子线程修改后，父线程的值: " + INHERITABLE.get());
        System.out.println("提示：InheritableThreadLocal 在线程池中会失效，"
                + "推荐使用阿里 TransmittableThreadLocal");

        INHERITABLE.remove();
    }
}

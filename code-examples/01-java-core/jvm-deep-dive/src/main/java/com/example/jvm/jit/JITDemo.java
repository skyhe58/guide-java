package com.example.jvm.jit;

import java.util.ArrayList;
import java.util.List;

/**
 * JIT 编译器演示 — 逃逸分析 / 标量替换 / 锁消除 / 锁粗化
 *
 * <p>本示例演示 JIT（Just-In-Time）编译器的核心优化技术：
 * <ul>
 *   <li>逃逸分析：判断对象是否逃逸出方法/线程</li>
 *   <li>标量替换：将对象拆解为基本类型，在栈上分配</li>
 *   <li>锁消除：消除不可能存在竞争的同步操作</li>
 *   <li>锁粗化：合并连续的同步块，减少加锁/解锁开销</li>
 * </ul>
 *
 * <h3>JIT 编译流程：</h3>
 * <pre>
 *  ┌──────────┐    热点探测     ┌──────────┐    编译优化    ┌──────────┐
 *  │ 字节码    │──────────────→│ JIT 编译器│──────────────→│ 机器码    │
 *  │ 解释执行  │  方法调用计数器  │ C1(Client)│  逃逸分析     │ 直接执行  │
 *  │ (慢)     │  回边计数器     │ C2(Server)│  内联/展开    │ (快)     │
 *  └──────────┘               └──────────┘               └──────────┘
 *
 *  分层编译（Tiered Compilation, JDK 8+ 默认开启）：
 *  Level 0: 解释执行
 *  Level 1: C1 简单编译（无 profiling）
 *  Level 2: C1 有限 profiling
 *  Level 3: C1 完整 profiling
 *  Level 4: C2 完全优化（逃逸分析、标量替换等）
 * </pre>
 *
 * <h3>逃逸分析判定：</h3>
 * <pre>
 *  ┌─────────────────┐
 *  │ 对象在方法内创建  │
 *  └────────┬────────┘
 *           ↓
 *  ┌─────────────────┐   是   ┌──────────────┐
 *  │ 作为返回值返回？  │──────→│ 方法逃逸      │→ 堆上分配
 *  └────────┬────────┘       └──────────────┘
 *        否 ↓
 *  ┌─────────────────┐   是   ┌──────────────┐
 *  │ 赋值给类变量/     │──────→│ 线程逃逸      │→ 堆上分配
 *  │ 传递给其他线程？   │       └──────────────┘
 *  └────────┬────────┘
 *        否 ↓
 *  ┌──────────────────┐
 *  │ 未逃逸 → 可优化   │→ 栈上分配 / 标量替换 / 锁消除
 *  └──────────────────┘
 * </pre>
 *
 * @author JIT 编译器示例
 * @since 1.0
 */
public class JITDemo {

    // ==================== 一、逃逸分析 ====================

    /** 用于逃逸分析演示的简单坐标类 */
    static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        int distanceSquared() { return x * x + y * y; }
    }

    // 类变量 — 赋值给它的对象会发生线程逃逸
    static Point escapedPoint;

    /**
     * 演示1：逃逸分析 — 对象逃逸 vs 未逃逸。
     *
     * <p>JVM 参数：
     * <ul>
     *   <li>-XX:+DoEscapeAnalysis（默认开启，JDK 8+）</li>
     *   <li>-XX:-DoEscapeAnalysis（关闭逃逸分析，用于对比）</li>
     *   <li>-XX:+PrintEscapeAnalysis（打印逃逸分析结果，debug 版 JVM）</li>
     * </ul>
     */
    static void demoEscapeAnalysis() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：逃逸分析 — 对象逃逸 vs 未逃逸");
        System.out.println("═══════════════════════════════════════════════════");

        int iterations = 10_000_000;

        // --- 未逃逸的对象（可被优化） ---
        System.out.println("\n  【未逃逸对象 — 可被栈上分配/标量替换】");
        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.nanoTime();

        long sumNoEscape = 0;
        for (int i = 0; i < iterations; i++) {
            // Point 对象未逃逸出方法：不作为返回值，不赋给类变量
            // JIT 可能将其优化为栈上分配或标量替换
            Point p = new Point(i, i + 1);
            sumNoEscape += p.distanceSquared();
        }
        long noEscapeTime = System.nanoTime() - start;
        long memAfterNoEscape = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.printf("    创建 %d 百万个未逃逸 Point 对象%n", iterations / 1_000_000);
        System.out.printf("    耗时: %d ms%n", noEscapeTime / 1_000_000);
        System.out.printf("    内存增长: %d KB（逃逸分析优化后，增长很小）%n",
                (memAfterNoEscape - memBefore) / 1024);
        System.out.printf("    计算结果: %d%n", sumNoEscape);

        // --- 逃逸的对象（无法优化） ---
        System.out.println("\n  【逃逸对象 — 必须在堆上分配】");
        System.gc();
        memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.nanoTime();

        List<Point> escapedList = new ArrayList<>();
        int escapeCount = 100_000; // 减少数量避免 OOM
        for (int i = 0; i < escapeCount; i++) {
            // Point 对象逃逸到 List 中（方法逃逸）
            Point p = new Point(i, i + 1);
            escapedList.add(p); // 逃逸！对象被外部集合引用
        }
        long escapeTime = System.nanoTime() - start;
        long memAfterEscape = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.printf("    创建 %d 个逃逸 Point 对象（存入 List）%n", escapeCount);
        System.out.printf("    耗时: %d ms%n", escapeTime / 1_000_000);
        System.out.printf("    内存增长: %d KB（对象在堆上，内存增长明显）%n",
                (memAfterEscape - memBefore) / 1024);

        // 线程逃逸示例
        System.out.println("\n  【线程逃逸 — 赋值给类变量】");
        escapedPoint = new Point(100, 200);
        System.out.printf("    escapedPoint = Point(%d, %d)%n",
                escapedPoint.x, escapedPoint.y);
        System.out.println("    赋值给 static 变量 → 线程逃逸，无法栈上分配");

        // 逃逸类型总结
        System.out.println("\n  【逃逸类型总结】");
        System.out.println("    未逃逸:   对象仅在方法内使用 → 栈上分配/标量替换");
        System.out.println("    方法逃逸: 对象作为返回值或传参 → 堆上分配");
        System.out.println("    线程逃逸: 对象赋给类变量/其他线程可见 → 堆上分配，不能锁消除");
        System.out.println();
    }

    // ==================== 二、标量替换 ====================

    /**
     * 演示2：标量替换 — 对象拆解为基本类型。
     *
     * <pre>
     *  标量替换过程：
     *  原始代码:                    优化后:
     *  Point p = new Point(3, 4);  int p_x = 3;
     *  int d = p.x * p.x           int p_y = 4;
     *        + p.y * p.y;          int d = p_x * p_x + p_y * p_y;
     *
     *  标量（Scalar）: 不可再分的数据，如 int, long, double, reference
     *  聚合量（Aggregate）: 可再分的数据，如对象（包含多个标量）
     * </pre>
     *
     * <p>JVM 参数：-XX:+EliminateAllocations（默认开启）
     */
    static void demoScalarReplacement() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：标量替换 — 对象拆解为基本类型");
        System.out.println("═══════════════════════════════════════════════════");

        int iterations = 50_000_000;

        // --- 可被标量替换的代码 ---
        System.out.println("\n  【可被标量替换 — 对象拆解为 int x, int y】");
        long start = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < iterations; i++) {
            // JIT 优化后等价于: int x = i, y = i+1; sum1 += x*x + y*y;
            Point p = new Point(i, i + 1);
            sum1 += p.distanceSquared();
        }
        long scalarTime = System.nanoTime() - start;
        System.out.printf("    %d 百万次 Point 创建+计算: %d ms%n",
                iterations / 1_000_000, scalarTime / 1_000_000);

        // --- 直接用基本类型（手动标量替换） ---
        System.out.println("\n  【手动标量替换 — 直接用基本类型】");
        start = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < iterations; i++) {
            int x = i, y = i + 1;
            sum2 += x * x + y * y;
        }
        long primitiveTime = System.nanoTime() - start;
        System.out.printf("    %d 百万次基本类型计算:      %d ms%n",
                iterations / 1_000_000, primitiveTime / 1_000_000);

        // 对比
        System.out.printf("\n    结果一致: %s%n", sum1 == sum2);
        double ratio = (double) scalarTime / primitiveTime;
        System.out.printf("    性能比: %.2fx%n", ratio);
        if (ratio < 2.0) {
            System.out.println("    → JIT 标量替换生效，两者性能接近");
        } else {
            System.out.println("    → JIT 可能未充分优化（解释执行阶段）");
        }

        // 不能被标量替换的情况
        System.out.println("\n  【不能标量替换的情况】");
        System.out.println("    1. 对象逃逸出方法（作为返回值、存入集合）");
        System.out.println("    2. 对象字段太多（JIT 优化有复杂度限制）");
        System.out.println("    3. 对象有虚方法调用（多态无法内联）");
        System.out.println("    4. 逃逸分析被关闭（-XX:-DoEscapeAnalysis）");
        System.out.println();
    }

    // ==================== 三、锁消除 ====================

    /**
     * 演示3：锁消除 — 消除不必要的同步。
     *
     * <pre>
     *  锁消除条件：
     *  1. 逃逸分析确定锁对象未逃逸
     *  2. 锁对象是方法内的局部变量
     *  3. 不可能被其他线程访问
     *
     *  示例：
     *  void method() {
     *      Object lock = new Object();  // 局部变量，未逃逸
     *      synchronized(lock) {         // JIT 会消除这个 synchronized
     *          // ...
     *      }
     *  }
     *
     *  StringBuffer 的 append 方法是 synchronized 的，
     *  如果 StringBuffer 是局部变量，JIT 会消除锁。
     * </pre>
     *
     * <p>JVM 参数：-XX:+EliminateLocks（默认开启）
     */
    static void demoLockElimination() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：锁消除 — 消除不必要的同步");
        System.out.println("═══════════════════════════════════════════════════");

        int iterations = 1_000_000;

        // 预热 JIT
        for (int w = 0; w < 5; w++) {
            concatWithStringBuffer(iterations / 10);
            concatWithStringBuilder(iterations / 10);
        }

        // --- StringBuffer（synchronized，但局部变量可被锁消除） ---
        System.out.println("\n  【StringBuffer — synchronized 方法（可被锁消除）】");
        long start = System.nanoTime();
        String result1 = concatWithStringBuffer(iterations);
        long bufferTime = System.nanoTime() - start;
        System.out.printf("    StringBuffer %d 百万次 append: %d ms%n",
                iterations / 1_000_000, bufferTime / 1_000_000);

        // --- StringBuilder（无锁） ---
        System.out.println("\n  【StringBuilder — 无锁方法】");
        start = System.nanoTime();
        String result2 = concatWithStringBuilder(iterations);
        long builderTime = System.nanoTime() - start;
        System.out.printf("    StringBuilder %d 百万次 append: %d ms%n",
                iterations / 1_000_000, builderTime / 1_000_000);

        // 对比
        double ratio = (double) bufferTime / builderTime;
        System.out.printf("\n    性能比: %.2fx%n", ratio);
        if (ratio < 1.5) {
            System.out.println("    → 锁消除生效，StringBuffer ≈ StringBuilder");
        } else {
            System.out.println("    → 锁消除可能未完全生效（JIT 预热不足）");
        }

        // --- 对局部对象加锁（会被消除） ---
        System.out.println("\n  【局部对象加锁 — 会被 JIT 消除】");
        start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < iterations; i++) {
            Object localLock = new Object(); // 局部变量，未逃逸
            synchronized (localLock) {       // JIT 会消除此锁
                sum += i;
            }
        }
        long lockTime = System.nanoTime() - start;

        start = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < iterations; i++) {
            sum2 += i; // 无锁版本
        }
        long noLockTime = System.nanoTime() - start;

        System.out.printf("    有锁（局部对象）: %d ms, sum=%d%n", lockTime / 1_000_000, sum);
        System.out.printf("    无锁:           %d ms, sum=%d%n", noLockTime / 1_000_000, sum2);
        System.out.printf("    性能比: %.2fx（锁消除后应接近 1.0）%n",
                (double) lockTime / Math.max(noLockTime, 1));
        System.out.println();
    }

    /** StringBuffer 拼接（synchronized） */
    private static String concatWithStringBuffer(int count) {
        // StringBuffer 是局部变量，未逃逸 → JIT 可消除 append 的 synchronized
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            sb.append(i % 10);
        }
        return sb.toString();
    }

    /** StringBuilder 拼接（无锁） */
    private static String concatWithStringBuilder(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(i % 10);
        }
        return sb.toString();
    }

    // ==================== 四、锁粗化 ====================

    /**
     * 演示4：锁粗化 — 合并连续的同步块。
     *
     * <pre>
     *  锁粗化前：                    锁粗化后：
     *  synchronized(lock) {         synchronized(lock) {
     *      op1();                       op1();
     *  }                                op2();
     *  synchronized(lock) {             op3();
     *      op2();                   }
     *  }
     *  synchronized(lock) {
     *      op3();
     *  }
     *
     *  减少了 2 次加锁/解锁操作（锁的获取和释放有开销）
     * </pre>
     *
     * <p>JVM 参数：-XX:+EliminateLocks（锁粗化也受此参数控制）
     */
    static void demoLockCoarsening() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：锁粗化 — 合并连续的同步块");
        System.out.println("═══════════════════════════════════════════════════");

        int iterations = 1_000_000;
        Object sharedLock = new Object();

        // 预热
        for (int w = 0; w < 5; w++) {
            fineSyncOps(sharedLock, iterations / 10);
            coarseSyncOps(sharedLock, iterations / 10);
        }

        // --- 细粒度锁（多次加锁/解锁） ---
        System.out.println("\n  【细粒度锁 — 循环内多次 synchronized】");
        long start = System.nanoTime();
        long result1 = fineSyncOps(sharedLock, iterations);
        long fineTime = System.nanoTime() - start;
        System.out.printf("    %d 百万次（每次 3 个 synchronized）: %d ms%n",
                iterations / 1_000_000, fineTime / 1_000_000);

        // --- 粗粒度锁（手动合并） ---
        System.out.println("\n  【粗粒度锁 — 手动合并为 1 个 synchronized】");
        start = System.nanoTime();
        long result2 = coarseSyncOps(sharedLock, iterations);
        long coarseTime = System.nanoTime() - start;
        System.out.printf("    %d 百万次（每次 1 个 synchronized）: %d ms%n",
                iterations / 1_000_000, coarseTime / 1_000_000);

        // 对比
        System.out.printf("\n    结果一致: %s%n", result1 == result2);
        double ratio = (double) fineTime / Math.max(coarseTime, 1);
        System.out.printf("    性能比: %.2fx%n", ratio);
        if (ratio < 1.5) {
            System.out.println("    → JIT 锁粗化生效，细粒度锁被自动合并");
        } else {
            System.out.println("    → 细粒度锁开销明显，建议手动合并");
        }

        // JIT 优化参数总结
        System.out.println("\n  【JIT 优化相关 JVM 参数】");
        String[][] params = {
            {"参数", "默认值", "说明"},
            {"-XX:+DoEscapeAnalysis", "开启", "逃逸分析（标量替换/锁消除的前提）"},
            {"-XX:+EliminateAllocations", "开启", "标量替换（依赖逃逸分析）"},
            {"-XX:+EliminateLocks", "开启", "锁消除+锁粗化"},
            {"-XX:+TieredCompilation", "开启", "分层编译（C1+C2）"},
            {"-XX:CompileThreshold", "10000", "方法调用次数阈值（触发 JIT）"},
            {"-XX:+PrintCompilation", "关闭", "打印 JIT 编译日志"},
            {"-XX:+UnlockDiagnosticVMOptions", "关闭", "解锁诊断参数"},
        };
        for (int i = 0; i < params.length; i++) {
            System.out.printf("    %-35s %-8s %s%n", params[i][0], params[i][1], params[i][2]);
            if (i == 0) System.out.println("    " + "─".repeat(70));
        }
        System.out.println();
    }

    /** 细粒度锁：循环内多次 synchronized */
    private static long fineSyncOps(Object lock, int iterations) {
        long a = 0, b = 0, c = 0;
        for (int i = 0; i < iterations; i++) {
            synchronized (lock) { a += i; }       // 加锁1
            synchronized (lock) { b += i * 2; }   // 加锁2
            synchronized (lock) { c += i * 3; }   // 加锁3
        }
        return a + b + c;
    }

    /** 粗粒度锁：手动合并为一个 synchronized */
    private static long coarseSyncOps(Object lock, int iterations) {
        long a = 0, b = 0, c = 0;
        for (int i = 0; i < iterations; i++) {
            synchronized (lock) {   // 只加锁一次
                a += i;
                b += i * 2;
                c += i * 3;
            }
        }
        return a + b + c;
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  JIT 编译器演示 — 逃逸分析+标量替换+锁消除+锁粗化     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoEscapeAnalysis();
        demoScalarReplacement();
        demoLockElimination();
        demoLockCoarsening();
    }
}

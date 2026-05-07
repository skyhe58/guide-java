package com.example.jvm.memory;

/**
 * JVM 内存区域演示 — 堆/栈/方法区的实际内存分配与 OOM 触发
 *
 * <p>本示例通过实际代码演示 JVM 各内存区域的行为：
 * <ul>
 *   <li>堆（Heap）— 对象分配、数组分配、GC 触发</li>
 *   <li>虚拟机栈（VM Stack）— 栈帧结构、局部变量表、递归溢出</li>
 *   <li>方法区/元空间（Metaspace）— 类信息存储、常量池</li>
 *   <li>程序计数器（PC Register）— 线程私有</li>
 *   <li>直接内存（Direct Memory）— NIO ByteBuffer</li>
 *   <li>OOM 触发与排查</li>
 * </ul>
 *
 * <h3>JVM 内存结构：</h3>
 * <pre>
 *  线程私有                          线程共享
 *  ┌──────────────┐                ┌──────────────────────┐
 *  │ 程序计数器(PC) │                │ 堆（Heap）            │
 *  │ 虚拟机栈(Stack)│                │ ├── 新生代（Eden+S0+S1）│
 *  │ 本地方法栈     │                │ └── 老年代（Old）       │
 *  └──────────────┘                ├──────────────────────┤
 *                                  │ 方法区/元空间          │
 *                                  │ （类信息+常量池+静态变量）│
 *                                  └──────────────────────┘
 *                                  ┌──────────────────────┐
 *                                  │ 直接内存（堆外）        │
 *                                  └──────────────────────┘
 * </pre>
 */
public class MemoryAreaDemo {

    // ==================== 堆内存演示 ====================

    /** 演示1：堆内存 — 对象分配与 GC */
    static void demoHeapMemory() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：堆内存 — 对象分配与 GC");
        System.out.println("═══════════════════════════════════════════════════");

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;

        System.out.printf("\n  JVM 堆内存信息：%n");
        System.out.printf("    最大堆内存（-Xmx）: %d MB%n", maxMemory);
        System.out.printf("    当前堆大小（-Xms）: %d MB%n", totalMemory);
        System.out.printf("    空闲内存:           %d MB%n", freeMemory);
        System.out.printf("    已使用:             %d MB%n", totalMemory - freeMemory);

        // 分配对象观察内存变化
        System.out.println("\n  分配 10000 个对象观察内存变化：");
        long usedBefore = runtime.totalMemory() - runtime.freeMemory();
        Object[] objects = new Object[10000];
        for (int i = 0; i < 10000; i++) {
            objects[i] = new byte[1024]; // 每个 1KB
        }
        long usedAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("    分配前已使用: %d KB%n", usedBefore / 1024);
        System.out.printf("    分配后已使用: %d KB%n", usedAfter / 1024);
        System.out.printf("    增长: %d KB（约 10MB，符合 10000 × 1KB）%n", (usedAfter - usedBefore) / 1024);

        // 释放引用，触发 GC
        objects = null;
        System.gc();
        long usedAfterGC = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("    GC 后已使用: %d KB（内存回收）%n", usedAfterGC / 1024);
        System.out.println();
    }

    // ==================== 虚拟机栈演示 ====================

    /** 演示2：虚拟机栈 — 栈帧结构与递归溢出 */
    static void demoVMStack() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：虚拟机栈 — 栈帧结构与 StackOverflow");
        System.out.println("═══════════════════════════════════════════════════");

        // 栈帧结构说明
        System.out.println("\n  每个方法调用创建一个栈帧（Stack Frame）：");
        System.out.println("    ┌─────────────────────┐");
        System.out.println("    │ 局部变量表            │ ← 方法参数 + 局部变量");
        System.out.println("    │ 操作数栈              │ ← 计算过程的临时数据");
        System.out.println("    │ 动态链接              │ ← 指向运行时常量池的引用");
        System.out.println("    │ 方法返回地址           │ ← 方法执行完后返回的位置");
        System.out.println("    └─────────────────────┘");

        // 演示栈帧深度
        System.out.println("\n  测试最大递归深度（-Xss 默认 512KB~1MB）：");
        int maxDepth = testStackDepth(0);
        System.out.printf("    最大递归深度: %d 层%n", maxDepth);
        System.out.println("    每个栈帧约占: " + (512 * 1024 / maxDepth) + " 字节");

        // 局部变量对栈帧大小的影响
        System.out.println("\n  局部变量数量对栈深度的影响：");
        int depth1 = testStackDepthFewVars(0);
        int depth2 = testStackDepthManyVars(0);
        System.out.printf("    少量局部变量: 最大深度 %d%n", depth1);
        System.out.printf("    大量局部变量: 最大深度 %d（栈帧更大，深度更浅）%n", depth2);
        System.out.println();
    }

    /** 测试栈深度 */
    static int testStackDepth(int depth) {
        try {
            return testStackDepth(depth + 1);
        } catch (StackOverflowError e) {
            return depth;
        }
    }

    /** 少量局部变量的递归 */
    static int testStackDepthFewVars(int depth) {
        try {
            int a = 1;
            return testStackDepthFewVars(depth + 1);
        } catch (StackOverflowError e) {
            return depth;
        }
    }

    /** 大量局部变量的递归（栈帧更大） */
    static int testStackDepthManyVars(int depth) {
        try {
            long a = 1, b = 2, c = 3, d = 4, e = 5;
            long f = 6, g = 7, h = 8, i = 9, j = 10;
            long k = a + b + c + d + e + f + g + h + i + j;
            return testStackDepthManyVars(depth + 1);
        } catch (StackOverflowError e) {
            return depth;
        }
    }

    // ==================== 方法区/元空间演示 ====================

    /** 演示3：方法区/元空间 — 类信息与字符串常量池 */
    static void demoMetaspace() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：方法区/元空间 — 类信息与字符串常量池");
        System.out.println("═══════════════════════════════════════════════════");

        // 字符串常量池（JDK 7+ 移到堆中）
        System.out.println("\n  【字符串常量池】");
        String s1 = "hello";           // 常量池中的字符串
        String s2 = "hello";           // 复用常量池中的同一个对象
        String s3 = new String("hello"); // 堆中新建对象
        String s4 = s3.intern();       // intern() 返回常量池中的引用

        System.out.printf("    s1 == s2: %s（都指向常量池同一对象）%n", s1 == s2);
        System.out.printf("    s1 == s3: %s（s3 是堆中新对象）%n", s1 == s3);
        System.out.printf("    s1 == s4: %s（intern() 返回常量池引用）%n", s1 == s4);

        // 类信息
        System.out.println("\n  【类信息存储在元空间】");
        Class<?> clazz = MemoryAreaDemo.class;
        System.out.printf("    类名: %s%n", clazz.getName());
        System.out.printf("    类加载器: %s%n", clazz.getClassLoader());
        System.out.printf("    方法数: %d%n", clazz.getDeclaredMethods().length);
        System.out.printf("    字段数: %d%n", clazz.getDeclaredFields().length);

        // 元空间大小
        System.out.println("\n  元空间配置：");
        System.out.println("    -XX:MetaspaceSize=256m      初始大小");
        System.out.println("    -XX:MaxMetaspaceSize=512m   最大大小");
        System.out.println("    JDK 8+：方法区从永久代（PermGen）改为元空间（Metaspace）");
        System.out.println("    元空间使用本地内存（Native Memory），不在 JVM 堆中");
        System.out.println();
    }

    // ==================== 直接内存演示 ====================

    /** 演示4：直接内存 — NIO ByteBuffer */
    static void demoDirectMemory() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：直接内存 — NIO DirectByteBuffer");
        System.out.println("═══════════════════════════════════════════════════");

        // 堆内 ByteBuffer vs 直接 ByteBuffer
        System.out.println("\n  【堆内 ByteBuffer】");
        java.nio.ByteBuffer heapBuffer = java.nio.ByteBuffer.allocate(1024 * 1024); // 1MB
        System.out.printf("    allocate(1MB): isDirect=%s, capacity=%d%n",
                heapBuffer.isDirect(), heapBuffer.capacity());

        System.out.println("\n  【直接 ByteBuffer（堆外内存）】");
        java.nio.ByteBuffer directBuffer = java.nio.ByteBuffer.allocateDirect(1024 * 1024); // 1MB
        System.out.printf("    allocateDirect(1MB): isDirect=%s, capacity=%d%n",
                directBuffer.isDirect(), directBuffer.capacity());

        // 性能对比
        System.out.println("\n  【性能对比】写入 100 万次：");
        int iterations = 1_000_000;

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            heapBuffer.clear();
            heapBuffer.putInt(i);
        }
        long heapTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            directBuffer.clear();
            directBuffer.putInt(i);
        }
        long directTime = System.nanoTime() - start;

        System.out.printf("    堆内 Buffer: %d ms%n", heapTime / 1_000_000);
        System.out.printf("    直接 Buffer: %d ms%n", directTime / 1_000_000);
        System.out.println("    直接内存优势：减少一次内存拷贝（零拷贝），适合 I/O 密集场景");

        System.out.println("\n  直接内存配置：");
        System.out.println("    -XX:MaxDirectMemorySize=256m");
        System.out.println("    注意：直接内存不受 -Xmx 限制，但受物理内存限制");
        System.out.println();
    }

    // ==================== OOM 演示 ====================

    /** 演示5：各种 OOM 场景（仅说明，不实际触发） */
    static void demoOOMScenarios() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：常见 OOM 场景与排查");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【堆内存溢出】java.lang.OutOfMemoryError: Java heap space");
        System.out.println("    原因：对象太多或太大，堆放不下");
        System.out.println("    触发：List 无限添加、大数组、内存泄漏");
        System.out.println("    排查：-XX:+HeapDumpOnOutOfMemoryError → MAT 分析");

        System.out.println("\n  【栈溢出】java.lang.StackOverflowError");
        System.out.println("    原因：递归太深或方法调用链太长");
        System.out.println("    触发：无终止条件的递归");
        System.out.println("    排查：检查递归逻辑，增大 -Xss");

        System.out.println("\n  【元空间溢出】java.lang.OutOfMemoryError: Metaspace");
        System.out.println("    原因：加载的类太多（动态代理、CGLIB、JSP）");
        System.out.println("    触发：大量动态生成类");
        System.out.println("    排查：增大 -XX:MaxMetaspaceSize，检查类加载器泄漏");

        System.out.println("\n  【直接内存溢出】java.lang.OutOfMemoryError: Direct buffer memory");
        System.out.println("    原因：NIO DirectByteBuffer 分配过多");
        System.out.println("    排查：增大 -XX:MaxDirectMemorySize，检查 Buffer 是否及时释放");

        System.out.println("\n  【GC 开销过大】java.lang.OutOfMemoryError: GC overhead limit exceeded");
        System.out.println("    原因：GC 占用 98% 以上时间，回收不到 2% 内存");
        System.out.println("    排查：检查内存泄漏，增大堆内存");

        // 实际触发堆 OOM 的代码（注释掉，避免影响其他演示）
        System.out.println("\n  触发堆 OOM 的代码（已注释，可取消注释测试）：");
        System.out.println("    // 运行参数：-Xmx32m");
        System.out.println("    // List<byte[]> list = new ArrayList<>();");
        System.out.println("    // while (true) { list.add(new byte[1024 * 1024]); }");
        System.out.println();
    }

    // ==================== MXBean 监控 ====================

    /** 演示6：通过 MXBean 获取 JVM 运行时信息 */
    static void demoMXBean() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示6：MXBean — JVM 运行时监控");
        System.out.println("═══════════════════════════════════════════════════");

        // 内存 MXBean
        java.lang.management.MemoryMXBean memoryMXBean = java.lang.management.ManagementFactory.getMemoryMXBean();
        java.lang.management.MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        java.lang.management.MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        System.out.println("\n  【堆内存】");
        System.out.printf("    初始: %d MB%n", heapUsage.getInit() / 1024 / 1024);
        System.out.printf("    已用: %d MB%n", heapUsage.getUsed() / 1024 / 1024);
        System.out.printf("    提交: %d MB%n", heapUsage.getCommitted() / 1024 / 1024);
        System.out.printf("    最大: %d MB%n", heapUsage.getMax() / 1024 / 1024);

        System.out.println("\n  【非堆内存（元空间+代码缓存）】");
        System.out.printf("    已用: %d MB%n", nonHeapUsage.getUsed() / 1024 / 1024);

        // 线程 MXBean
        java.lang.management.ThreadMXBean threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean();
        System.out.println("\n  【线程信息】");
        System.out.printf("    当前线程数: %d%n", threadMXBean.getThreadCount());
        System.out.printf("    峰值线程数: %d%n", threadMXBean.getPeakThreadCount());
        System.out.printf("    守护线程数: %d%n", threadMXBean.getDaemonThreadCount());

        // GC MXBean
        System.out.println("\n  【GC 信息】");
        for (java.lang.management.GarbageCollectorMXBean gcBean :
                java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("    %s: 次数=%d, 总耗时=%dms%n",
                    gcBean.getName(), gcBean.getCollectionCount(), gcBean.getCollectionTime());
        }

        // 运行时信息
        java.lang.management.RuntimeMXBean runtimeMXBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
        System.out.println("\n  【运行时信息】");
        System.out.printf("    JVM: %s%n", runtimeMXBean.getVmName());
        System.out.printf("    版本: %s%n", runtimeMXBean.getVmVersion());
        System.out.printf("    启动时间: %d ms%n", runtimeMXBean.getUptime());
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  JVM 内存区域演示 — 堆/栈/方法区/直接内存               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoHeapMemory();
        demoVMStack();
        demoMetaspace();
        demoDirectMemory();
        demoOOMScenarios();
        demoMXBean();
    }
}

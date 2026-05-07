package com.example.jvm.tuning;

import java.lang.management.*;
import java.util.*;

/**
 * JVM 调优演示 — MXBean 实际监控 + GC 日志解读 + 内存分配策略验证
 *
 * <p>本示例通过实际代码演示 JVM 调优的核心技术：
 * <ul>
 *   <li>MXBean 监控：MemoryMXBean / ThreadMXBean / GCMXBean 实时数据采集</li>
 *   <li>内存分配策略：TLAB 分配、Eden 区分配、大对象直接进入老年代</li>
 *   <li>GC 日志解读：GC 日志格式解析与关键指标提取</li>
 *   <li>调优参数：常用 JVM 参数对比与推荐配置</li>
 * </ul>
 *
 * <h3>JVM 调优流程：</h3>
 * <pre>
 *  ┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
 *  │ 监控采集 │───→│ 问题定位 │───→│ 参数调整 │───→│ 效果验证 │
 *  │ MXBean  │    │ GC 日志  │    │ JVM 参数 │    │ 压测对比 │
 *  │ JMX     │    │ Heap Dump│    │ 堆/栈/GC │    │ 指标对比 │
 *  └─────────┘    └──────────┘    └──────────┘    └──────────┘
 *
 *  调优目标三角：
 *       吞吐量（Throughput）
 *          /\
 *         /  \
 *        /    \
 *       /______\
 *  延迟(Latency)  内存占用(Footprint)
 * </pre>
 *
 * @author JVM 调优示例
 * @since 1.0
 */
public class TuningDemo {

    // ==================== 一、MXBean 实际监控 ====================

    /**
     * 演示1：通过 MXBean 采集 JVM 运行时数据。
     *
     * <p>MXBean 是 JVM 内置的管理接口，可获取：
     * <ul>
     *   <li>MemoryMXBean — 堆/非堆内存使用情况</li>
     *   <li>ThreadMXBean — 线程数、死锁检测、CPU 时间</li>
     *   <li>GarbageCollectorMXBean — GC 次数与耗时</li>
     *   <li>MemoryPoolMXBean — 各内存池（Eden/Survivor/Old）详情</li>
     * </ul>
     */
    static void demoMXBeanMonitoring() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：MXBean 实际监控 — 采集 JVM 运行时数据");
        System.out.println("═══════════════════════════════════════════════════");

        // --- 内存 MXBean ---
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

        System.out.println("\n  【MemoryMXBean — 内存概览】");
        System.out.printf("    堆内存  → init: %dMB, used: %dMB, committed: %dMB, max: %dMB%n",
                toMB(heap.getInit()), toMB(heap.getUsed()),
                toMB(heap.getCommitted()), toMB(heap.getMax()));
        System.out.printf("    非堆内存 → used: %dMB, committed: %dMB%n",
                toMB(nonHeap.getUsed()), toMB(nonHeap.getCommitted()));

        // 使用率计算（调优关键指标）
        double heapUsageRatio = (double) heap.getUsed() / heap.getMax() * 100;
        System.out.printf("    堆使用率: %.1f%%%n", heapUsageRatio);

        // --- 各内存池详情 ---
        System.out.println("\n  【MemoryPoolMXBean — 各内存池详情】");
        System.out.printf("    %-25s %-10s %-10s %-10s %-8s%n",
                "内存池", "已用(MB)", "提交(MB)", "最大(MB)", "使用率");
        System.out.println("    " + "─".repeat(65));

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            if (usage == null) continue;
            long max = usage.getMax() > 0 ? usage.getMax() : usage.getCommitted();
            double ratio = max > 0 ? (double) usage.getUsed() / max * 100 : 0;
            System.out.printf("    %-25s %-10d %-10d %-10d %.1f%%%n",
                    pool.getName(), toMB(usage.getUsed()),
                    toMB(usage.getCommitted()), toMB(max), ratio);
        }

        // --- 线程 MXBean ---
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        System.out.println("\n  【ThreadMXBean — 线程信息】");
        System.out.printf("    当前活跃线程: %d%n", threadBean.getThreadCount());
        System.out.printf("    峰值线程数:   %d%n", threadBean.getPeakThreadCount());
        System.out.printf("    守护线程数:   %d%n", threadBean.getDaemonThreadCount());
        System.out.printf("    累计启动数:   %d%n", threadBean.getTotalStartedThreadCount());

        // 线程 CPU 时间（需要开启）
        if (threadBean.isThreadCpuTimeSupported()) {
            long currentThreadCpu = threadBean.getCurrentThreadCpuTime();
            long currentThreadUser = threadBean.getCurrentThreadUserTime();
            System.out.printf("    当前线程 CPU 时间: %d ms%n", currentThreadCpu / 1_000_000);
            System.out.printf("    当前线程用户时间: %d ms%n", currentThreadUser / 1_000_000);
        }

        // 死锁检测
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        System.out.printf("    死锁线程: %s%n",
                deadlockedThreads == null ? "无" : deadlockedThreads.length + " 个");

        // --- GC MXBean ---
        System.out.println("\n  【GarbageCollectorMXBean — GC 统计】");
        System.out.printf("    %-25s %-10s %-12s %-15s%n",
                "收集器", "GC 次数", "总耗时(ms)", "管理内存池");
        System.out.println("    " + "─".repeat(65));
        long totalGcCount = 0, totalGcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGcCount += gc.getCollectionCount();
            totalGcTime += gc.getCollectionTime();
            System.out.printf("    %-25s %-10d %-12d %s%n",
                    gc.getName(), gc.getCollectionCount(),
                    gc.getCollectionTime(), String.join(", ", gc.getMemoryPoolNames()));
        }

        // GC 吞吐量计算
        RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
        long uptime = rtBean.getUptime();
        double gcThroughput = uptime > 0 ? (1.0 - (double) totalGcTime / uptime) * 100 : 100;
        System.out.printf("\n    GC 总次数: %d, 总耗时: %dms, JVM 运行: %dms%n",
                totalGcCount, totalGcTime, uptime);
        System.out.printf("    GC 吞吐量: %.2f%%（目标 > 95%%）%n", gcThroughput);
        System.out.println();
    }

    // ==================== 二、内存分配策略验证 ====================

    /**
     * 演示2：内存分配策略 — TLAB / Eden / 大对象直接进老年代。
     *
     * <pre>
     *  对象分配流程：
     *  ┌──────────┐   是   ┌──────────┐   是   ┌──────────┐
     *  │ 尝试栈上  │──────→│ 栈上分配  │       │ 标量替换  │
     *  │ 分配(逃逸 │  否   └──────────┘       └──────────┘
     *  │ 分析)    │───┐
     *  └──────────┘   │
     *                 ↓
     *  ┌──────────┐   是   ┌──────────┐
     *  │ 大对象？  │──────→│ 老年代    │
     *  │(>阈值)   │  否   └──────────┘
     *  └──────────┘───┐
     *                 ↓
     *  ┌──────────┐   是   ┌──────────┐
     *  │ TLAB 有  │──────→│ TLAB 分配 │
     *  │ 空间？   │  否   └──────────┘
     *  └──────────┘───┐
     *                 ↓
     *  ┌──────────┐
     *  │ Eden CAS │
     *  │ 分配     │
     *  └──────────┘
     * </pre>
     */
    static void demoMemoryAllocationStrategy() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：内存分配策略 — TLAB / Eden / 大对象");
        System.out.println("═══════════════════════════════════════════════════");

        Runtime rt = Runtime.getRuntime();

        // --- TLAB 分配验证 ---
        // TLAB（Thread Local Allocation Buffer）是每个线程在 Eden 区的私有缓冲区
        // 避免多线程分配时的 CAS 竞争
        System.out.println("\n  【TLAB 分配 — 线程私有缓冲区】");
        System.out.println("    TLAB 默认开启: -XX:+UseTLAB（JDK 默认开启）");
        System.out.println("    TLAB 大小: -XX:TLABSize=512k（默认由 JVM 动态调整）");

        // 多线程并发分配，验证 TLAB 减少竞争
        int threadCount = 4;
        int allocPerThread = 100_000;
        long[] threadAllocTimes = new long[threadCount];
        Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int idx = t;
            threads[t] = new Thread(() -> {
                long start = System.nanoTime();
                Object[] objs = new Object[allocPerThread];
                for (int i = 0; i < allocPerThread; i++) {
                    objs[i] = new byte[64]; // 小对象，走 TLAB 快速分配
                }
                threadAllocTimes[idx] = System.nanoTime() - start;
            }, "alloc-thread-" + t);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.printf("    %d 线程各分配 %d 个小对象（64B）：%n", threadCount, allocPerThread);
        for (int i = 0; i < threadCount; i++) {
            System.out.printf("      线程 %d: %d ms%n", i, threadAllocTimes[i] / 1_000_000);
        }

        // --- Eden 区分配验证 ---
        System.out.println("\n  【Eden 区分配 — 指针碰撞】");
        System.gc(); // 清理，获取基线
        long usedBefore = rt.totalMemory() - rt.freeMemory();

        // 分配一批中等对象（在 Eden 区）
        int objCount = 1000;
        int objSize = 1024; // 1KB
        Object[] edenObjects = new Object[objCount];
        for (int i = 0; i < objCount; i++) {
            edenObjects[i] = new byte[objSize];
        }

        long usedAfter = rt.totalMemory() - rt.freeMemory();
        long allocated = usedAfter - usedBefore;
        System.out.printf("    分配 %d 个 %dB 对象%n", objCount, objSize);
        System.out.printf("    预期分配: %d KB%n", objCount * objSize / 1024);
        System.out.printf("    实际增长: %d KB（含对象头等开销）%n", allocated / 1024);

        // --- 大对象直接进入老年代 ---
        System.out.println("\n  【大对象直接进入老年代】");
        System.out.println("    参数: -XX:PretenureSizeThreshold=1048576（1MB）");
        System.out.println("    注意: 此参数仅对 Serial 和 ParNew 收集器有效");

        // 采集 GC 前后的老年代数据
        List<MemoryPoolMXBean> oldGenPools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().contains("Old") || pool.getName().contains("Tenured")) {
                oldGenPools.add(pool);
            }
        }

        if (!oldGenPools.isEmpty()) {
            MemoryPoolMXBean oldGen = oldGenPools.get(0);
            long oldUsedBefore = oldGen.getUsage().getUsed();

            // 分配大对象（可能直接进入老年代，取决于 GC 策略）
            byte[] largeObj = new byte[4 * 1024 * 1024]; // 4MB
            largeObj[0] = 1; // 防止被优化掉

            long oldUsedAfterAlloc = oldGen.getUsage().getUsed();
            System.out.printf("    分配 4MB 大对象前老年代: %d KB%n", oldUsedBefore / 1024);
            System.out.printf("    分配 4MB 大对象后老年代: %d KB%n", oldUsedAfterAlloc / 1024);
            System.out.printf("    老年代增长: %d KB%n", (oldUsedAfterAlloc - oldUsedBefore) / 1024);
        }
        System.out.println();
    }

    // ==================== 三、GC 日志解读 ====================

    /**
     * 演示3：GC 日志格式解析。
     *
     * <p>开启 GC 日志的 JVM 参数：
     * <pre>
     *  JDK 8:  -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log
     *  JDK 9+: -Xlog:gc*:file=gc.log:time,uptime,level,tags
     * </pre>
     */
    static void demoGCLogAnalysis() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：GC 日志格式解读");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟 GC 日志行并解析
        System.out.println("\n  【JDK 8 GC 日志格式】");
        String gcLog = "[GC (Allocation Failure) [PSYoungGen: 65536K->10752K(76288K)] 65536K->15848K(251392K), 0.0123456 secs]";
        System.out.println("    " + gcLog);
        System.out.println("\n    解析：");
        System.out.println("    ┌─ GC 类型: GC（Minor GC）");
        System.out.println("    ├─ 触发原因: Allocation Failure（Eden 区满）");
        System.out.println("    ├─ 年轻代: PSYoungGen 65536K → 10752K（容量 76288K）");
        System.out.println("    ├─ 整个堆: 65536K → 15848K（容量 251392K）");
        System.out.println("    └─ 耗时: 0.0123456 秒（12.3ms）");

        // 关键指标计算
        long youngBefore = 65536, youngAfter = 10752;
        long heapBefore = 65536, heapAfter = 15848;
        long youngReclaimed = youngBefore - youngAfter;
        long promoted = (heapAfter - heapBefore) + youngReclaimed - (heapBefore - heapAfter);

        System.out.println("\n    关键指标：");
        System.out.printf("      年轻代回收: %dK → %dK（回收 %dK）%n",
                youngBefore, youngAfter, youngReclaimed);
        System.out.printf("      晋升到老年代: %dK%n", (heapAfter - youngAfter));

        // Full GC 日志
        System.out.println("\n  【Full GC 日志格式】");
        String fullGcLog = "[Full GC (Ergonomics) [PSYoungGen: 10752K->0K(76288K)] "
                + "[ParOldGen: 120000K->85000K(175104K)] 130752K->85000K(251392K), "
                + "[Metaspace: 3500K->3500K(1056768K)], 0.2345678 secs]";
        System.out.println("    " + fullGcLog);
        System.out.println("\n    解析：");
        System.out.println("    ┌─ Full GC: 整堆回收（STW 时间长！）");
        System.out.println("    ├─ 年轻代: 10752K → 0K（全部清空）");
        System.out.println("    ├─ 老年代: 120000K → 85000K（回收 35MB）");
        System.out.println("    ├─ 元空间: 3500K → 3500K（未回收）");
        System.out.println("    └─ 耗时: 0.2345678 秒（234ms，需要关注！）");

        // JDK 9+ 统一日志格式
        System.out.println("\n  【JDK 9+ 统一日志格式（-Xlog:gc*）】");
        System.out.println("    [2024-01-15T10:30:45.123+0800][info][gc] GC(5) Pause Young");
        System.out.println("    (Normal) (G1 Evacuation Pause) 256M->128M(512M) 15.234ms");
        System.out.println("\n    解析：时间戳 + 级别 + GC 编号 + 类型 + 堆变化 + 耗时");

        // 触发一次 GC 并采集实际数据
        System.out.println("\n  【实际 GC 数据采集】");
        Map<String, long[]> gcBefore = captureGCStats();

        // 制造一些垃圾触发 GC
        for (int i = 0; i < 100; i++) {
            byte[] garbage = new byte[1024 * 1024]; // 1MB 垃圾
        }
        System.gc();

        Map<String, long[]> gcAfter = captureGCStats();
        for (String name : gcAfter.keySet()) {
            long[] before = gcBefore.getOrDefault(name, new long[]{0, 0});
            long[] after = gcAfter.get(name);
            System.out.printf("    %s: 次数 %d→%d（+%d）, 耗时 %dms→%dms（+%dms）%n",
                    name, before[0], after[0], after[0] - before[0],
                    before[1], after[1], after[1] - before[1]);
        }
        System.out.println();
    }

    // ==================== 四、调优参数对比 ====================

    /**
     * 演示4：常用 JVM 调优参数对比表。
     *
     * <pre>
     *  调优参数分类：
     *  ┌──────────────┐
     *  │ 堆内存参数    │ -Xms, -Xmx, -Xmn, -XX:NewRatio
     *  ├──────────────┤
     *  │ GC 参数      │ -XX:+UseG1GC, -XX:MaxGCPauseMillis
     *  ├──────────────┤
     *  │ 栈参数       │ -Xss
     *  ├──────────────┤
     *  │ 元空间参数    │ -XX:MetaspaceSize, -XX:MaxMetaspaceSize
     *  ├──────────────┤
     *  │ 诊断参数     │ -XX:+HeapDumpOnOutOfMemoryError
     *  └──────────────┘
     * </pre>
     */
    static void demoTuningParameters() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：常用 JVM 调优参数对比");
        System.out.println("═══════════════════════════════════════════════════");

        // 参数对比表
        String[][] params = {
            {"参数", "说明", "推荐值", "场景"},
            {"-Xms", "初始堆大小", "与-Xmx相同", "避免堆动态扩缩"},
            {"-Xmx", "最大堆大小", "物理内存50-70%", "根据应用需求"},
            {"-Xmn", "年轻代大小", "堆的1/3~1/2", "对象存活率低时增大"},
            {"-Xss", "线程栈大小", "256k~1m", "递归深/线程多时调整"},
            {"-XX:NewRatio", "老年代/年轻代比", "2(默认)", "2表示Old:Young=2:1"},
            {"-XX:SurvivorRatio", "Eden/Survivor比", "8(默认)", "8表示Eden:S0:S1=8:1:1"},
            {"-XX:MaxTenuringThreshold", "晋升年龄阈值", "15(默认)", "CMS默认6"},
            {"-XX:MetaspaceSize", "元空间初始大小", "256m", "避免频繁Full GC"},
            {"-XX:MaxMetaspaceSize", "元空间最大值", "512m", "防止无限增长"},
            {"-XX:+UseG1GC", "使用G1收集器", "JDK9+默认", "大内存低延迟"},
            {"-XX:MaxGCPauseMillis", "GC最大停顿目标", "200(默认)", "G1会尽量满足"},
            {"-XX:+HeapDumpOnOOM", "OOM时自动dump", "建议开启", "生产环境必备"},
        };

        System.out.println();
        for (int i = 0; i < params.length; i++) {
            System.out.printf("    %-30s %-18s %-16s %s%n",
                    params[i][0], params[i][1], params[i][2], params[i][3]);
            if (i == 0) System.out.println("    " + "─".repeat(85));
        }

        // 当前 JVM 实际参数
        System.out.println("\n  【当前 JVM 实际参数】");
        RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
        List<String> vmArgs = rtBean.getInputArguments();
        if (vmArgs.isEmpty()) {
            System.out.println("    （未设置自定义 JVM 参数，使用默认值）");
        } else {
            for (String arg : vmArgs) {
                System.out.printf("    %s%n", arg);
            }
        }

        // 推荐配置模板
        System.out.println("\n  【推荐配置模板】");
        System.out.println("    --- 4C8G 服务器（Web 应用）---");
        System.out.println("    -Xms4g -Xmx4g -Xmn2g");
        System.out.println("    -XX:+UseG1GC -XX:MaxGCPauseMillis=200");
        System.out.println("    -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m");
        System.out.println("    -XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("    -XX:HeapDumpPath=/tmp/heapdump.hprof");

        System.out.println("\n    --- 8C16G 服务器（高吞吐量）---");
        System.out.println("    -Xms8g -Xmx8g -Xmn4g");
        System.out.println("    -XX:+UseG1GC -XX:MaxGCPauseMillis=100");
        System.out.println("    -XX:G1HeapRegionSize=16m");
        System.out.println("    -XX:InitiatingHeapOccupancyPercent=45");

        // 验证当前堆配置
        System.out.println("\n  【当前堆配置验证】");
        Runtime rt = Runtime.getRuntime();
        System.out.printf("    -Xmx (maxMemory):   %d MB%n", rt.maxMemory() / 1024 / 1024);
        System.out.printf("    -Xms (totalMemory):  %d MB%n", rt.totalMemory() / 1024 / 1024);
        System.out.printf("    可用处理器:           %d 核%n", rt.availableProcessors());
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    /** 字节转 MB */
    private static long toMB(long bytes) {
        return bytes / 1024 / 1024;
    }

    /** 采集当前 GC 统计数据 */
    private static Map<String, long[]> captureGCStats() {
        Map<String, long[]> stats = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            stats.put(gc.getName(), new long[]{gc.getCollectionCount(), gc.getCollectionTime()});
        }
        return stats;
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  JVM 调优演示 — MXBean 监控 + 内存分配 + GC 日志      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoMXBeanMonitoring();
        demoMemoryAllocationStrategy();
        demoGCLogAnalysis();
        demoTuningParameters();
    }
}

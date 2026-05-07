package com.example.jvm.diagnostic;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM 诊断演示 — 内存泄漏 / CPU 飙高 / 死锁模拟 + 排查工具使用
 *
 * <p>本示例模拟生产环境常见的 JVM 问题，并演示排查方法：
 * <ul>
 *   <li>内存泄漏：HashMap 持续增长导致 OOM</li>
 *   <li>CPU 飙高：死循环线程占满 CPU</li>
 *   <li>死锁：两个线程互相等待对方持有的锁</li>
 *   <li>排查工具：jps / jstack / jmap / jstat 命令详解</li>
 * </ul>
 *
 * <h3>JVM 问题排查流程：</h3>
 * <pre>
 *  ┌──────────────┐
 *  │ 发现异常      │  监控告警 / 用户反馈 / 日志异常
 *  └──────┬───────┘
 *         ↓
 *  ┌──────────────┐
 *  │ 初步定位      │  top -Hp pid → 找到高 CPU 线程
 *  │              │  jps → 找到 Java 进程 PID
 *  └──────┬───────┘
 *         ↓
 *  ┌──────────────┐
 *  │ 深入分析      │  jstack → 线程堆栈（死锁/CPU 飙高）
 *  │              │  jmap → 堆内存快照（内存泄漏）
 *  │              │  jstat → GC 统计（频繁 GC）
 *  └──────┬───────┘
 *         ↓
 *  ┌──────────────┐
 *  │ 工具分析      │  MAT → 分析 heap dump
 *  │              │  Arthas → 在线诊断
 *  └──────────────┘
 * </pre>
 *
 * @author JVM 诊断示例
 * @since 1.0
 */
public class DiagnosticDemo {

    // ==================== 一、内存泄漏模拟 ====================

    /**
     * 演示1：模拟内存泄漏 — HashMap 持续增长。
     *
     * <p>常见内存泄漏场景：
     * <ul>
     *   <li>静态集合类持有对象引用（本例）</li>
     *   <li>未关闭的资源（Connection, Stream）</li>
     *   <li>内部类持有外部类引用</li>
     *   <li>ThreadLocal 未 remove</li>
     *   <li>监听器/回调未注销</li>
     * </ul>
     */
    static void demoMemoryLeak() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：内存泄漏模拟 — HashMap 持续增长");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟缓存未设置上限导致的内存泄漏
        // 生产中常见：本地缓存无淘汰策略，数据只进不出
        Map<String, byte[]> leakyCache = new HashMap<>();
        Runtime rt = Runtime.getRuntime();

        System.out.println("\n  模拟场景：本地缓存无淘汰策略，数据持续增长");
        System.out.printf("    初始已用内存: %d KB%n", (rt.totalMemory() - rt.freeMemory()) / 1024);

        // 持续往缓存中添加数据（模拟泄漏）
        int leakRounds = 50;
        int entrySize = 10 * 1024; // 每个条目 10KB
        long[] memorySnapshots = new long[leakRounds / 10 + 1];
        int snapIdx = 0;
        memorySnapshots[snapIdx++] = (rt.totalMemory() - rt.freeMemory()) / 1024;

        for (int i = 0; i < leakRounds; i++) {
            // 模拟业务不断产生缓存数据
            leakyCache.put("session-" + UUID.randomUUID(), new byte[entrySize]);

            if ((i + 1) % 10 == 0) {
                long usedKB = (rt.totalMemory() - rt.freeMemory()) / 1024;
                memorySnapshots[snapIdx++] = usedKB;
                System.out.printf("    第 %3d 次添加后: 缓存大小=%d, 已用内存=%d KB%n",
                        i + 1, leakyCache.size(), usedKB);
            }
        }

        // 内存增长趋势分析
        System.out.println("\n  内存增长趋势（泄漏特征：持续上升不回落）：");
        int maxBar = 40;
        long maxMem = Arrays.stream(memorySnapshots, 0, snapIdx).max().orElse(1);
        for (int i = 0; i < snapIdx; i++) {
            int barLen = (int) (memorySnapshots[i] * maxBar / maxMem);
            System.out.printf("    %3d条 │%s %dKB%n",
                    i * 10, "█".repeat(Math.max(1, barLen)), memorySnapshots[i]);
        }

        // 排查方法
        System.out.println("\n  【排查方法】");
        System.out.println("    1. jmap -histo:live <pid> | head -20  → 查看对象数量 Top20");
        System.out.println("    2. jmap -dump:format=b,file=heap.hprof <pid>  → 导出堆快照");
        System.out.println("    3. MAT 打开 heap.hprof → Leak Suspects → 定位泄漏对象");
        System.out.println("    4. 检查 GC Roots 引用链，找到持有泄漏对象的根引用");

        // 修复方案
        System.out.println("\n  【修复方案】");
        System.out.println("    1. 使用 LRU 缓存（如 LinkedHashMap + removeEldestEntry）");
        System.out.println("    2. 使用 WeakHashMap（key 无强引用时自动回收）");
        System.out.println("    3. 使用 Caffeine/Guava Cache（带过期和容量限制）");

        // 演示 LRU 缓存修复
        int maxSize = 20;
        Map<String, byte[]> lruCache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > maxSize;
            }
        };

        for (int i = 0; i < 50; i++) {
            lruCache.put("key-" + i, new byte[1024]);
        }
        System.out.printf("\n    LRU 缓存修复后: 添加 50 条，实际大小=%d（上限 %d）%n",
                lruCache.size(), maxSize);

        // 清理泄漏数据
        leakyCache.clear();
        System.out.println();
    }

    // ==================== 二、CPU 飙高模拟 ====================

    /**
     * 演示2：模拟 CPU 飙高 — 死循环线程。
     *
     * <p>CPU 飙高常见原因：
     * <ul>
     *   <li>死循环（本例）</li>
     *   <li>正则表达式回溯（灾难性回溯）</li>
     *   <li>频繁 Full GC（GC 线程占满 CPU）</li>
     *   <li>大量线程上下文切换</li>
     * </ul>
     */
    static void demoCPUSpike() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：CPU 飙高模拟 — 死循环线程");
        System.out.println("═══════════════════════════════════════════════════");

        AtomicBoolean running = new AtomicBoolean(true);
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // 创建一个 CPU 密集型线程（模拟死循环）
        Thread busyThread = new Thread(() -> {
            long counter = 0;
            while (running.get()) {
                counter++; // 空循环，消耗 CPU
                // 实际生产中可能是：while(true) { processQueue(); }
                if (counter > 50_000_000) break; // 安全限制，避免真的跑满
            }
        }, "cpu-busy-thread");

        // 创建一个正常线程作为对比
        Thread normalThread = new Thread(() -> {
            try {
                Thread.sleep(200); // 正常线程大部分时间在等待
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "normal-thread");

        busyThread.start();
        normalThread.start();

        // 等待一小段时间让线程运行
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 采集线程 CPU 时间
        System.out.println("\n  【线程 CPU 时间对比】");
        long busyCpu = threadBean.getThreadCpuTime(busyThread.getId());
        long normalCpu = threadBean.getThreadCpuTime(normalThread.getId());
        System.out.printf("    cpu-busy-thread:  CPU 时间 = %d ms%n", busyCpu / 1_000_000);
        System.out.printf("    normal-thread:    CPU 时间 = %d ms%n", normalCpu / 1_000_000);

        // 获取线程状态
        System.out.println("\n  【线程状态】");
        System.out.printf("    cpu-busy-thread:  状态 = %s%n", busyThread.getState());
        System.out.printf("    normal-thread:    状态 = %s%n", normalThread.getState());

        // 停止忙线程
        running.set(false);
        try {
            busyThread.join(1000);
            normalThread.join(1000);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 排查步骤
        System.out.println("\n  【CPU 飙高排查步骤】");
        System.out.println("    步骤1: top → 找到 CPU 高的 Java 进程 PID");
        System.out.println("    步骤2: top -Hp <pid> → 找到 CPU 高的线程 TID");
        System.out.println("    步骤3: printf '%x' <tid> → 将 TID 转为十六进制");
        System.out.println("    步骤4: jstack <pid> | grep <hex_tid> -A 30 → 查看线程堆栈");
        System.out.println("    步骤5: 分析堆栈，定位死循环/热点代码");

        // 模拟 jstack 输出
        System.out.println("\n  【模拟 jstack 输出】");
        ThreadInfo busyInfo = threadBean.getThreadInfo(busyThread.getId(), 5);
        if (busyInfo != null) {
            System.out.printf("    \"%s\" #%d%n", busyInfo.getThreadName(), busyInfo.getThreadId());
            System.out.printf("      java.lang.Thread.State: %s%n", busyInfo.getThreadState());
            for (StackTraceElement ste : busyInfo.getStackTrace()) {
                System.out.printf("        at %s%n", ste);
            }
        }
        System.out.println();
    }

    // ==================== 三、死锁模拟 ====================

    /**
     * 演示3：模拟死锁 — 两个线程互相等待。
     *
     * <pre>
     *  死锁条件（四个必要条件同时满足）：
     *  ┌────────────┐     ┌────────────┐
     *  │ 互斥条件    │     │ 请求与保持  │
     *  │ 资源独占    │     │ 持有并等待  │
     *  └────────────┘     └────────────┘
     *  ┌────────────┐     ┌────────────┐
     *  │ 不可剥夺    │     │ 循环等待    │
     *  │ 不能强制释放 │     │ A→B→A 环路 │
     *  └────────────┘     └────────────┘
     * </pre>
     */
    static void demoDeadlock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：死锁模拟 — 两个线程互相等待");
        System.out.println("═══════════════════════════════════════════════════");

        // 两把锁
        final Object lockA = new Object();
        final Object lockB = new Object();
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean deadlockCreated = new AtomicBoolean(false);

        // 线程1：先获取 lockA，再请求 lockB
        Thread thread1 = new Thread(() -> {
            synchronized (lockA) {
                latch.countDown();
                try { latch.await(); } catch (InterruptedException e) { return; }
                // 此时线程2已持有 lockB，这里会阻塞 → 死锁
                synchronized (lockB) {
                    deadlockCreated.set(true);
                }
            }
        }, "deadlock-thread-1");

        // 线程2：先获取 lockB，再请求 lockA
        Thread thread2 = new Thread(() -> {
            synchronized (lockB) {
                latch.countDown();
                try { latch.await(); } catch (InterruptedException e) { return; }
                // 此时线程1已持有 lockA，这里会阻塞 → 死锁
                synchronized (lockA) {
                    deadlockCreated.set(true);
                }
            }
        }, "deadlock-thread-2");

        thread1.start();
        thread2.start();

        // 等待死锁形成
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 使用 ThreadMXBean 检测死锁
        System.out.println("\n  【ThreadMXBean 死锁检测】");
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = threadBean.findDeadlockedThreads();

        if (deadlockedIds != null) {
            System.out.printf("    检测到 %d 个死锁线程：%n", deadlockedIds.length);
            ThreadInfo[] infos = threadBean.getThreadInfo(deadlockedIds, true, true);
            for (ThreadInfo info : infos) {
                System.out.printf("\n    \"%s\" (id=%d)%n", info.getThreadName(), info.getThreadId());
                System.out.printf("      状态: %s%n", info.getThreadState());
                System.out.printf("      等待锁: %s%n", info.getLockName());
                System.out.printf("      锁持有者: \"%s\"%n", info.getLockOwnerName());

                // 打印持有的锁
                MonitorInfo[] monitors = info.getLockedMonitors();
                if (monitors.length > 0) {
                    System.out.println("      已持有的锁:");
                    for (MonitorInfo mi : monitors) {
                        System.out.printf("        %s (在 %s)%n",
                                mi.getClassName(), mi.getLockedStackFrame());
                    }
                }
            }
        } else {
            System.out.println("    未检测到死锁（可能死锁尚未形成）");
        }

        // 强制中断死锁线程
        thread1.interrupt();
        thread2.interrupt();
        try {
            thread1.join(1000);
            thread2.join(1000);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 排查方法
        System.out.println("\n  【死锁排查方法】");
        System.out.println("    1. jstack <pid> → 搜索 'Found one Java-level deadlock'");
        System.out.println("    2. jconsole → 线程 Tab → 检测死锁");
        System.out.println("    3. Arthas: thread -b → 找到阻塞线程");

        // 避免死锁的方法
        System.out.println("\n  【避免死锁的方法】");
        System.out.println("    1. 固定加锁顺序（所有线程按相同顺序获取锁）");
        System.out.println("    2. 使用 tryLock 超时机制（ReentrantLock.tryLock）");
        System.out.println("    3. 减小锁粒度（ConcurrentHashMap 分段锁）");
        System.out.println("    4. 使用无锁数据结构（CAS、Atomic 类）");

        // 演示 tryLock 避免死锁
        System.out.println("\n  【tryLock 避免死锁示例】");
        java.util.concurrent.locks.ReentrantLock rLockA = new java.util.concurrent.locks.ReentrantLock();
        java.util.concurrent.locks.ReentrantLock rLockB = new java.util.concurrent.locks.ReentrantLock();

        boolean acquired = false;
        try {
            if (rLockA.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    if (rLockB.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            acquired = true;
                        } finally {
                            rLockB.unlock();
                        }
                    }
                } finally {
                    rLockA.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("    tryLock 获取两把锁: %s（超时自动放弃，不会死锁）%n", acquired);
        System.out.println();
    }

    // ==================== 四、排查工具使用 ====================

    /**
     * 演示4：JVM 排查工具命令详解。
     *
     * <pre>
     *  工具关系图：
     *  ┌─────────────────────────────────────────┐
     *  │              JVM 诊断工具                 │
     *  ├──────────┬──────────┬──────────┬─────────┤
     *  │   jps    │  jstack  │   jmap   │  jstat  │
     *  │ 进程列表  │ 线程堆栈  │ 内存快照  │ GC 统计 │
     *  ├──────────┼──────────┼──────────┼─────────┤
     *  │   jinfo  │  jcmd    │  jhat    │ Arthas  │
     *  │ JVM 配置  │ 综合命令  │ dump分析 │ 在线诊断│
     *  └──────────┴──────────┴──────────┴─────────┘
     * </pre>
     */
    static void demoToolUsage() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：JVM 排查工具命令详解");
        System.out.println("═══════════════════════════════════════════════════");

        // 获取当前进程信息用于演示
        RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
        String processName = rtBean.getName(); // pid@hostname
        String pid = processName.split("@")[0];

        System.out.printf("\n  当前 Java 进程: PID=%s%n", pid);

        // jps
        System.out.println("\n  【jps — 列出 Java 进程】");
        System.out.println("    jps              → 列出所有 Java 进程");
        System.out.println("    jps -l           → 显示完整类名");
        System.out.println("    jps -v           → 显示 JVM 参数");
        System.out.printf("    示例输出: %s DiagnosticDemo%n", pid);

        // jstack
        System.out.println("\n  【jstack — 线程堆栈分析】");
        System.out.printf("    jstack %s              → 打印所有线程堆栈%n", pid);
        System.out.printf("    jstack -l %s           → 包含锁信息%n", pid);
        System.out.printf("    jstack %s > thread.txt → 导出到文件%n", pid);
        System.out.println("    用途: 死锁检测、CPU 飙高定位、线程阻塞分析");

        // 实际获取当前线程信息
        System.out.println("\n    当前线程快照（模拟 jstack 输出）：");
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] allThreads = threadBean.getThreadInfo(threadBean.getAllThreadIds(), 3);
        int shown = 0;
        for (ThreadInfo ti : allThreads) {
            if (ti == null) continue;
            if (shown >= 3) break; // 只显示前 3 个
            System.out.printf("      \"%s\" #%d  %s%n",
                    ti.getThreadName(), ti.getThreadId(), ti.getThreadState());
            for (StackTraceElement ste : ti.getStackTrace()) {
                System.out.printf("          at %s%n", ste);
            }
            shown++;
        }

        // jmap
        System.out.println("\n  【jmap — 内存分析】");
        System.out.printf("    jmap -heap %s                          → 堆内存概览%n", pid);
        System.out.printf("    jmap -histo:live %s | head -20         → 对象统计 Top20%n", pid);
        System.out.printf("    jmap -dump:format=b,file=heap.hprof %s → 导出堆快照%n", pid);
        System.out.println("    用途: 内存泄漏排查、大对象定位");
        System.out.println("    分析工具: Eclipse MAT, VisualVM, JProfiler");

        // jstat
        System.out.println("\n  【jstat — GC 统计监控】");
        System.out.printf("    jstat -gc %s 1000 10       → 每秒采集 GC 数据，共 10 次%n", pid);
        System.out.printf("    jstat -gcutil %s 1000      → GC 使用率百分比%n", pid);
        System.out.printf("    jstat -gccause %s 1000     → 包含 GC 原因%n", pid);

        // 模拟 jstat -gcutil 输出
        System.out.println("\n    模拟 jstat -gcutil 输出：");
        System.out.println("      S0     S1     E      O      M     CCS    YGC  YGCT   FGC  FGCT   GCT");
        // 从 MXBean 获取实际数据
        Map<String, Double> poolUsage = new LinkedHashMap<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            if (usage == null) continue;
            long max = usage.getMax() > 0 ? usage.getMax() : usage.getCommitted();
            double pct = max > 0 ? (double) usage.getUsed() / max * 100 : 0;
            poolUsage.put(pool.getName(), pct);
        }

        long ygc = 0, fgc = 0;
        double ygct = 0, fgct = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gc.getName().toLowerCase();
            if (name.contains("young") || name.contains("minor") || name.contains("scavenge")
                    || name.contains("copy") || name.contains("parnew")) {
                ygc = gc.getCollectionCount();
                ygct = gc.getCollectionTime() / 1000.0;
            } else {
                fgc = gc.getCollectionCount();
                fgct = gc.getCollectionTime() / 1000.0;
            }
        }
        System.out.printf("      %-6.2f %-6.2f %-6.2f %-6.2f %-6.2f %-6.2f %-4d %-6.3f %-4d %-6.3f %.3f%n",
                0.0, 0.0,
                poolUsage.values().stream().findFirst().orElse(0.0),
                poolUsage.values().stream().skip(1).findFirst().orElse(0.0),
                0.0, 0.0, ygc, ygct, fgc, fgct, ygct + fgct);

        // Arthas 简介
        System.out.println("\n  【Arthas — 阿里开源在线诊断工具】");
        System.out.println("    安装: curl -O https://arthas.aliyun.com/arthas-boot.jar");
        System.out.println("    启动: java -jar arthas-boot.jar");
        System.out.println("    常用命令:");
        System.out.println("      dashboard        → 实时面板（线程/内存/GC）");
        System.out.println("      thread -b         → 找到阻塞线程");
        System.out.println("      thread -n 3       → CPU 最高的 3 个线程");
        System.out.println("      heapdump /tmp/a.hprof → 导出堆快照");
        System.out.println("      watch com.example.Service method '{params,returnObj}' → 观察方法");
        System.out.println("      trace com.example.Service method → 方法调用链路耗时");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  JVM 诊断演示 — 内存泄漏 / CPU 飙高 / 死锁排查       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoMemoryLeak();
        demoCPUSpike();
        demoDeadlock();
        demoToolUsage();
    }
}

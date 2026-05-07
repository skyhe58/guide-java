package com.example.jvm.gc;

/**
 * JVM 垃圾回收演示 — 四种引用类型 + GC 触发条件 + 对象晋升
 *
 * <p>本示例演示 JVM 垃圾回收的核心机制：
 * <ul>
 *   <li>四种引用类型：强引用 / 软引用 / 弱引用 / 虚引用</li>
 *   <li>GC 触发条件：Eden 满 → Minor GC，Old 满 → Major/Full GC</li>
 *   <li>对象晋升：年龄达到阈值 / Survivor 放不下 → 晋升老年代</li>
 *   <li>GC 算法：标记-清除 / 标记-复制 / 标记-整理</li>
 *   <li>垃圾收集器：Serial / Parallel / CMS / G1 / ZGC</li>
 * </ul>
 */
public class GCDemo {

    // ==================== 四种引用类型 ====================

    /** 演示1：四种引用类型的 GC 行为 */
    static void demoReferenceTypes() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：四种引用类型的 GC 行为");
        System.out.println("═══════════════════════════════════════════════════");

        // 强引用：GC 永远不会回收（即使 OOM 也不回收）
        System.out.println("\n  【强引用 Strong Reference】");
        Object strongRef = new byte[1024];
        System.out.printf("    GC 前: %s%n", strongRef != null ? "存在" : "已回收");
        System.gc();
        System.out.printf("    GC 后: %s（强引用不会被回收）%n", strongRef != null ? "存在" : "已回收");

        // 软引用：内存不足时才回收（适合缓存）
        System.out.println("\n  【软引用 Soft Reference】");
        java.lang.ref.SoftReference<byte[]> softRef = new java.lang.ref.SoftReference<>(new byte[1024]);
        System.out.printf("    GC 前: %s%n", softRef.get() != null ? "存在" : "已回收");
        System.gc();
        System.out.printf("    GC 后: %s（内存充足时不回收）%n", softRef.get() != null ? "存在" : "已回收");
        System.out.println("    应用场景：图片缓存、页面缓存");

        // 弱引用：下次 GC 一定回收
        System.out.println("\n  【弱引用 Weak Reference】");
        java.lang.ref.WeakReference<byte[]> weakRef = new java.lang.ref.WeakReference<>(new byte[1024]);
        System.out.printf("    GC 前: %s%n", weakRef.get() != null ? "存在" : "已回收");
        System.gc();
        // 注意：System.gc() 只是建议，不保证立即执行
        System.out.printf("    GC 后: %s（弱引用在 GC 时被回收）%n", weakRef.get() != null ? "存在" : "已回收");
        System.out.println("    应用场景：ThreadLocal 的 Entry、WeakHashMap");

        // 虚引用：无法通过虚引用获取对象，仅用于跟踪 GC
        System.out.println("\n  【虚引用 Phantom Reference】");
        java.lang.ref.ReferenceQueue<byte[]> queue = new java.lang.ref.ReferenceQueue<>();
        java.lang.ref.PhantomReference<byte[]> phantomRef =
                new java.lang.ref.PhantomReference<>(new byte[1024], queue);
        System.out.printf("    get() = %s（虚引用永远返回 null）%n", phantomRef.get());
        System.out.println("    应用场景：跟踪对象被 GC 的时机（如 DirectByteBuffer 释放堆外内存）");

        // 对比表
        System.out.println("\n  引用类型对比：");
        System.out.printf("    %-10s %-15s %-15s %-20s%n", "类型", "GC 回收时机", "get() 返回", "应用场景");
        System.out.println("    " + "─".repeat(60));
        System.out.printf("    %-10s %-15s %-15s %-20s%n", "强引用", "永不回收", "对象本身", "普通变量");
        System.out.printf("    %-10s %-15s %-15s %-20s%n", "软引用", "内存不足时", "对象/null", "缓存");
        System.out.printf("    %-10s %-15s %-15s %-20s%n", "弱引用", "下次 GC", "对象/null", "ThreadLocal");
        System.out.printf("    %-10s %-15s %-15s %-20s%n", "虚引用", "随时", "永远 null", "跟踪 GC");
        System.out.println();
    }

    // ==================== GC 算法 ====================

    /** 演示2：GC 算法模拟 */
    static void demoGCAlgorithms() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：GC 算法 — 标记-清除 / 标记-复制 / 标记-整理");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟内存空间
        String[] memory = {"A", null, "B", null, "C", null, null, "D", null, "E"};

        System.out.println("\n  初始内存（■=存活, □=垃圾）：");
        printMemory(memory);

        // 标记阶段（假设 B 和 D 是垃圾）
        boolean[] alive = {true, false, false, false, true, false, false, false, false, true};

        // 标记-清除
        System.out.println("\n  【标记-清除】直接清除垃圾对象（产生碎片）：");
        String[] afterSweep = memory.clone();
        for (int i = 0; i < afterSweep.length; i++) {
            if (!alive[i]) afterSweep[i] = null;
        }
        printMemory(afterSweep);
        System.out.println("    缺点：内存碎片，大对象可能无法分配");

        // 标记-复制
        System.out.println("\n  【标记-复制】存活对象复制到另一半空间（新生代用）：");
        String[] toSpace = new String[memory.length];
        int toIdx = 0;
        for (int i = 0; i < memory.length; i++) {
            if (alive[i] && memory[i] != null) {
                toSpace[toIdx++] = memory[i];
            }
        }
        printMemory(toSpace);
        System.out.println("    优点：无碎片，分配快（指针碰撞）");
        System.out.println("    缺点：浪费一半空间（Eden:S0:S1 = 8:1:1 优化）");

        // 标记-整理
        System.out.println("\n  【标记-整理】存活对象向一端移动（老年代用）：");
        String[] afterCompact = new String[memory.length];
        int compactIdx = 0;
        for (int i = 0; i < memory.length; i++) {
            if (alive[i] && memory[i] != null) {
                afterCompact[compactIdx++] = memory[i];
            }
        }
        printMemory(afterCompact);
        System.out.println("    优点：无碎片");
        System.out.println("    缺点：移动对象需要更新引用，STW 时间长");
        System.out.println();
    }

    /** 演示3：对象晋升与 GC 触发 */
    static void demoObjectPromotion() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：对象晋升老年代的条件");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  对象晋升老年代的 4 种情况：");
        System.out.println("    1. 年龄达到阈值（-XX:MaxTenuringThreshold=15）");
        System.out.println("       对象每经历一次 Minor GC 年龄 +1，达到阈值后晋升");
        System.out.println("    2. 大对象直接进入老年代（-XX:PretenureSizeThreshold）");
        System.out.println("       避免大对象在 Eden 和 Survivor 之间来回复制");
        System.out.println("    3. Survivor 空间不足（动态年龄判断）");
        System.out.println("       相同年龄的对象总大小 > Survivor 空间的一半 → 该年龄及以上的对象晋升");
        System.out.println("    4. Minor GC 后 Survivor 放不下 → 直接进入老年代");

        System.out.println("\n  GC 触发条件：");
        System.out.println("    Minor GC: Eden 区满时触发");
        System.out.println("    Major GC: 老年代空间不足时触发（通常伴随 Minor GC）");
        System.out.println("    Full GC:  老年代满 / 元空间满 / System.gc() / CMS 并发失败");

        // 通过 MXBean 观察 GC
        System.out.println("\n  当前 GC 信息：");
        for (java.lang.management.GarbageCollectorMXBean gc :
                java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("    %s: 次数=%d, 总耗时=%dms%n",
                    gc.getName(), gc.getCollectionCount(), gc.getCollectionTime());
        }

        // 触发一次 GC 观察变化
        System.gc();
        System.out.println("  System.gc() 后：");
        for (java.lang.management.GarbageCollectorMXBean gc :
                java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("    %s: 次数=%d, 总耗时=%dms%n",
                    gc.getName(), gc.getCollectionCount(), gc.getCollectionTime());
        }
        System.out.println();
    }

    /** 演示4：垃圾收集器对比 */
    static void demoCollectorComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：垃圾收集器对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"收集器",    "算法",      "线程",  "STW",    "适用场景",          "JDK 版本"},
                {"Serial",   "复制/整理", "单线程", "长",     "客户端/小内存",      "所有"},
                {"Parallel", "复制/整理", "多线程", "中",     "吞吐量优先（默认）",   "JDK 8 默认"},
                {"CMS",      "标记-清除", "并发",  "短",     "低延迟（已废弃）",    "JDK 9 废弃"},
                {"G1",       "分区复制",  "并发",  "可控",   "大内存低延迟",       "JDK 9 默认"},
                {"ZGC",      "染色指针",  "并发",  "<1ms",  "超大内存超低延迟",    "JDK 15+"},
                {"Shenandoah","转发指针", "并发",  "<10ms", "低延迟",            "JDK 12+"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-12s %-10s %-8s %-8s %-20s %-10s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2],
                    comparison[i][3], comparison[i][4], comparison[i][5]);
            if (i == 0) System.out.println("  " + "─".repeat(70));
        }

        // 当前使用的收集器
        System.out.println("\n  当前 JVM 使用的收集器：");
        for (java.lang.management.GarbageCollectorMXBean gc :
                java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("    %s（管理区域: %s）%n",
                    gc.getName(), String.join(", ", gc.getMemoryPoolNames()));
        }

        System.out.println("\n  推荐选择：");
        System.out.println("    JDK 8: Parallel（默认）或 G1（-XX:+UseG1GC）");
        System.out.println("    JDK 11+: G1（默认）");
        System.out.println("    JDK 17+: ZGC（-XX:+UseZGC）适合大内存低延迟");
        System.out.println();
    }

    static void printMemory(String[] memory) {
        System.out.print("    [");
        for (int i = 0; i < memory.length; i++) {
            System.out.print(memory[i] != null ? "■" + memory[i] : "□");
            if (i < memory.length - 1) System.out.print("|");
        }
        System.out.println("]");
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  JVM 垃圾回收演示 — 引用类型 + GC 算法 + 收集器对比    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoReferenceTypes();
        demoGCAlgorithms();
        demoObjectPromotion();
        demoCollectorComparison();
    }
}

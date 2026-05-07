package com.example.basics.datatypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Java 数据类型演示 — 装箱拆箱陷阱 / Integer 缓存池 / 浮点精度 / 性能对比
 *
 * <p>本示例演示 Java 数据类型的核心知识点和常见陷阱：
 * <ul>
 *   <li>装箱拆箱：== vs equals、NPE 风险、三目运算符陷阱</li>
 *   <li>Integer 缓存池：-128~127 范围内的对象复用机制</li>
 *   <li>浮点精度：0.1+0.2!=0.3、BigDecimal 正确用法</li>
 *   <li>性能对比：基本类型 vs 包装类的内存与速度差异</li>
 * </ul>
 *
 * <h3>Java 基本类型内存布局：</h3>
 * <pre>
 *  基本类型（栈上分配）：
 *  ┌──────┐
 *  │ 42   │  int: 4 字节，直接存储值
 *  └──────┘
 *
 *  包装类型（堆上分配）：
 *  ┌──────────────────────────┐
 *  │ 对象头 (12B) │ value (4B) │  Integer: 16 字节 + 引用 4 字节
 *  │ Mark Word    │   42       │
 *  │ Klass Ptr    │            │
 *  └──────────────────────────┘
 *
 *  Integer 缓存池：
 *  IntegerCache[-128] ──→ Integer(-128)
 *  IntegerCache[-127] ──→ Integer(-127)
 *  ...
 *  IntegerCache[127]  ──→ Integer(127)
 *  valueOf(100) ──→ 返回缓存对象（==为true）
 *  valueOf(200) ──→ new Integer(200)（==为false）
 * </pre>
 *
 * @author Java 基础示例
 * @since 1.0
 */
public class DataTypesDemo {

    // ==================== 一、装箱拆箱陷阱 ====================

    /**
     * 演示1：自动装箱/拆箱的陷阱。
     *
     * <p>装箱：基本类型 → 包装类型（编译器自动调用 valueOf）
     * <p>拆箱：包装类型 → 基本类型（编译器自动调用 xxxValue）
     *
     * <pre>
     *  编译器转换：
     *  Integer a = 100;        →  Integer a = Integer.valueOf(100);
     *  int b = a;              →  int b = a.intValue();
     *  a + 1                   →  a.intValue() + 1
     * </pre>
     */
    static void demoAutoBoxing() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：装箱拆箱陷阱 — == vs equals + NPE");
        System.out.println("═══════════════════════════════════════════════════");

        // --- == vs equals ---
        System.out.println("\n  【== vs equals 对比】");
        Integer a = 100, b = 100;   // 缓存范围内
        Integer c = 200, d = 200;   // 缓存范围外

        System.out.printf("    Integer(100) == Integer(100)  → %s（缓存命中，同一对象）%n", a == b);
        System.out.printf("    Integer(200) == Integer(200)  → %s（缓存未命中，不同对象！）%n", c == d);
        System.out.printf("    Integer(200).equals(200)      → %s（值比较，推荐用法）%n", c.equals(d));

        // int 与 Integer 混合比较
        int primitiveVal = 200;
        System.out.printf("    int(200) == Integer(200)      → %s（Integer 自动拆箱）%n",
                primitiveVal == c);

        // 不同包装类型比较
        Long longVal = 200L;
        System.out.printf("    Integer(200).equals(Long(200))→ %s（类型不同！）%n", c.equals(longVal));
        System.out.printf("    200 == 200L                   → %s（基本类型自动提升）%n", 200 == 200L);

        // --- NPE 风险 ---
        System.out.println("\n  【自动拆箱 NPE 风险】");

        // 场景1：null 拆箱
        Integer nullVal = null;
        try {
            int unboxed = nullVal; // nullVal.intValue() → NPE
        } catch (NullPointerException e) {
            System.out.println("    场景1: null 拆箱 → NullPointerException");
        }

        // 场景2：三目运算符陷阱
        Integer x = 1;
        Integer y = null;
        try {
            // 编译器统一类型为 int，导致 y.intValue() → NPE
            int result = false ? x : y;
        } catch (NullPointerException e) {
            System.out.println("    场景2: 三目运算符 null 拆箱 → NPE");
        }

        // 场景3：Map.get 返回 null
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 42);
        try {
            int val = map.get("key2"); // null.intValue() → NPE
        } catch (NullPointerException e) {
            System.out.println("    场景3: Map.get(不存在的key) 拆箱 → NPE");
        }

        // 安全做法
        System.out.println("\n  【安全做法】");
        int safe1 = Optional.ofNullable(map.get("key2")).orElse(0);
        int safe2 = map.getOrDefault("key2", 0);
        System.out.printf("    Optional.ofNullable().orElse(0) = %d%n", safe1);
        System.out.printf("    map.getOrDefault(\"key2\", 0)     = %d%n", safe2);
        System.out.println();
    }

    // ==================== 二、Integer 缓存池 ====================

    /**
     * 演示2：Integer 缓存池机制 — -128 ~ 127。
     *
     * <p>Integer.valueOf(int) 源码逻辑：
     * <pre>
     *  public static Integer valueOf(int i) {
     *      if (i >= IntegerCache.low && i <= IntegerCache.high)
     *          return IntegerCache.cache[i + (-IntegerCache.low)];
     *      return new Integer(i);
     *  }
     * </pre>
     */
    static void demoIntegerCache() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Integer 缓存池 — -128 ~ 127");
        System.out.println("═══════════════════════════════════════════════════");

        // 缓存池边界验证
        System.out.println("\n  【缓存池边界验证】");
        int[] testValues = {-129, -128, -1, 0, 1, 127, 128, 1000};
        for (int val : testValues) {
            Integer x = Integer.valueOf(val);
            Integer y = Integer.valueOf(val);
            boolean cached = (val >= -128 && val <= 127);
            System.out.printf("    valueOf(%5d): x == y → %-5s  缓存: %s%n",
                    val, x == y, cached ? "✓" : "✗");
        }

        // 自动装箱也走 valueOf
        System.out.println("\n  【自动装箱走 valueOf】");
        Integer autoBox1 = 50;  // 编译为 Integer.valueOf(50)
        Integer autoBox2 = 50;
        Integer newObj1 = new Integer(50);  // 直接创建新对象（已废弃）
        System.out.printf("    自动装箱(50) == 自动装箱(50)  → %s（走缓存）%n", autoBox1 == autoBox2);
        System.out.printf("    自动装箱(50) == new Integer(50) → %s（new 不走缓存）%n", autoBox1 == newObj1);

        // 各包装类缓存范围
        System.out.println("\n  【各包装类缓存范围】");
        String[][] cacheRanges = {
            {"包装类", "缓存范围", "可调整"},
            {"Byte", "-128 ~ 127（全部）", "否"},
            {"Short", "-128 ~ 127", "否"},
            {"Integer", "-128 ~ 127", "是（-XX:AutoBoxCacheMax）"},
            {"Long", "-128 ~ 127", "否"},
            {"Character", "0 ~ 127", "否"},
            {"Boolean", "TRUE / FALSE", "否"},
            {"Float", "无缓存", "—"},
            {"Double", "无缓存", "—"},
        };
        for (int i = 0; i < cacheRanges.length; i++) {
            System.out.printf("    %-12s %-22s %s%n",
                    cacheRanges[i][0], cacheRanges[i][1], cacheRanges[i][2]);
            if (i == 0) System.out.println("    " + "─".repeat(55));
        }

        // 验证 Long 缓存
        System.out.println("\n  【Long 缓存验证】");
        Long l1 = 127L, l2 = 127L;
        Long l3 = 128L, l4 = 128L;
        System.out.printf("    Long(127) == Long(127) → %s%n", l1 == l2);
        System.out.printf("    Long(128) == Long(128) → %s%n", l3 == l4);
        System.out.println();
    }

    // ==================== 三、浮点精度问题 ====================

    /**
     * 演示3：浮点精度陷阱与 BigDecimal 正确用法。
     *
     * <pre>
     *  IEEE 754 双精度浮点数（64位）：
     *  ┌───┬──────────┬────────────────────────────────────────────────────┐
     *  │ S │ Exponent │                  Mantissa                         │
     *  │1位│  11位     │                  52位                              │
     *  └───┴──────────┴────────────────────────────────────────────────────┘
     *
     *  0.1 的二进制表示：0.0001100110011001100110011...（无限循环）
     *  → 截断后存储，导致精度丢失
     * </pre>
     */
    static void demoFloatingPoint() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：浮点精度问题 — 0.1 + 0.2 ≠ 0.3");
        System.out.println("═══════════════════════════════════════════════════");

        // 经典精度问题
        System.out.println("\n  【经典精度问题】");
        double sum = 0.1 + 0.2;
        System.out.printf("    0.1 + 0.2   = %.17f%n", sum);
        System.out.printf("    == 0.3      → %s%n", sum == 0.3);
        System.out.printf("    1.0 - 0.9   = %.17f%n", 1.0 - 0.9);
        System.out.printf("    0.1 * 3     = %.17f%n", 0.1 * 3);
        System.out.printf("    1.03 - 0.42 = %.17f%n", 1.03 - 0.42);

        // 浮点数比较的正确方式
        System.out.println("\n  【浮点数比较 — epsilon 方法】");
        double epsilon = 1e-10;
        boolean closeEnough = Math.abs(sum - 0.3) < epsilon;
        System.out.printf("    |0.1+0.2 - 0.3| < 1e-10 → %s%n", closeEnough);

        // BigDecimal 正确用法
        System.out.println("\n  【BigDecimal 正确用法】");
        BigDecimal wrong = new BigDecimal(0.1);
        BigDecimal right = new BigDecimal("0.1");
        BigDecimal also = BigDecimal.valueOf(0.1);
        System.out.printf("    new BigDecimal(0.1)     = %s（错误！double 精度已丢失）%n", wrong);
        System.out.printf("    new BigDecimal(\"0.1\")   = %s（正确）%n", right);
        System.out.printf("    BigDecimal.valueOf(0.1) = %s（正确）%n", also);

        // BigDecimal 运算
        System.out.println("\n  【BigDecimal 运算】");
        BigDecimal x = new BigDecimal("0.1");
        BigDecimal y = new BigDecimal("0.2");
        System.out.printf("    0.1 + 0.2 = %s%n", x.add(y));
        System.out.printf("    0.1 * 3   = %s%n", x.multiply(new BigDecimal("3")));

        // 除法必须指定精度
        BigDecimal ten = new BigDecimal("10");
        BigDecimal three = new BigDecimal("3");
        try {
            ten.divide(three); // 无限循环小数 → ArithmeticException
        } catch (ArithmeticException e) {
            System.out.println("    10 / 3 不指定精度 → ArithmeticException");
        }
        BigDecimal quotient = ten.divide(three, 6, RoundingMode.HALF_UP);
        System.out.printf("    10 / 3 (6位, HALF_UP) = %s%n", quotient);

        // BigDecimal 比较陷阱
        System.out.println("\n  【BigDecimal 比较陷阱】");
        BigDecimal bd1 = new BigDecimal("1.0");
        BigDecimal bd2 = new BigDecimal("1.00");
        System.out.printf("    1.0.equals(1.00)    → %s（精度不同！）%n", bd1.equals(bd2));
        System.out.printf("    1.0.compareTo(1.00) → %d（值相等，推荐用法）%n", bd1.compareTo(bd2));

        // 金额计算最佳实践
        System.out.println("\n  【金额计算最佳实践】");
        BigDecimal price = new BigDecimal("19.99");
        BigDecimal qty = new BigDecimal("3");
        BigDecimal discount = new BigDecimal("0.85");
        BigDecimal total = price.multiply(qty).multiply(discount)
                .setScale(2, RoundingMode.HALF_UP);
        System.out.printf("    单价=19.99, 数量=3, 折扣=0.85%n");
        System.out.printf("    总价 = 19.99 × 3 × 0.85 = %s%n", total);
        System.out.println();
    }

    // ==================== 四、性能对比 ====================

    /**
     * 演示4：基本类型 vs 包装类性能对比。
     *
     * <p>性能差异来源：
     * <ul>
     *   <li>装箱/拆箱开销：每次装箱创建新对象（缓存外）</li>
     *   <li>内存占用：Integer 16B vs int 4B（4倍差距）</li>
     *   <li>GC 压力：大量临时包装对象增加 GC 负担</li>
     *   <li>缓存局部性：数组连续内存 vs 对象分散在堆中</li>
     * </ul>
     */
    static void demoPerformanceComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：基本类型 vs 包装类 — 性能对比");
        System.out.println("═══════════════════════════════════════════════════");

        int iterations = 10_000_000;

        // --- 累加运算对比 ---
        System.out.printf("\n  【累加运算对比】（%d 百万次）%n", iterations / 1_000_000);

        // 预热 JIT
        for (int warmup = 0; warmup < 3; warmup++) {
            long s = 0;
            for (int i = 0; i < iterations; i++) s += i;
        }

        // int 累加
        long start = System.nanoTime();
        long primitiveSum = 0;
        for (int i = 0; i < iterations; i++) {
            primitiveSum += i;
        }
        long primitiveTime = System.nanoTime() - start;

        // Integer 累加（大量装箱拆箱）
        start = System.nanoTime();
        Long wrapperSum = 0L;
        for (int i = 0; i < iterations; i++) {
            wrapperSum += i; // 拆箱 → 加法 → 装箱
        }
        long wrapperTime = System.nanoTime() - start;

        System.out.printf("    long 累加:   %d ms (sum=%d)%n", primitiveTime / 1_000_000, primitiveSum);
        System.out.printf("    Long 累加:   %d ms (sum=%d)%n", wrapperTime / 1_000_000, wrapperSum);
        System.out.printf("    性能差距:    %.1fx%n", (double) wrapperTime / primitiveTime);

        // --- 数组 vs ArrayList ---
        System.out.println("\n  【数组 vs ArrayList】");
        int size = 1_000_000;

        start = System.nanoTime();
        int[] intArray = new int[size];
        for (int i = 0; i < size; i++) intArray[i] = i;
        long arraySum = 0;
        for (int v : intArray) arraySum += v;
        long arrayTime = System.nanoTime() - start;

        start = System.nanoTime();
        List<Integer> intList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) intList.add(i); // 装箱
        long listSum = 0;
        for (int v : intList) listSum += v; // 拆箱
        long listTime = System.nanoTime() - start;

        System.out.printf("    int[] 填充+求和:        %d ms%n", arrayTime / 1_000_000);
        System.out.printf("    ArrayList<Integer>:     %d ms%n", listTime / 1_000_000);
        System.out.printf("    性能差距:               %.1fx%n", (double) listTime / arrayTime);

        // --- 内存占用对比 ---
        System.out.println("\n  【内存占用对比】");
        String[][] memTable = {
            {"类型", "单个大小", "1000个数组", "倍数"},
            {"int", "4B", "~4KB", "1x"},
            {"Integer", "16B+4B引用", "~20KB", "5x"},
            {"long", "8B", "~8KB", "1x"},
            {"Long", "16B+4B引用", "~20KB", "2.5x"},
            {"double", "8B", "~8KB", "1x"},
            {"Double", "16B+4B引用", "~20KB", "2.5x"},
        };
        for (int i = 0; i < memTable.length; i++) {
            System.out.printf("    %-10s %-15s %-12s %s%n",
                    memTable[i][0], memTable[i][1], memTable[i][2], memTable[i][3]);
            if (i == 0) System.out.println("    " + "─".repeat(45));
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Java 数据类型演示 — 装箱拆箱 + 缓存池 + 浮点精度     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoAutoBoxing();
        demoIntegerCache();
        demoFloatingPoint();
        demoPerformanceComparison();
    }
}

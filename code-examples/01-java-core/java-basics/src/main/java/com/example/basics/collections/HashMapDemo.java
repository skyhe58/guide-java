package com.example.basics.collections;

import java.lang.reflect.Field;
import java.util.*;

/**
 * HashMap 深入解析示例
 *
 * 核心知识点：
 * 1. HashMap 底层结构：数组 + 链表 + 红黑树（JDK 8+）
 * 2. 扩容机制：容量 * 2，阈值 = 容量 * 负载因子
 * 3. 红黑树转换条件：链表长度 >= 8 且数组长度 >= 64
 * 4. hash 扰动函数：(h = key.hashCode()) ^ (h >>> 16)
 * 5. 线程不安全场景
 *
 * 对应文档：docs/java-basics/collections.md
 */
public class HashMapDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===== 1. HashMap 扩容机制 =====");
        demonstrateResize();

        System.out.println("\n===== 2. 红黑树转换条件 =====");
        demonstrateTreeify();

        System.out.println("\n===== 3. hash 扰动函数 =====");
        demonstrateHashFunction();

        System.out.println("\n===== 4. ConcurrentModificationException =====");
        demonstrateConcurrentModification();

        System.out.println("\n===== 5. HashMap 常用操作 =====");
        demonstrateCommonOperations();
    }

    /**
     * 通过反射观察 HashMap 内部数组的扩容过程
     * 默认初始容量 16，负载因子 0.75，阈值 12
     * 当 size > threshold 时触发扩容，新容量 = 旧容量 * 2
     */
    static void demonstrateResize() throws Exception {
        HashMap<Integer, String> map = new HashMap<>(4, 0.75f);
        // 初始容量 4，阈值 = 4 * 0.75 = 3

        System.out.println("初始状态 - 容量: " + getCapacity(map) + ", 阈值: " + getThreshold(map));

        for (int i = 0; i < 10; i++) {
            map.put(i, "value" + i);
            System.out.println("put(" + i + ") → size=" + map.size()
                    + ", 容量=" + getCapacity(map)
                    + ", 阈值=" + getThreshold(map));
        }
        // 观察：容量从 4 → 8 → 16，每次翻倍
    }

    /**
     * 演示红黑树转换条件
     * 条件1：链表长度 >= 8（TREEIFY_THRESHOLD）
     * 条件2：数组长度 >= 64（MIN_TREEIFY_CAPACITY）
     * 如果链表长度 >= 8 但数组长度 < 64，会先扩容而不是树化
     */
    static void demonstrateTreeify() throws Exception {
        // 使用自定义 hashCode 让所有 key 落在同一个桶
        HashMap<BadHashKey, String> map = new HashMap<>(64);

        for (int i = 0; i < 12; i++) {
            map.put(new BadHashKey(i), "value" + i);
            System.out.println("put(key" + i + ") → size=" + map.size()
                    + ", 容量=" + getCapacity(map));
        }
        // 当第 8 个元素插入同一桶时，链表转为红黑树
        System.out.println("所有 key 的 hashCode 相同，触发红黑树转换");
    }

    /** 自定义 key 类，所有实例返回相同的 hashCode，模拟哈希碰撞 */
    static class BadHashKey {
        final int value;

        BadHashKey(int value) { this.value = value; }

        @Override
        public int hashCode() { return 1; } // 所有实例 hashCode 相同

        @Override
        public boolean equals(Object o) {
            return o instanceof BadHashKey other && this.value == other.value;
        }

        @Override
        public String toString() { return "Key(" + value + ")"; }
    }

    /**
     * hash 扰动函数分析
     * 目的：让高 16 位也参与运算，减少哈希碰撞
     * 公式：(h = key.hashCode()) ^ (h >>> 16)
     */
    static void demonstrateHashFunction() {
        String key = "Hello";
        int h = key.hashCode();
        int hash = h ^ (h >>> 16); // 扰动函数

        System.out.println("key: \"" + key + "\"");
        System.out.println("hashCode():     " + toBinaryString(h));
        System.out.println("h >>> 16:       " + toBinaryString(h >>> 16));
        System.out.println("hash (扰动后):  " + toBinaryString(hash));

        // 计算桶位置：(n-1) & hash（n 是数组长度，必须是 2 的幂）
        int n = 16;
        int index = (n - 1) & hash;
        System.out.println("数组长度 n=" + n + ", 桶位置: " + index);

        // 为什么容量必须是 2 的幂？
        // 因为 (n-1) & hash 等价于 hash % n，但位运算更快
        System.out.println("(n-1) & hash == hash % n: " + (((n - 1) & hash) == (Math.floorMod(hash, n))));
    }

    static String toBinaryString(int value) {
        return String.format("%32s", Integer.toBinaryString(value)).replace(' ', '0');
    }

    /**
     * ConcurrentModificationException 演示与正确的遍历删除方式
     */
    static void demonstrateConcurrentModification() {
        // ❌ 错误：增强 for 循环中删除元素
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        try {
            for (String s : list) {
                if ("b".equals(s)) {
                    list.remove(s); // ConcurrentModificationException!
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("❌ 增强 for 循环删除: ConcurrentModificationException");
        }

        // ✅ 正确方式1：Iterator.remove()
        list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if ("b".equals(it.next())) {
                it.remove();
            }
        }
        System.out.println("✅ Iterator 删除后: " + list);

        // ✅ 正确方式2：removeIf（JDK 8+）
        list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        list.removeIf("b"::equals);
        System.out.println("✅ removeIf 删除后: " + list);

        // ✅ 正确方式3：倒序遍历删除
        list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        for (int i = list.size() - 1; i >= 0; i--) {
            if ("b".equals(list.get(i))) {
                list.remove(i);
            }
        }
        System.out.println("✅ 倒序删除后: " + list);
    }

    /**
     * HashMap JDK 8+ 常用操作
     */
    static void demonstrateCommonOperations() {
        Map<String, Integer> map = new HashMap<>();
        map.put("Java", 1);
        map.put("Python", 2);
        map.put("Go", 3);

        // getOrDefault：key 不存在时返回默认值
        int val = map.getOrDefault("Rust", 0);
        System.out.println("getOrDefault(\"Rust\", 0): " + val);

        // putIfAbsent：key 不存在时才放入
        map.putIfAbsent("Java", 100); // 不会覆盖
        System.out.println("putIfAbsent 后 Java: " + map.get("Java")); // 仍然是 1

        // computeIfAbsent：key 不存在时计算并放入
        map.computeIfAbsent("Rust", k -> k.length());
        System.out.println("computeIfAbsent Rust: " + map.get("Rust")); // 4

        // merge：合并值
        map.merge("Java", 10, Integer::sum); // 1 + 10 = 11
        System.out.println("merge 后 Java: " + map.get("Java"));

        // forEach
        System.out.println("遍历:");
        map.forEach((k, v) -> System.out.println("  " + k + " → " + v));
    }

    // ==================== 反射工具方法 ====================

    static int getCapacity(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        return table == null ? 0 : table.length;
    }

    static int getThreshold(HashMap<?, ?> map) throws Exception {
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        return thresholdField.getInt(map);
    }
}

package com.example.advanced.collections;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

/**
 * 集合源码分析演示
 * <p>
 * 演示内容：
 * 1. HashMap put 流程跟踪（通过反射观察内部状态）
 * 2. ConcurrentHashMap 并发安全验证
 * 3. LinkedHashMap 插入顺序 vs 访问顺序（LRU）
 * </p>
 */
public class CollectionSourceDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 集合源码分析演示 ==========\n");

        demonstrateHashMapPut();
        System.out.println();

        demonstrateConcurrentHashMap();
        System.out.println();

        demonstrateLinkedHashMapLRU();
    }

    // ==================== 1. HashMap put 流程跟踪 ====================

    /**
     * 通过反射观察 HashMap 内部数组的变化，
     * 验证扩容机制和红黑树转换。
     */
    private static void demonstrateHashMapPut() throws Exception {
        System.out.println("--- 1. HashMap put 流程跟踪 ---");

        // 初始容量 4，负载因子 0.75，阈值 = 4 * 0.75 = 3
        HashMap<String, String> map = new HashMap<>(4, 0.75f);

        System.out.println("初始状态：");
        printHashMapInternals(map);

        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");
        System.out.println("\n放入 3 个元素后（达到阈值）：");
        printHashMapInternals(map);

        map.put("D", "4"); // 触发扩容
        System.out.println("\n放入第 4 个元素后（触发扩容）：");
        printHashMapInternals(map);

        // 演示 hash 扰动函数
        System.out.println("\n--- hash 扰动函数演示 ---");
        String key = "Hello";
        int h = key.hashCode();
        int hash = h ^ (h >>> 16);
        System.out.println("key = \"" + key + "\"");
        System.out.println("hashCode()       = " + Integer.toBinaryString(h) + " (" + h + ")");
        System.out.println("h >>> 16         = " + Integer.toBinaryString(h >>> 16));
        System.out.println("hash (扰动后)    = " + Integer.toBinaryString(hash) + " (" + hash + ")");
        System.out.println("bucket index (n=16): " + (hash & 15));
    }

    /**
     * 通过反射获取 HashMap 内部数组信息
     */
    @SuppressWarnings("unchecked")
    private static void printHashMapInternals(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);

        int capacity = (table == null) ? 0 : table.length;
        System.out.println("  size = " + map.size() + ", capacity = " + capacity);

        if (table != null) {
            int nonEmpty = 0;
            for (Object node : table) {
                if (node != null) nonEmpty++;
            }
            System.out.println("  非空桶数量 = " + nonEmpty);
        }
    }

    // ==================== 2. ConcurrentHashMap 并发安全验证 ====================

    /**
     * 对比 HashMap 和 ConcurrentHashMap 在并发写入下的表现
     */
    private static void demonstrateConcurrentHashMap() throws Exception {
        System.out.println("--- 2. ConcurrentHashMap 并发安全验证 ---");

        int threadCount = 10;
        int opsPerThread = 10000;
        int expectedSize = threadCount * opsPerThread;

        // HashMap 并发写入（不安全）
        Map<Integer, Integer> hashMap = new HashMap<>();
        CountDownLatch latch1 = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int offset = t * opsPerThread;
            new Thread(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    hashMap.put(offset + i, i);
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();
        System.out.println("HashMap 并发写入：");
        System.out.println("  期望 size = " + expectedSize);
        System.out.println("  实际 size = " + hashMap.size());
        System.out.println("  数据丢失 = " + (expectedSize != hashMap.size() ? "是 ✗" : "否 ✓"));

        // ConcurrentHashMap 并发写入（安全）
        ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int offset = t * opsPerThread;
            new Thread(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    concurrentMap.put(offset + i, i);
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.println("\nConcurrentHashMap 并发写入：");
        System.out.println("  期望 size = " + expectedSize);
        System.out.println("  实际 size = " + concurrentMap.size());
        System.out.println("  数据完整 = " + (expectedSize == concurrentMap.size() ? "是 ✓" : "否 ✗"));
    }

    // ==================== 3. LinkedHashMap LRU ====================

    /**
     * 对比 LinkedHashMap 的插入顺序和访问顺序模式，
     * 演示 LRU 缓存实现。
     */
    private static void demonstrateLinkedHashMapLRU() {
        System.out.println("--- 3. LinkedHashMap LRU 缓存演示 ---");

        // 插入顺序（默认）
        LinkedHashMap<String, Integer> insertionOrder = new LinkedHashMap<>();
        insertionOrder.put("C", 3);
        insertionOrder.put("A", 1);
        insertionOrder.put("B", 2);
        insertionOrder.get("C"); // 访问 C
        System.out.println("插入顺序模式: " + insertionOrder.keySet());
        // 输出: [C, A, B]（按插入顺序，get 不影响）

        // 访问顺序
        LinkedHashMap<String, Integer> accessOrder =
                new LinkedHashMap<>(16, 0.75f, true);
        accessOrder.put("C", 3);
        accessOrder.put("A", 1);
        accessOrder.put("B", 2);
        accessOrder.get("C"); // 访问 C，C 移到尾部
        System.out.println("访问顺序模式: " + accessOrder.keySet());
        // 输出: [A, B, C]（C 被访问后移到尾部）

        // LRU 缓存
        System.out.println("\nLRU 缓存演示（容量 = 3）：");
        LRUCache<String, String> lru = new LRUCache<>(3);
        lru.put("a", "1");
        lru.put("b", "2");
        lru.put("c", "3");
        System.out.println("初始: " + lru);

        lru.get("a"); // 访问 a，a 移到尾部
        System.out.println("访问 a 后: " + lru);

        lru.put("d", "4"); // 超过容量，移除最久未使用的 b
        System.out.println("放入 d 后: " + lru + "（b 被淘汰）");

        lru.put("e", "5"); // 移除 c
        System.out.println("放入 e 后: " + lru + "（c 被淘汰）");
    }

    /**
     * 基于 LinkedHashMap 的 LRU 缓存实现
     */
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(maxSize, 0.75f, true); // accessOrder = true
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}

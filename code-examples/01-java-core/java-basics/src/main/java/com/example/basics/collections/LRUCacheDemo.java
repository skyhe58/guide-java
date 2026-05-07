package com.example.basics.collections;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU 缓存实现示例
 *
 * 核心知识点：
 * 1. LinkedHashMap 维护双向链表保持顺序
 * 2. accessOrder=true 时按访问顺序排列
 * 3. 重写 removeEldestEntry 实现自动淘汰
 * 4. ArrayList 扩容机制（1.5 倍）
 *
 * 对应文档：docs/java-basics/collections.md
 */
public class LRUCacheDemo {

    /**
     * 基于 LinkedHashMap 实现的 LRU 缓存
     * accessOrder=true：每次 get/put 都会将元素移到链表尾部
     * removeEldestEntry：当 size > maxSize 时移除链表头部（最久未访问）的元素
     */
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            // initialCapacity, loadFactor, accessOrder
            super(maxSize, 0.75f, true); // accessOrder=true 是关键
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            boolean shouldRemove = size() > maxSize;
            if (shouldRemove) {
                System.out.println("  淘汰: " + eldest.getKey() + "=" + eldest.getValue());
            }
            return shouldRemove;
        }
    }

    public static void main(String[] args) {
        System.out.println("===== LRU 缓存演示 =====");
        LRUCache<String, Integer> cache = new LRUCache<>(3);

        // 放入 3 个元素
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        System.out.println("初始状态: " + cache); // {A=1, B=2, C=3}

        // 访问 A，A 移到尾部
        cache.get("A");
        System.out.println("访问 A 后: " + cache); // {B=2, C=3, A=1}

        // 放入 D，超出容量，淘汰最久未访问的 B
        cache.put("D", 4);
        System.out.println("放入 D 后: " + cache); // {C=3, A=1, D=4}

        // 放入 E，淘汰 C
        cache.put("E", 5);
        System.out.println("放入 E 后: " + cache); // {A=1, D=4, E=5}

        // 验证 B 和 C 已被淘汰
        System.out.println("B 是否存在: " + cache.containsKey("B")); // false
        System.out.println("C 是否存在: " + cache.containsKey("C")); // false

        System.out.println("\n===== ArrayList 扩容机制 =====");
        demonstrateArrayListResize();
    }

    /**
     * ArrayList 扩容机制演示
     * 默认初始容量 10，扩容因子 1.5 倍
     */
    static void demonstrateArrayListResize() {
        var list = new java.util.ArrayList<Integer>(4); // 初始容量 4
        System.out.println("初始容量: 4");

        for (int i = 0; i < 15; i++) {
            list.add(i);
            // 通过反射获取内部数组长度
            try {
                var field = java.util.ArrayList.class.getDeclaredField("elementData");
                field.setAccessible(true);
                Object[] elementData = (Object[]) field.get(list);
                if (i == 0 || elementData.length != getLastCapacity(list, i)) {
                    System.out.println("add(" + i + ") → size=" + list.size()
                            + ", 内部数组长度=" + elementData.length);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        // 扩容过程：4 → 6 → 9 → 13 → 19（每次 1.5 倍）
    }

    private static int getLastCapacity(java.util.ArrayList<?> list, int index) {
        if (index == 0) return 0;
        try {
            var field = java.util.ArrayList.class.getDeclaredField("elementData");
            field.setAccessible(true);
            return ((Object[]) field.get(list)).length;
        } catch (Exception e) {
            return 0;
        }
    }
}

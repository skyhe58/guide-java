package com.example.basics.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashMap 核心行为测试
 */
class HashMapTest {

    @Test
    @DisplayName("HashMap 在添加元素后 size 正确增长")
    void shouldTrackSizeCorrectly() {
        HashMap<Integer, String> map = new HashMap<>(4, 0.75f);
        for (int i = 0; i < 10; i++) {
            map.put(i, "v" + i);
            assertEquals(i + 1, map.size());
        }
    }

    @Test
    @DisplayName("HashMap 扩容后所有元素仍可访问")
    void shouldRetainAllElementsAfterResize() {
        // 初始容量 4，负载因子 0.75，阈值 3
        // 添加超过阈值的元素会触发扩容
        HashMap<Integer, String> map = new HashMap<>(4, 0.75f);
        for (int i = 0; i < 20; i++) {
            map.put(i, "value" + i);
        }
        // 扩容后所有元素仍然可以正确访问
        assertEquals(20, map.size());
        for (int i = 0; i < 20; i++) {
            assertEquals("value" + i, map.get(i));
        }
    }

    @Test
    @DisplayName("HashMap put 相同 key 覆盖 value")
    void shouldOverrideValueForSameKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        map.put("key", 2);
        assertEquals(2, map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    @DisplayName("HashMap 支持 null key 和 null value")
    void shouldSupportNullKeyAndValue() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "nullKey");
        map.put("key", null);
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("key"));
    }

    @Test
    @DisplayName("getOrDefault 在 key 不存在时返回默认值")
    void shouldReturnDefaultWhenKeyNotFound() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertEquals(1, map.getOrDefault("a", 0));
        assertEquals(0, map.getOrDefault("b", 0));
    }

    @Test
    @DisplayName("computeIfAbsent 在 key 不存在时计算并放入")
    void shouldComputeIfAbsent() {
        Map<String, List<String>> map = new HashMap<>();
        map.computeIfAbsent("list", k -> new ArrayList<>()).add("item1");
        map.computeIfAbsent("list", k -> new ArrayList<>()).add("item2");
        assertEquals(List.of("item1", "item2"), map.get("list"));
    }

    @Test
    @DisplayName("LRU 缓存淘汰最久未访问的元素")
    void shouldEvictLeastRecentlyUsed() {
        var cache = new LRUCacheDemo.LRUCache<String, Integer>(3);
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);

        cache.get("A"); // 访问 A，A 移到尾部

        cache.put("D", 4); // 超出容量，淘汰最久未访问的 B

        assertFalse(cache.containsKey("B"));
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
    }

    @Test
    @DisplayName("ConcurrentModificationException 在增强 for 循环中删除时抛出")
    void shouldThrowConcurrentModificationException() {
        // 使用更多元素确保触发 CME（3 个元素删除中间元素可能不触发）
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertThrows(ConcurrentModificationException.class, () -> {
            for (String s : list) {
                if ("b".equals(s)) {
                    list.remove(s);
                }
            }
        });
    }

    @Test
    @DisplayName("Iterator.remove 安全删除元素")
    void shouldSafelyRemoveWithIterator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if ("b".equals(it.next())) {
                it.remove();
            }
        }
        assertEquals(List.of("a", "c"), list);
    }

    @Test
    @DisplayName("removeIf 安全删除元素")
    void shouldSafelyRemoveWithRemoveIf() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        list.removeIf("b"::equals);
        assertEquals(List.of("a", "c"), list);
    }
}

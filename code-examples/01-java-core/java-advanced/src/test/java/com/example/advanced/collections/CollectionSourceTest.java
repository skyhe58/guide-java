package com.example.advanced.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集合源码相关测试 —— ConcurrentHashMap 并发安全验证
 */
class CollectionSourceTest {

    // ==================== ConcurrentHashMap 并发安全测试 ====================

    @Test
    @DisplayName("ConcurrentHashMap 并发写入应保证数据完整性")
    void concurrentHashMapShouldBeThreadSafe() throws Exception {
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        int threadCount = 10;
        int opsPerThread = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int offset = t * opsPerThread;
            new Thread(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    map.put(offset + i, i);
                }
                latch.countDown();
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(threadCount * opsPerThread, map.size(),
                "ConcurrentHashMap 并发写入不应丢失数据");
    }

    @Test
    @DisplayName("ConcurrentHashMap 并发 compute 应保证原子性")
    void concurrentHashMapComputeShouldBeAtomic() throws Exception {
        ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
        map.put("counter", new AtomicInteger(0));

        int threadCount = 10;
        int opsPerThread = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    map.get("counter").incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(threadCount * opsPerThread, map.get("counter").get(),
                "并发 increment 应保证原子性");
    }

    // ==================== LinkedHashMap LRU 测试 ====================

    @Test
    @DisplayName("LRU 缓存应在超过容量时淘汰最久未使用的元素")
    void lruCacheShouldEvictLeastRecentlyUsed() {
        CollectionSourceDemo.LRUCache<String, Integer> lru =
                new CollectionSourceDemo.LRUCache<>(3);

        lru.put("a", 1);
        lru.put("b", 2);
        lru.put("c", 3);
        assertEquals(3, lru.size());

        // 访问 a，使 a 成为最近使用
        lru.get("a");

        // 放入 d，应淘汰最久未使用的 b
        lru.put("d", 4);
        assertEquals(3, lru.size());
        assertFalse(lru.containsKey("b"), "b 应被淘汰");
        assertTrue(lru.containsKey("a"), "a 最近被访问，不应被淘汰");
        assertTrue(lru.containsKey("c"), "c 不应被淘汰");
        assertTrue(lru.containsKey("d"), "d 刚放入，不应被淘汰");
    }

    @Test
    @DisplayName("LinkedHashMap 默认按插入顺序排列")
    void linkedHashMapShouldMaintainInsertionOrder() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("C", 3);
        map.put("A", 1);
        map.put("B", 2);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("C", "A", "B"), keys);
    }

    @Test
    @DisplayName("LinkedHashMap accessOrder=true 时按访问顺序排列")
    void linkedHashMapAccessOrderShouldReorderOnGet() {
        LinkedHashMap<String, Integer> map =
                new LinkedHashMap<>(16, 0.75f, true);
        map.put("C", 3);
        map.put("A", 1);
        map.put("B", 2);

        map.get("C"); // 访问 C，C 移到尾部

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("A", "B", "C"), keys);
    }
}

package com.example.database.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 雪花算法唯一性和有序性验证
 */
@DisplayName("雪花算法测试")
class SnowflakeTest {

    @Test
    @DisplayName("生成的 ID 应为正数")
    void shouldGeneratePositiveId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        for (int i = 0; i < 100; i++) {
            long id = generator.nextId();
            assertTrue(id > 0, "ID 应为正数，实际值: " + id);
        }
    }

    @Test
    @DisplayName("单线程下生成的 ID 应唯一")
    void shouldGenerateUniqueIdsSingleThread() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        int count = 10_000;

        for (int i = 0; i < count; i++) {
            long id = generator.nextId();
            assertTrue(ids.add(id), "发现重复 ID: " + id);
        }

        assertEquals(count, ids.size(), "生成的 ID 数量应等于请求数量");
    }

    @Test
    @DisplayName("单线程下生成的 ID 应趋势递增")
    void shouldGenerateOrderedIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        long previousId = 0;

        for (int i = 0; i < 10_000; i++) {
            long id = generator.nextId();
            assertTrue(id > previousId,
                    String.format("ID 应递增，前一个: %d, 当前: %d", previousId, id));
            previousId = id;
        }
    }

    @Test
    @DisplayName("多线程下生成的 ID 应唯一")
    void shouldGenerateUniqueIdsMultiThread() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int threadCount = 10;
        int idsPerThread = 1_000;
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = generator.nextId();
                        assertTrue(allIds.add(id), "发现重复 ID: " + id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount * idsPerThread, allIds.size(),
                "所有线程生成的 ID 总数应等于请求总数");
    }

    @Test
    @DisplayName("不同机器 ID 生成的 ID 应不同")
    void shouldGenerateDifferentIdsForDifferentWorkers() {
        SnowflakeIdGenerator generator1 = new SnowflakeIdGenerator(1, 1);
        SnowflakeIdGenerator generator2 = new SnowflakeIdGenerator(2, 1);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            assertTrue(ids.add(generator1.nextId()), "generator1 产生重复 ID");
            assertTrue(ids.add(generator2.nextId()), "generator2 产生重复 ID");
        }

        assertEquals(200, ids.size());
    }

    @Test
    @DisplayName("机器 ID 超出范围应抛异常")
    void shouldThrowExceptionForInvalidWorkerId() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(32, 1),
                "机器 ID 超过 31 应抛异常");

        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(-1, 1),
                "机器 ID 为负数应抛异常");
    }

    @Test
    @DisplayName("数据中心 ID 超出范围应抛异常")
    void shouldThrowExceptionForInvalidDatacenterId() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(1, 32),
                "数据中心 ID 超过 31 应抛异常");

        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(1, -1),
                "数据中心 ID 为负数应抛异常");
    }

    @Test
    @DisplayName("getter 方法应返回正确的值")
    void shouldReturnCorrectWorkerAndDatacenterId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(5, 10);
        assertEquals(5, generator.getWorkerId());
        assertEquals(10, generator.getDatacenterId());
    }
}

package com.example.concurrent.cas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CAS 与原子操作正确性验证
 */
@DisplayName("CAS 与原子类测试")
class CASTest {

    @Test
    @DisplayName("AtomicInteger CAS 操作正确性")
    void shouldPerformCASCorrectly() {
        AtomicInteger atomic = new AtomicInteger(10);

        // CAS 成功：期望值匹配
        assertTrue(atomic.compareAndSet(10, 20), "期望值匹配时 CAS 应成功");
        assertEquals(20, atomic.get());

        // CAS 失败：期望值不匹配
        assertFalse(atomic.compareAndSet(10, 30), "期望值不匹配时 CAS 应失败");
        assertEquals(20, atomic.get(), "CAS 失败后值不应改变");
    }

    @Test
    @DisplayName("AtomicInteger 多线程并发递增正确性")
    void shouldIncrementAtomicallyUnderConcurrency() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        int incrementPerThread = 100_000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementPerThread; j++) {
                    counter.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount * incrementPerThread, counter.get(),
                "多线程并发递增结果应精确");
    }

    @Test
    @DisplayName("AtomicStampedReference 解决 ABA 问题")
    void shouldDetectABAWithStampedReference() {
        AtomicStampedReference<String> ref =
                new AtomicStampedReference<>("A", 0);

        // 记录初始版本号
        int initialStamp = ref.getStamp();

        // 模拟 ABA：A → B → A
        ref.compareAndSet("A", "B", initialStamp, initialStamp + 1);
        ref.compareAndSet("B", "A", initialStamp + 1, initialStamp + 2);

        // 值虽然回到 A，但版本号已变化
        assertEquals("A", ref.getReference(), "值应回到 A");
        assertEquals(initialStamp + 2, ref.getStamp(), "版本号应递增到 2");

        // 使用旧版本号的 CAS 应失败
        boolean success = ref.compareAndSet("A", "C", initialStamp, initialStamp + 1);
        assertFalse(success, "旧版本号的 CAS 应失败（检测到 ABA）");
    }

    @Test
    @DisplayName("LongAdder 多线程并发累加正确性")
    void shouldAccumulateCorrectlyWithLongAdder() throws Exception {
        LongAdder adder = new LongAdder();
        int threadCount = 10;
        int incrementPerThread = 100_000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementPerThread; j++) {
                    adder.increment();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount * incrementPerThread, adder.sum(),
                "LongAdder 并发累加结果应正确");
    }

    @Test
    @DisplayName("AtomicInteger getAndUpdate 操作正确性")
    void shouldGetAndUpdateAtomically() {
        AtomicInteger atomic = new AtomicInteger(10);

        int oldValue = atomic.getAndUpdate(v -> v * 2);
        assertEquals(10, oldValue, "getAndUpdate 应返回旧值");
        assertEquals(20, atomic.get(), "更新后值应为 20");

        int newValue = atomic.updateAndGet(v -> v + 5);
        assertEquals(25, newValue, "updateAndGet 应返回新值");
    }

    @Test
    @DisplayName("AtomicLong 多线程并发递减正确性")
    void shouldDecrementAtomicallyUnderConcurrency() throws Exception {
        int threadCount = 8;
        int decrementPerThread = 50_000;
        long initialValue = (long) threadCount * decrementPerThread;
        AtomicLong counter = new AtomicLong(initialValue);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < decrementPerThread; j++) {
                    counter.decrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(0, counter.get(),
                "多线程并发递减后结果应为 0");
    }
}

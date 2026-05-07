package com.example.patterns.creational;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单例模式测试 — 线程安全验证、反射破坏测试
 */
@DisplayName("单例模式测试")
class SingletonTest {

    @BeforeEach
    void setUp() {
        // 重置 DCL 单例以便每个测试独立
        DCLSingleton.resetForTesting();
    }

    // ==================== 基本单例验证 ====================

    @Test
    @DisplayName("饿汉式：多次获取应返回同一实例")
    void eagerSingleton_shouldReturnSameInstance() {
        EagerSingleton s1 = EagerSingleton.getInstance();
        EagerSingleton s2 = EagerSingleton.getInstance();
        assertSame(s1, s2, "饿汉式单例应返回同一实例");
    }

    @Test
    @DisplayName("懒汉式：多次获取应返回同一实例")
    void lazySingleton_shouldReturnSameInstance() {
        LazySingleton s1 = LazySingleton.getInstance();
        LazySingleton s2 = LazySingleton.getInstance();
        assertSame(s1, s2, "懒汉式单例应返回同一实例");
    }

    @Test
    @DisplayName("DCL：多次获取应返回同一实例")
    void dclSingleton_shouldReturnSameInstance() {
        DCLSingleton s1 = DCLSingleton.getInstance();
        DCLSingleton s2 = DCLSingleton.getInstance();
        assertSame(s1, s2, "DCL 单例应返回同一实例");
    }

    @Test
    @DisplayName("静态内部类：多次获取应返回同一实例")
    void innerClassSingleton_shouldReturnSameInstance() {
        InnerClassSingleton s1 = InnerClassSingleton.getInstance();
        InnerClassSingleton s2 = InnerClassSingleton.getInstance();
        assertSame(s1, s2, "静态内部类单例应返回同一实例");
    }

    @Test
    @DisplayName("枚举：多次获取应返回同一实例")
    void enumSingleton_shouldReturnSameInstance() {
        EnumSingleton s1 = EnumSingleton.INSTANCE;
        EnumSingleton s2 = EnumSingleton.INSTANCE;
        assertSame(s1, s2, "枚举单例应返回同一实例");
    }

    // ==================== 线程安全验证 ====================

    @Test
    @DisplayName("DCL 单例在多线程环境下应保持唯一")
    void dclSingleton_shouldBeThreadSafe() throws Exception {
        int threadCount = 50;
        Set<DCLSingleton> instances = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 所有线程同时开始
                    instances.add(DCLSingleton.getInstance());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 触发所有线程同时执行
        endLatch.await();
        executor.shutdown();

        assertEquals(1, instances.size(),
                "多线程并发获取 DCL 单例，应只有一个实例");
    }

    // ==================== 反射破坏测试 ====================

    @Test
    @DisplayName("反射可以破坏 DCL 单例")
    void reflection_shouldBreakDCLSingleton() throws Exception {
        DCLSingleton original = DCLSingleton.getInstance();

        Constructor<DCLSingleton> constructor = DCLSingleton.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DCLSingleton reflected = constructor.newInstance();

        assertNotSame(original, reflected,
                "反射创建的实例应与原单例不同（单例被破坏）");
    }

    @Test
    @DisplayName("枚举单例不能被反射破坏")
    void reflection_shouldNotBreakEnumSingleton() {
        assertThrows(Exception.class, () -> {
            Constructor<EnumSingleton> constructor =
                    EnumSingleton.class.getDeclaredConstructor(String.class, int.class);
            constructor.setAccessible(true);
            constructor.newInstance("INSTANCE", 0);
        }, "枚举单例应阻止反射创建实例");
    }

    // ==================== 序列化防护测试 ====================

    @Test
    @DisplayName("SafeSingleton 的 readResolve 应防止序列化破坏")
    void serialization_shouldNotBreakSafeSingleton() throws Exception {
        SafeSingleton original = SafeSingleton.getInstance();

        // 序列化
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        // 反序列化
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        SafeSingleton deserialized = (SafeSingleton) ois.readObject();
        ois.close();

        assertSame(original, deserialized,
                "readResolve 应确保反序列化返回同一实例");
    }
}

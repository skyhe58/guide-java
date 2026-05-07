package com.example.patterns.creational;

/**
 * 懒汉式单例 — synchronized 保证线程安全
 * <p>
 * 优点：支持懒加载
 * 缺点：每次调用 getInstance() 都加锁，性能差
 * </p>
 */
public class LazySingleton {

    private static LazySingleton instance;

    private LazySingleton() {}

    public static synchronized LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }
}

package com.example.patterns.creational;

/**
 * 饿汉式单例 — 类加载时创建实例
 * <p>
 * 优点：实现简单，JVM 类加载机制保证线程安全
 * 缺点：不支持懒加载，类加载即创建实例
 * </p>
 */
public class EagerSingleton {

    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {
        // 私有构造方法，防止外部 new
    }

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}

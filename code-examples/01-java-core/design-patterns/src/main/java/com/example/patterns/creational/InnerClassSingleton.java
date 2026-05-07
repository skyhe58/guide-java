package com.example.patterns.creational;

/**
 * 静态内部类单例
 * <p>
 * 利用 JVM 类加载机制保证线程安全：
 * - Holder 类只有在 getInstance() 被调用时才会加载
 * - 类加载过程由 JVM 保证线程安全
 * - 兼具懒加载和高性能
 * </p>
 */
public class InnerClassSingleton {

    private InnerClassSingleton() {}

    // 静态内部类，只有在被引用时才会加载
    private static class Holder {
        private static final InnerClassSingleton INSTANCE = new InnerClassSingleton();
    }

    public static InnerClassSingleton getInstance() {
        return Holder.INSTANCE;
    }
}

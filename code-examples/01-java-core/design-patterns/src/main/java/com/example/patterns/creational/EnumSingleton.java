package com.example.patterns.creational;

/**
 * 枚举单例 — Effective Java 推荐方式
 * <p>
 * 优点：
 * 1. 天然线程安全（JVM 保证枚举实例唯一）
 * 2. 天然防止反射攻击（JVM 禁止通过反射创建枚举实例）
 * 3. 天然防止序列化破坏（枚举序列化由 JVM 特殊处理）
 * 4. 代码最简洁
 * </p>
 */
public enum EnumSingleton {

    INSTANCE;

    // 可以定义实例变量和方法
    private int count = 0;

    public void doSomething() {
        count++;
        System.out.println("枚举单例执行业务方法，调用次数: " + count);
    }

    public int getCount() {
        return count;
    }
}

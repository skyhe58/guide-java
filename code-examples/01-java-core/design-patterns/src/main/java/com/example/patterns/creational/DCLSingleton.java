package com.example.patterns.creational;

/**
 * DCL 双重检查锁单例
 * <p>
 * volatile 防止指令重排序：
 * instance = new DCLSingleton() 实际包含三步：
 * 1. 分配内存空间
 * 2. 初始化对象
 * 3. 将引用指向内存地址
 * 指令重排可能导致 1→3→2，其他线程拿到未初始化的对象
 * </p>
 */
public class DCLSingleton {

    // volatile 是关键！防止指令重排序
    private static volatile DCLSingleton instance;

    DCLSingleton() {
        // 包级别访问，方便测试反射破坏
    }

    public static DCLSingleton getInstance() {
        if (instance == null) {                    // 第一次检查：避免不必要的加锁
            synchronized (DCLSingleton.class) {
                if (instance == null) {            // 第二次检查：防止重复创建
                    instance = new DCLSingleton();
                }
            }
        }
        return instance;
    }

    /**
     * 重置实例（仅用于测试）
     */
    static void resetForTesting() {
        instance = null;
    }
}

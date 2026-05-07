package com.example.patterns.creational;

import java.io.Serial;
import java.io.Serializable;

/**
 * 防序列化破坏的单例
 * <p>
 * 通过 readResolve() 方法防止序列化创建新实例：
 * 反序列化时 JVM 会检查是否存在 readResolve() 方法，
 * 如果存在则用其返回值替代反序列化创建的新对象。
 * </p>
 */
public class SafeSingleton implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SafeSingleton INSTANCE = new SafeSingleton();

    private SafeSingleton() {
        // 防止反射破坏：如果实例已存在，抛出异常
        if (INSTANCE != null) {
            throw new IllegalStateException("单例实例已存在，禁止通过反射创建！");
        }
    }

    public static SafeSingleton getInstance() {
        return INSTANCE;
    }

    /**
     * 防止序列化破坏：反序列化时返回已有实例
     */
    @Serial
    private Object readResolve() {
        return INSTANCE;
    }
}

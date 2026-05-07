package com.example.patterns.creational;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * 单例模式演示 — 5 种实现方式 + 反射/序列化破坏防护
 * <p>
 * 演示内容：
 * 1. 饿汉式（类加载时创建）
 * 2. 懒汉式（synchronized）
 * 3. DCL 双重检查锁（volatile + synchronized）
 * 4. 静态内部类（利用类加载机制）
 * 5. 枚举（推荐，天然防反射和序列化）
 * 6. 反射破坏单例演示
 * 7. 序列化破坏单例演示
 * </p>
 */
public class SingletonDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 单例模式 5 种实现方式 ==========\n");

        demonstrateEagerSingleton();
        demonstrateLazySingleton();
        demonstrateDCLSingleton();
        demonstrateInnerClassSingleton();
        demonstrateEnumSingleton();

        System.out.println("\n========== 反射和序列化破坏单例 ==========\n");
        demonstrateReflectionAttack();
        demonstrateSerializationAttack();
    }

    // ==================== 1. 饿汉式 ====================
    private static void demonstrateEagerSingleton() {
        System.out.println("--- 1. 饿汉式 ---");
        EagerSingleton s1 = EagerSingleton.getInstance();
        EagerSingleton s2 = EagerSingleton.getInstance();
        System.out.println("同一实例: " + (s1 == s2));
    }

    // ==================== 2. 懒汉式 ====================
    private static void demonstrateLazySingleton() {
        System.out.println("--- 2. 懒汉式 (synchronized) ---");
        LazySingleton s1 = LazySingleton.getInstance();
        LazySingleton s2 = LazySingleton.getInstance();
        System.out.println("同一实例: " + (s1 == s2));
    }

    // ==================== 3. DCL ====================
    private static void demonstrateDCLSingleton() {
        System.out.println("--- 3. DCL 双重检查锁 ---");
        DCLSingleton s1 = DCLSingleton.getInstance();
        DCLSingleton s2 = DCLSingleton.getInstance();
        System.out.println("同一实例: " + (s1 == s2));
    }

    // ==================== 4. 静态内部类 ====================
    private static void demonstrateInnerClassSingleton() {
        System.out.println("--- 4. 静态内部类 ---");
        InnerClassSingleton s1 = InnerClassSingleton.getInstance();
        InnerClassSingleton s2 = InnerClassSingleton.getInstance();
        System.out.println("同一实例: " + (s1 == s2));
    }

    // ==================== 5. 枚举 ====================
    private static void demonstrateEnumSingleton() {
        System.out.println("--- 5. 枚举单例（推荐） ---");
        EnumSingleton s1 = EnumSingleton.INSTANCE;
        EnumSingleton s2 = EnumSingleton.INSTANCE;
        System.out.println("同一实例: " + (s1 == s2));
        s1.doSomething();
    }

    // ==================== 6. 反射破坏 ====================
    private static void demonstrateReflectionAttack() throws Exception {
        System.out.println("--- 6. 反射破坏单例演示 ---");

        // 对普通单例的反射攻击
        DCLSingleton original = DCLSingleton.getInstance();
        Constructor<DCLSingleton> constructor = DCLSingleton.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DCLSingleton reflected = constructor.newInstance();
        System.out.println("DCL 单例被反射破坏: " + (original != reflected));

        // 枚举单例天然防反射
        try {
            Constructor<EnumSingleton> enumConstructor =
                    EnumSingleton.class.getDeclaredConstructor(String.class, int.class);
            enumConstructor.setAccessible(true);
            enumConstructor.newInstance("INSTANCE", 0);
        } catch (Exception e) {
            System.out.println("枚举单例防反射: " + e.getClass().getSimpleName()
                    + " - 无法通过反射创建枚举实例");
        }
    }

    // ==================== 7. 序列化破坏 ====================
    private static void demonstrateSerializationAttack() throws Exception {
        System.out.println("--- 7. 序列化破坏单例演示 ---");

        // SafeSingleton 实现了 readResolve 防护
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

        System.out.println("readResolve 防护有效: " + (original == deserialized));
    }
}

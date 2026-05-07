package com.example.basics.string;

/**
 * String 深入解析示例
 *
 * 核心知识点：
 * 1. String 不可变性原理（final char[]/byte[] + final class）
 * 2. String 常量池（String Pool）机制
 * 3. intern() 方法的作用
 * 4. String vs StringBuilder vs StringBuffer 性能对比
 * 5. JDK 9+ String 底层从 char[] 改为 byte[]（Compact Strings）
 *
 * 对应文档：docs/java-basics/string-deep-dive.md
 */
public class StringPoolDemo {

    public static void main(String[] args) {
        System.out.println("===== 1. String 常量池 =====");
        demonstrateStringPool();

        System.out.println("\n===== 2. intern() 方法 =====");
        demonstrateIntern();

        System.out.println("\n===== 3. String 拼接方式对比 =====");
        demonstrateConcatenation();

        System.out.println("\n===== 4. StringBuilder vs StringBuffer =====");
        demonstrateBuilderVsBuffer();

        System.out.println("\n===== 5. String 不可变性的好处 =====");
        demonstrateImmutability();
    }

    /**
     * String 常量池机制
     * 字面量字符串在编译期确定，存储在常量池中
     * new String() 在堆上创建新对象
     */
    static void demonstrateStringPool() {
        // 字面量：编译期放入常量池，相同字面量指向同一对象
        String s1 = "hello";
        String s2 = "hello";
        System.out.println("s1 == s2 (字面量): " + (s1 == s2)); // true

        // new String()：在堆上创建新对象
        String s3 = new String("hello");
        System.out.println("s1 == s3 (new): " + (s1 == s3));     // false
        System.out.println("s1.equals(s3): " + s1.equals(s3));    // true

        // 编译期常量拼接：编译器优化为一个字面量
        String s4 = "hel" + "lo"; // 编译期优化为 "hello"
        System.out.println("s1 == s4 (编译期拼接): " + (s1 == s4)); // true

        // 运行时拼接：创建新对象
        String prefix = "hel";
        String s5 = prefix + "lo"; // 运行时通过 StringBuilder 拼接
        System.out.println("s1 == s5 (运行时拼接): " + (s1 == s5)); // false

        // 面试经典题：new String("abc") 创建了几个对象？
        // 答：1 或 2 个。如果常量池中没有 "abc"，先在常量池创建一个，再在堆上创建一个。
        // 如果常量池中已有 "abc"，则只在堆上创建一个。
    }

    /**
     * intern() 方法：将字符串放入常量池并返回常量池中的引用
     * JDK 7+：常量池在堆中，intern() 可能直接存储堆对象的引用
     */
    static void demonstrateIntern() {
        String s1 = new String("java");
        String s2 = s1.intern(); // 返回常量池中的 "java"
        String s3 = "java";

        System.out.println("s1 == s2: " + (s1 == s2)); // false（s1 在堆上，s2 在常量池）
        System.out.println("s2 == s3: " + (s2 == s3)); // true（都指向常量池）

        // JDK 7+ 的特殊行为
        // 如果常量池中没有该字符串，intern() 会在常量池中存储堆对象的引用
        String s4 = new String("ja") + new String("va2"); // 堆上创建 "java2"
        s4.intern(); // 常量池中没有 "java2"，存储 s4 的引用
        String s5 = "java2";
        System.out.println("s4 == s5 (JDK7+): " + (s4 == s5)); // true（JDK 7+）
    }

    /**
     * 字符串拼接性能对比
     */
    static void demonstrateConcatenation() {
        int iterations = 50_000;

        // 方式1：String += （最慢，每次创建新对象）
        long start1 = System.currentTimeMillis();
        String result1 = "";
        for (int i = 0; i < iterations; i++) {
            result1 += "a"; // 每次都创建新的 String 和 StringBuilder
        }
        long time1 = System.currentTimeMillis() - start1;

        // 方式2：StringBuilder（最快，非线程安全）
        long start2 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            sb.append("a");
        }
        String result2 = sb.toString();
        long time2 = System.currentTimeMillis() - start2;

        // 方式3：StringBuffer（较快，线程安全）
        long start3 = System.currentTimeMillis();
        StringBuffer sbuf = new StringBuffer();
        for (int i = 0; i < iterations; i++) {
            sbuf.append("a");
        }
        String result3 = sbuf.toString();
        long time3 = System.currentTimeMillis() - start3;

        System.out.println("String +=     耗时: " + time1 + " ms");
        System.out.println("StringBuilder 耗时: " + time2 + " ms");
        System.out.println("StringBuffer  耗时: " + time3 + " ms");
        System.out.println("长度验证: " + result1.length() + ", " + result2.length() + ", " + result3.length());
    }

    /**
     * StringBuilder vs StringBuffer 对比
     */
    static void demonstrateBuilderVsBuffer() {
        // StringBuilder：非线程安全，性能更好，单线程场景首选
        StringBuilder builder = new StringBuilder("Hello");
        builder.append(" ").append("World");
        builder.insert(5, ",");
        builder.reverse();
        System.out.println("StringBuilder: " + builder);
        builder.reverse(); // 恢复

        // StringBuffer：线程安全（方法加 synchronized），多线程场景使用
        StringBuffer buffer = new StringBuffer("Hello");
        buffer.append(" ").append("World");
        System.out.println("StringBuffer: " + buffer);

        // 初始容量：默认 16，超出时扩容为 (旧容量 * 2 + 2)
        StringBuilder sb = new StringBuilder(); // 容量 16
        System.out.println("初始容量: " + sb.capacity()); // 16
        sb.append("12345678901234567"); // 超过 16
        System.out.println("扩容后容量: " + sb.capacity()); // 34 = 16 * 2 + 2
    }

    /**
     * String 不可变性的好处
     */
    static void demonstrateImmutability() {
        // 1. 常量池优化：相同字面量共享同一对象，节省内存
        String a = "immutable";
        String b = "immutable";
        System.out.println("常量池共享: " + (a == b)); // true

        // 2. 线程安全：不可变对象天然线程安全
        // 多个线程可以安全地共享同一个 String 对象

        // 3. hashCode 缓存：String 的 hashCode 只计算一次
        String key = "HashMap Key";
        int hash1 = key.hashCode();
        int hash2 = key.hashCode(); // 直接返回缓存值
        System.out.println("hashCode 一致: " + (hash1 == hash2));

        // 4. 安全性：作为参数传递时不会被修改
        String filename = "important.txt";
        processFile(filename);
        System.out.println("原始文件名未被修改: " + filename);
    }

    static void processFile(String name) {
        name = "hacked.txt"; // 只是修改了局部变量，不影响调用者
    }
}

package com.example.basics.datatypes;

import java.util.Arrays;

/**
 * Java 值传递示例
 *
 * 核心知识点：
 * 1. Java 只有值传递，没有引用传递
 * 2. 基本类型传递的是值的副本
 * 3. 引用类型传递的是引用（地址）的副本
 * 4. 交换引用不会影响原始变量
 *
 * 对应文档：docs/java-basics/value-passing.md
 */
public class ValuePassingDemo {

    public static void main(String[] args) {
        System.out.println("===== 1. 基本类型的值传递 =====");
        demonstratePrimitivePass();

        System.out.println("\n===== 2. 引用类型的值传递 =====");
        demonstrateReferencePass();

        System.out.println("\n===== 3. String 的特殊性 =====");
        demonstrateStringPass();

        System.out.println("\n===== 4. 数组的传递 =====");
        demonstrateArrayPass();
    }

    /**
     * 基本类型传递：传递的是值的副本，方法内修改不影响原始值
     */
    static void demonstratePrimitivePass() {
        int num = 10;
        System.out.println("调用前: num = " + num);
        modifyPrimitive(num);
        System.out.println("调用后: num = " + num); // 仍然是 10
    }

    static void modifyPrimitive(int value) {
        value = 100; // 修改的是副本，不影响原始变量
        System.out.println("方法内: value = " + value);
    }

    /**
     * 引用类型传递：传递的是引用的副本
     * 通过引用副本可以修改对象内容，但不能改变原始引用指向
     */
    static void demonstrateReferencePass() {
        int[] arr = {1, 2, 3};
        System.out.println("调用前: " + Arrays.toString(arr));

        // 可以通过引用副本修改对象内容
        modifyArray(arr);
        System.out.println("修改内容后: " + Arrays.toString(arr)); // [99, 2, 3]

        // 但不能改变原始引用的指向
        reassignArray(arr);
        System.out.println("重新赋值后: " + Arrays.toString(arr)); // 仍然是 [99, 2, 3]
    }

    static void modifyArray(int[] array) {
        array[0] = 99; // 通过引用副本修改对象内容 → 影响原始对象
    }

    static void reassignArray(int[] array) {
        array = new int[]{100, 200, 300}; // 修改引用副本的指向 → 不影响原始引用
        System.out.println("方法内重新赋值: " + Arrays.toString(array));
    }

    /**
     * String 的不可变性导致看起来像"值传递"
     * 实际上传递的仍然是引用的副本，但 String 不可变，任何修改都会创建新对象
     */
    static void demonstrateStringPass() {
        String str = "Hello";
        System.out.println("调用前: " + str);
        modifyString(str);
        System.out.println("调用后: " + str); // 仍然是 "Hello"
    }

    static void modifyString(String s) {
        s = s + " World"; // 创建了新的 String 对象，s 指向新对象
        System.out.println("方法内: " + s);   // "Hello World"
    }

    /**
     * 经典面试题：swap 方法能否交换两个 Integer？
     */
    static void demonstrateArrayPass() {
        Integer a = 1;
        Integer b = 2;
        System.out.println("交换前: a=" + a + ", b=" + b);
        swap(a, b);
        System.out.println("交换后: a=" + a + ", b=" + b); // 仍然是 a=1, b=2
        // 原因：swap 方法内交换的是引用副本的指向，不影响原始引用
    }

    static void swap(Integer x, Integer y) {
        Integer temp = x;
        x = y;
        y = temp;
        // x 和 y 是引用的副本，交换它们不影响调用者的 a 和 b
    }
}

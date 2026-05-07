package com.example.basics.generics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 泛型深入解析示例
 *
 * 核心知识点：
 * 1. 类型擦除（Type Erasure）原理
 * 2. 通配符：?、? extends T、? super T
 * 3. PECS 原则（Producer Extends, Consumer Super）
 * 4. 泛型类、泛型方法、泛型接口
 * 5. 泛型的限制与陷阱
 *
 * 对应文档：docs/java-basics/generics.md
 */
public class GenericsDemo {

    // ==================== 泛型类 ====================

    /** 泛型容器类 */
    static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() { return key; }
        public V getValue() { return value; }

        @Override
        public String toString() {
            return "Pair{" + key + " = " + value + "}";
        }
    }

    // ==================== 泛型接口 ====================

    interface Converter<F, T> {
        T convert(F from);
    }

    // ==================== 有界类型参数 ====================

    /** 要求 T 必须是 Comparable 的子类 */
    static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /** 多重边界：T 必须同时实现多个接口 */
    static <T extends Comparable<T> & java.io.Serializable> void process(T item) {
        System.out.println("处理: " + item);
    }

    public static void main(String[] args) {
        System.out.println("===== 1. 泛型类与泛型方法 =====");
        demonstrateBasicGenerics();

        System.out.println("\n===== 2. 类型擦除 =====");
        demonstrateTypeErasure();

        System.out.println("\n===== 3. 通配符与 PECS =====");
        demonstrateWildcards();

        System.out.println("\n===== 4. 泛型的限制 =====");
        demonstrateLimitations();
    }

    static void demonstrateBasicGenerics() {
        // 泛型类
        Pair<String, Integer> pair = new Pair<>("Java", 21);
        System.out.println("泛型类: " + pair);

        // 泛型方法
        String maxStr = max("apple", "banana");
        Integer maxInt = max(10, 20);
        System.out.println("max(\"apple\", \"banana\"): " + maxStr);
        System.out.println("max(10, 20): " + maxInt);

        // 泛型接口 + Lambda
        Converter<String, Integer> converter = Integer::parseInt;
        System.out.println("convert(\"123\"): " + converter.convert("123"));
    }

    /**
     * 类型擦除：编译后泛型信息被擦除，替换为 Object 或上界类型
     * 这是 Java 泛型与 C++ 模板的根本区别
     */
    static void demonstrateTypeErasure() {
        // 擦除后 List<String> 和 List<Integer> 的 Class 对象相同
        List<String> stringList = new ArrayList<>();
        List<Integer> intList = new ArrayList<>();
        System.out.println("List<String>.class == List<Integer>.class: "
                + (stringList.getClass() == intList.getClass())); // true

        // 擦除后无法使用 instanceof 检查泛型类型
        // if (stringList instanceof List<String>) {} // 编译错误
        System.out.println("只能检查原始类型: " + (stringList instanceof List<?>)); // true

        // 桥方法（Bridge Method）示例
        // 编译器会自动生成桥方法来保持多态性
        // 例如 Comparable<String> 擦除后变成 Comparable，
        // 编译器生成 compareTo(Object) 桥方法调用 compareTo(String)
    }

    /**
     * 通配符与 PECS 原则
     * PECS = Producer Extends, Consumer Super
     * - 如果只从集合中读取（生产者），用 ? extends T
     * - 如果只向集合中写入（消费者），用 ? super T
     * - 如果既读又写，不用通配符
     */
    static void demonstrateWildcards() {
        // ===== ? extends T（上界通配符）=====
        // 只能读取，不能写入（除了 null）
        List<Integer> intList = Arrays.asList(1, 2, 3);
        double sum = sumOfList(intList); // Integer extends Number
        System.out.println("sum(Integer): " + sum);

        List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        sum = sumOfList(doubleList); // Double extends Number
        System.out.println("sum(Double): " + sum);

        // ===== ? super T（下界通配符）=====
        // 只能写入，读取只能得到 Object
        List<Number> numberList = new ArrayList<>();
        addNumbers(numberList); // Number super Integer
        System.out.println("addNumbers: " + numberList);

        List<Object> objectList = new ArrayList<>();
        addNumbers(objectList); // Object super Integer
        System.out.println("addNumbers to Object list: " + objectList);

        // ===== PECS 实际应用 =====
        System.out.println("\n--- PECS 示例: copy ---");
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Number> dest = new ArrayList<>();
        copy(src, dest); // src 是 Producer(extends)，dest 是 Consumer(super)
        System.out.println("copy 结果: " + dest);
    }

    /** Producer Extends：从 list 中读取 Number */
    static double sumOfList(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) { // 可以安全地读取为 Number
            sum += n.doubleValue();
        }
        // list.add(1); // ❌ 编译错误：不能写入
        return sum;
    }

    /** Consumer Super：向 list 中写入 Integer */
    static void addNumbers(List<? super Integer> list) {
        list.add(1);  // ✅ 可以安全地写入 Integer
        list.add(2);
        list.add(3);
        // Integer n = list.get(0); // ❌ 编译错误：读取只能得到 Object
    }

    /** PECS 综合应用：从 src 读取，写入 dest */
    static <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T item : src) {
            dest.add(item);
        }
    }

    /**
     * 泛型的限制
     */
    static void demonstrateLimitations() {
        // 1. 不能用基本类型作为类型参数
        // List<int> list = new ArrayList<>(); // ❌ 编译错误
        List<Integer> list = new ArrayList<>(); // ✅ 使用包装类

        // 2. 不能创建泛型数组
        // T[] array = new T[10]; // ❌ 编译错误
        // 替代方案：
        @SuppressWarnings("unchecked")
        List<String>[] arrayOfLists = new ArrayList[10]; // 可以但有警告
        System.out.println("泛型数组替代方案: " + (arrayOfLists != null));

        // 3. 不能实例化类型参数
        // T obj = new T(); // ❌ 编译错误
        // 替代方案：传入 Class<T> 或 Supplier<T>

        // 4. 静态字段不能使用类的类型参数
        // static T field; // ❌ 编译错误

        System.out.println("泛型限制演示完成");
    }
}

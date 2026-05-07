package com.example.basics.reflection;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;

/**
 * 反射深入解析示例
 *
 * 核心知识点：
 * 1. 获取 Class 对象的三种方式
 * 2. Field/Method/Constructor 操作
 * 3. 访问私有成员（setAccessible）
 * 4. 反射性能对比
 * 5. 获取泛型信息
 *
 * 对应文档：docs/java-basics/reflection.md
 */
public class ReflectionDemo {

    /** 用于反射操作的目标类 */
    static class Person {
        private String name;
        private int age;
        private List<String> hobbies;

        public Person() {
            this.name = "Unknown";
            this.age = 0;
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }

        private String secretMethod() {
            return "这是私有方法的返回值";
        }

        public static String staticMethod() {
            return "这是静态方法";
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== 1. 获取 Class 对象 =====");
        demonstrateGetClass();

        System.out.println("\n===== 2. Constructor 操作 =====");
        demonstrateConstructor();

        System.out.println("\n===== 3. Field 操作 =====");
        demonstrateField();

        System.out.println("\n===== 4. Method 操作 =====");
        demonstrateMethod();

        System.out.println("\n===== 5. 反射性能对比 =====");
        demonstratePerformance();

        System.out.println("\n===== 6. 获取泛型信息 =====");
        demonstrateGenericType();
    }

    /**
     * 获取 Class 对象的三种方式
     * 三种方式获取的是同一个 Class 对象（JVM 中每个类只有一个）
     */
    static void demonstrateGetClass() throws ClassNotFoundException {
        // 方式1：类名.class（编译期确定，不触发类初始化）
        Class<Person> clazz1 = Person.class;

        // 方式2：对象.getClass()（运行时获取）
        Person person = new Person("Alice", 25);
        Class<?> clazz2 = person.getClass();

        // 方式3：Class.forName()（动态加载，触发类初始化）
        Class<?> clazz3 = Class.forName(
                "com.example.basics.reflection.ReflectionDemo$Person");

        // 验证三种方式返回同一个 Class 对象
        System.out.println("clazz1 == clazz2: " + (clazz1 == clazz2)); // true
        System.out.println("clazz2 == clazz3: " + (clazz2 == clazz3)); // true
        System.out.println("类名: " + clazz1.getSimpleName());
    }

    /**
     * Constructor 操作：创建实例
     */
    static void demonstrateConstructor() throws Exception {
        Class<Person> clazz = Person.class;

        // 获取所有公共构造方法
        Constructor<?>[] constructors = clazz.getConstructors();
        System.out.println("公共构造方法数量: " + constructors.length);

        // 无参构造
        Constructor<Person> noArgCtor = clazz.getDeclaredConstructor();
        Person p1 = noArgCtor.newInstance();
        System.out.println("无参构造: " + p1);

        // 有参构造
        Constructor<Person> argCtor = clazz.getDeclaredConstructor(String.class, int.class);
        Person p2 = argCtor.newInstance("Bob", 30);
        System.out.println("有参构造: " + p2);
    }

    /**
     * Field 操作：读取和修改字段（包括私有字段）
     */
    static void demonstrateField() throws Exception {
        Class<Person> clazz = Person.class;
        Person person = new Person("Alice", 25);

        // 获取所有声明的字段（包括私有的）
        Field[] fields = clazz.getDeclaredFields();
        System.out.println("声明的字段: " + Arrays.toString(
                Arrays.stream(fields).map(Field::getName).toArray()));

        // 读取私有字段
        Field nameField = clazz.getDeclaredField("name");
        nameField.setAccessible(true); // 突破 private 访问控制
        String name = (String) nameField.get(person);
        System.out.println("读取私有字段 name: " + name);

        // 修改私有字段
        nameField.set(person, "Charlie");
        System.out.println("修改后: " + person);

        // 读取 int 字段
        Field ageField = clazz.getDeclaredField("age");
        ageField.setAccessible(true);
        int age = ageField.getInt(person); // 基本类型用 getInt
        System.out.println("读取 age: " + age);
    }

    /**
     * Method 操作：调用方法（包括私有方法和静态方法）
     */
    static void demonstrateMethod() throws Exception {
        Class<Person> clazz = Person.class;
        Person person = new Person("Alice", 25);

        // 调用公共方法
        Method getName = clazz.getDeclaredMethod("getName");
        String name = (String) getName.invoke(person);
        System.out.println("调用 getName(): " + name);

        // 调用私有方法
        Method secretMethod = clazz.getDeclaredMethod("secretMethod");
        secretMethod.setAccessible(true);
        String secret = (String) secretMethod.invoke(person);
        System.out.println("调用私有方法: " + secret);

        // 调用静态方法（不需要实例）
        Method staticMethod = clazz.getDeclaredMethod("staticMethod");
        String result = (String) staticMethod.invoke(null);
        System.out.println("调用静态方法: " + result);
    }

    /**
     * 反射性能对比
     * 反射调用比直接调用慢 10-100 倍
     * 优化：缓存 Method 对象 + setAccessible(true)
     */
    static void demonstratePerformance() throws Exception {
        Person person = new Person("Alice", 25);
        int iterations = 1_000_000;

        // 直接调用
        long start1 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            person.getName();
        }
        long directTime = System.nanoTime() - start1;

        // 反射调用（未优化：每次查找 Method）
        long start2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Method m = Person.class.getDeclaredMethod("getName");
            m.invoke(person);
        }
        long reflectTime = System.nanoTime() - start2;

        // 反射调用（优化：缓存 Method + setAccessible）
        Method cachedMethod = Person.class.getDeclaredMethod("getName");
        cachedMethod.setAccessible(true);
        long start3 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            cachedMethod.invoke(person);
        }
        long optimizedTime = System.nanoTime() - start3;

        System.out.println("直接调用:     " + directTime / 1_000_000 + " ms");
        System.out.println("反射(未优化): " + reflectTime / 1_000_000 + " ms");
        System.out.println("反射(已优化): " + optimizedTime / 1_000_000 + " ms");
        System.out.println("优化后提升: " + (reflectTime / Math.max(optimizedTime, 1)) + " 倍");
    }

    /**
     * 获取泛型信息
     * 虽然运行时泛型被擦除，但字段/方法签名中的泛型信息保留在 Class 文件中
     */
    static void demonstrateGenericType() throws Exception {
        Field hobbiesField = Person.class.getDeclaredField("hobbies");

        // 获取泛型类型
        Type genericType = hobbiesField.getGenericType();
        System.out.println("泛型类型: " + genericType);

        if (genericType instanceof ParameterizedType paramType) {
            Type rawType = paramType.getRawType();
            Type[] typeArgs = paramType.getActualTypeArguments();
            System.out.println("原始类型: " + rawType);
            System.out.println("类型参数: " + Arrays.toString(typeArgs));
        }
    }
}

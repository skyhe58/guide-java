package com.example.basics.oop;

import java.util.Objects;

/**
 * 面向对象核心示例
 *
 * 核心知识点：
 * 1. 多态的实现原理（虚方法表）
 * 2. 向上转型与向下转型
 * 3. equals/hashCode 契约
 * 4. 内部类的四种形式
 * 5. 抽象类 vs 接口
 *
 * 对应文档：docs/java-basics/oop.md
 */
public class OopDemo {

    public static void main(String[] args) {
        System.out.println("===== 1. 多态演示 =====");
        demonstratePolymorphism();

        System.out.println("\n===== 2. 向上转型与向下转型 =====");
        demonstrateCasting();

        System.out.println("\n===== 3. equals/hashCode 契约 =====");
        demonstrateEqualsHashCode();

        System.out.println("\n===== 4. 内部类 =====");
        demonstrateInnerClasses();
    }

    // ==================== 多态 ====================

    /** 基类：动物 */
    static abstract class Animal {
        String name;

        Animal(String name) { this.name = name; }

        /** 抽象方法：子类必须实现 */
        abstract String speak();

        /** 普通方法：子类可以重写 */
        String describe() {
            return name + " says: " + speak();
        }
    }

    static class Dog extends Animal {
        Dog(String name) { super(name); }

        @Override
        String speak() { return "汪汪!"; }
    }

    static class Cat extends Animal {
        Cat(String name) { super(name); }

        @Override
        String speak() { return "喵喵~"; }

        /** Cat 特有方法 */
        String purr() { return "呼噜呼噜..."; }
    }

    /**
     * 多态：父类引用指向子类对象，运行时调用实际类型的方法
     * 底层原理：JVM 通过虚方法表（vtable）实现动态分派
     */
    static void demonstratePolymorphism() {
        Animal dog = new Dog("旺财");
        Animal cat = new Cat("咪咪");

        // 运行时根据实际类型调用对应的 speak() 方法
        System.out.println(dog.describe()); // 旺财 says: 汪汪!
        System.out.println(cat.describe()); // 咪咪 says: 喵喵~

        // 多态数组
        Animal[] animals = {new Dog("小黑"), new Cat("小白"), new Dog("大黄")};
        for (Animal a : animals) {
            System.out.println(a.describe());
        }
    }

    /**
     * 向上转型（安全，自动）与向下转型（需要强制转换，可能 ClassCastException）
     */
    static void demonstrateCasting() {
        // 向上转型：子类 → 父类（自动，安全）
        Animal animal = new Cat("小花"); // Cat → Animal
        System.out.println("向上转型: " + animal.describe());

        // 向下转型前先用 instanceof 检查
        if (animal instanceof Cat cat) { // JDK 16+ Pattern Matching
            System.out.println("向下转型成功: " + cat.purr());
        }

        // 错误的向下转型
        Animal dog = new Dog("小黑");
        try {
            Cat wrongCast = (Cat) dog; // ClassCastException!
        } catch (ClassCastException e) {
            System.out.println("向下转型失败: Dog 不能转为 Cat");
        }

        // instanceof 的正确使用
        System.out.println("dog instanceof Animal: " + (dog instanceof Animal)); // true
        System.out.println("dog instanceof Cat: " + (dog instanceof Cat));       // false
    }

    // ==================== equals/hashCode ====================

    /**
     * 正确实现 equals 和 hashCode 的示例类
     * 契约：
     * 1. equals 相等的对象，hashCode 必须相等
     * 2. hashCode 相等的对象，equals 不一定相等（哈希碰撞）
     * 3. 重写 equals 必须同时重写 hashCode
     */
    static class Person {
        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;                    // 自反性
            if (o == null || getClass() != o.getClass()) return false; // 类型检查
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age); // 使用参与 equals 比较的所有字段
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }

    static void demonstrateEqualsHashCode() {
        Person p1 = new Person("Alice", 25);
        Person p2 = new Person("Alice", 25);
        Person p3 = new Person("Bob", 30);

        // equals 比较
        System.out.println("p1.equals(p2): " + p1.equals(p2)); // true
        System.out.println("p1.equals(p3): " + p1.equals(p3)); // false

        // hashCode 契约验证
        System.out.println("p1.hashCode == p2.hashCode: " + (p1.hashCode() == p2.hashCode())); // true
        System.out.println("p1 == p2: " + (p1 == p2)); // false（不同对象）

        // 在 HashSet 中的行为
        var set = new java.util.HashSet<Person>();
        set.add(p1);
        set.add(p2); // equals 相等，不会重复添加
        System.out.println("HashSet 大小: " + set.size()); // 1
    }

    // ==================== 内部类 ====================

    /** 1. 成员内部类：可以访问外部类的所有成员（包括 private） */
    private String outerField = "外部类字段";

    class MemberInner {
        void show() {
            System.out.println("成员内部类访问: " + outerField);
        }
    }

    /** 2. 静态内部类：不持有外部类引用，只能访问外部类的静态成员 */
    static class StaticInner {
        void show() {
            System.out.println("静态内部类（不依赖外部类实例）");
        }
    }

    /** 3. 接口用于演示匿名内部类 */
    interface Greeting {
        String greet(String name);
    }

    static void demonstrateInnerClasses() {
        // 1. 成员内部类：需要外部类实例
        OopDemo outer = new OopDemo();
        MemberInner memberInner = outer.new MemberInner();
        memberInner.show();

        // 2. 静态内部类：不需要外部类实例
        StaticInner staticInner = new StaticInner();
        staticInner.show();

        // 3. 匿名内部类
        Greeting anonymousGreeting = new Greeting() {
            @Override
            public String greet(String name) {
                return "你好, " + name + "!";
            }
        };
        System.out.println("匿名内部类: " + anonymousGreeting.greet("World"));

        // 4. Lambda 替代匿名内部类（函数式接口）
        Greeting lambdaGreeting = name -> "Hi, " + name + "!";
        System.out.println("Lambda: " + lambdaGreeting.greet("Java"));

        // 5. 局部内部类（方法内定义）
        class LocalInner {
            void show() { System.out.println("局部内部类（方法内定义）"); }
        }
        new LocalInner().show();
    }
}

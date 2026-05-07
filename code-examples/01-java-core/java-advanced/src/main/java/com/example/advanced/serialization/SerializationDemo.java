package com.example.advanced.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;

/**
 * 序列化机制演示
 * <p>
 * 演示内容：
 * 1. Java 原生序列化（Serializable）
 * 2. JSON 序列化（Jackson）
 * 3. 序列化性能对比
 * </p>
 */
public class SerializationDemo {

    // ==================== 数据类定义 ====================

    /**
     * 可序列化的用户类
     */
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private int age;
        private transient String password; // transient 字段不参与序列化

        public User() {} // Jackson 需要无参构造器

        public User(String name, int age, String password) {
            this.name = name;
            this.age = age;
            this.password = password;
        }

        // Getter/Setter（Jackson 需要）
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + ", password='" + password + "'}";
        }
    }

    // ==================== main ====================

    public static void main(String[] args) throws Exception {
        System.out.println("========== 序列化机制演示 ==========\n");

        demonstrateJavaSerialization();
        System.out.println();

        demonstrateJacksonSerialization();
        System.out.println();

        demonstratePerformanceComparison();
    }

    // ==================== 1. Java 原生序列化 ====================

    private static void demonstrateJavaSerialization() throws Exception {
        System.out.println("--- 1. Java 原生序列化 ---");

        User user = new User("张三", 25, "secret123");
        System.out.println("原始对象: " + user);

        // 序列化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(user);
        }
        byte[] bytes = bos.toByteArray();
        System.out.println("序列化后大小: " + bytes.length + " bytes");

        // 反序列化
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            User deserialized = (User) ois.readObject();
            System.out.println("反序列化: " + deserialized);
            System.out.println("  注意: password 为 null（transient 字段未序列化）");
        }

        // serialVersionUID 说明
        System.out.println("\nserialVersionUID 说明：");
        System.out.println("  当前 UID = " + User.serialVersionUID);
        System.out.println("  作用: 版本控制，UID 不匹配时反序列化抛出 InvalidClassException");
        System.out.println("  最佳实践: 显式声明 serialVersionUID，避免类结构变化导致反序列化失败");
    }

    // ==================== 2. Jackson JSON 序列化 ====================

    private static void demonstrateJacksonSerialization() throws Exception {
        System.out.println("--- 2. Jackson JSON 序列化 ---");

        ObjectMapper mapper = new ObjectMapper();
        User user = new User("李四", 30, "password456");
        System.out.println("原始对象: " + user);

        // 序列化为 JSON
        String json = mapper.writeValueAsString(user);
        System.out.println("JSON: " + json);
        System.out.println("JSON 大小: " + json.getBytes().length + " bytes");

        // 从 JSON 反序列化
        User deserialized = mapper.readValue(json, User.class);
        System.out.println("反序列化: " + deserialized);
        System.out.println("  注意: password 被序列化了（transient 对 Jackson 无效，需用 @JsonIgnore）");

        // 格式化输出
        String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);
        System.out.println("\n格式化 JSON:\n" + prettyJson);
    }

    // ==================== 3. 性能对比 ====================

    private static void demonstratePerformanceComparison() throws Exception {
        System.out.println("--- 3. 序列化性能对比 ---");

        User user = new User("王五", 28, "pwd789");
        ObjectMapper mapper = new ObjectMapper();
        int iterations = 10000;

        // 预热
        for (int i = 0; i < 1000; i++) {
            javaSerialize(user);
            mapper.writeValueAsBytes(user);
        }

        // Java 原生序列化
        long start = System.nanoTime();
        int javaSize = 0;
        for (int i = 0; i < iterations; i++) {
            byte[] bytes = javaSerialize(user);
            javaSize = bytes.length;
        }
        long javaTime = (System.nanoTime() - start) / 1_000_000;

        // Jackson 序列化
        start = System.nanoTime();
        int jacksonSize = 0;
        for (int i = 0; i < iterations; i++) {
            byte[] bytes = mapper.writeValueAsBytes(user);
            jacksonSize = bytes.length;
        }
        long jacksonTime = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  " + iterations + " 次序列化对比：");
        System.out.println(String.format("  %-20s %-15s %-15s", "方案", "耗时", "序列化大小"));
        System.out.println(String.format("  %-20s %-15s %-15s",
                "Java 原生", javaTime + "ms", javaSize + " bytes"));
        System.out.println(String.format("  %-20s %-15s %-15s",
                "Jackson JSON", jacksonTime + "ms", jacksonSize + " bytes"));

        System.out.println("\n  结论：");
        System.out.println("  - Jackson JSON 通常比 Java 原生序列化更快");
        System.out.println("  - JSON 可读性好，跨语言，是 Web API 的首选");
        System.out.println("  - Protobuf 体积最小、速度最快，适合 RPC 通信");
        System.out.println("  - Java 原生序列化有安全风险，不推荐使用");
    }

    /**
     * Java 原生序列化辅助方法
     */
    private static byte[] javaSerialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        return bos.toByteArray();
    }
}

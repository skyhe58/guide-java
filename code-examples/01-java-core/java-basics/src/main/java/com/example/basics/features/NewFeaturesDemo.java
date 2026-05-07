package com.example.basics.features;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * JDK 8/17/21 新特性代码演示
 *
 * 核心知识点：
 * 1. JDK 8: Lambda/Stream/Optional/Date-Time API/接口默认方法
 * 2. JDK 17: Sealed Classes/Pattern Matching/Records/Text Blocks/Switch 表达式
 * 3. JDK 21: Virtual Threads/Record Patterns/Sequenced Collections
 *
 * 对应文档：docs/java-basics/new-features.md
 */
public class NewFeaturesDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===== JDK 8 特性 =====");
        demonstrateJdk8Features();

        System.out.println("\n===== JDK 17 特性 =====");
        demonstrateJdk17Features();

        System.out.println("\n===== JDK 21 特性 =====");
        demonstrateJdk21Features();
    }

    // ==================== JDK 8 特性 ====================

    static void demonstrateJdk8Features() {
        // 1. Optional：优雅处理 null
        System.out.println("--- Optional ---");
        Optional<String> opt = Optional.ofNullable(null);
        String result = opt.orElse("默认值");
        System.out.println("Optional.orElse: " + result);

        Optional.of("Hello")
                .map(String::toUpperCase)
                .filter(s -> s.length() > 3)
                .ifPresent(s -> System.out.println("Optional 链式: " + s));

        // 2. Date-Time API（替代 Date/Calendar）
        System.out.println("\n--- Date-Time API ---");
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        System.out.println("今天: " + today);
        System.out.println("现在: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("30天后: " + today.plusDays(30));

        // 3. 接口默认方法
        System.out.println("\n--- 接口默认方法 ---");
        Greeter greeter = name -> "Hello, " + name;
        System.out.println(greeter.greet("Java 8"));
        System.out.println(greeter.greetLoudly("Java 8")); // 调用默认方法
    }

    /** 带默认方法的函数式接口 */
    @FunctionalInterface
    interface Greeter {
        String greet(String name);

        default String greetLoudly(String name) {
            return greet(name).toUpperCase() + "!!!";
        }
    }

    // ==================== JDK 17 特性 ====================

    static void demonstrateJdk17Features() {
        // 1. Records（JDK 16+）：不可变数据载体
        System.out.println("--- Records ---");
        record Point(int x, int y) {
            // 自动生成 constructor、getter、equals、hashCode、toString
            double distanceTo(Point other) {
                return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
            }
        }

        Point p1 = new Point(0, 0);
        Point p2 = new Point(3, 4);
        System.out.println("Point: " + p1);
        System.out.println("距离: " + p1.distanceTo(p2));
        System.out.println("equals: " + new Point(1, 2).equals(new Point(1, 2))); // true

        // 2. Text Blocks（JDK 15+）：多行字符串
        System.out.println("\n--- Text Blocks ---");
        String json = """
                {
                    "name": "Java",
                    "version": 21,
                    "features": ["Virtual Threads", "Records"]
                }
                """;
        System.out.println("Text Block:\n" + json);

        // 3. Switch 表达式（JDK 14+）
        System.out.println("--- Switch 表达式 ---");
        String day = "MONDAY";
        String type = switch (day) {
            case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "工作日";
            case "SATURDAY", "SUNDAY" -> "周末";
            default -> "未知";
        };
        System.out.println(day + " 是 " + type);

        // 4. Pattern Matching for instanceof（JDK 16+）
        System.out.println("\n--- Pattern Matching ---");
        Object obj = "Hello Pattern Matching";
        if (obj instanceof String s && s.length() > 5) {
            System.out.println("匹配到字符串: " + s.toUpperCase());
        }

        // 5. Sealed Classes（JDK 17）
        System.out.println("\n--- Sealed Classes ---");
        Shape circle = new Circle(5);
        Shape rect = new Rectangle(3, 4);
        System.out.println("圆形面积: " + calculateArea(circle));
        System.out.println("矩形面积: " + calculateArea(rect));
    }

    /** Sealed Class：限制子类范围 */
    sealed interface Shape permits Circle, Rectangle, Triangle {}

    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}

    /** Pattern Matching for Switch（JDK 21 正式） */
    static double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle t -> 0.5 * t.base() * t.height();
        };
    }

    // ==================== JDK 21 特性 ====================

    static void demonstrateJdk21Features() throws Exception {
        // 1. Virtual Threads（虚拟线程）
        System.out.println("--- Virtual Threads ---");
        demonstrateVirtualThreads();

        // 2. Record Patterns
        System.out.println("\n--- Record Patterns ---");
        demonstrateRecordPatterns();

        // 3. Sequenced Collections
        System.out.println("\n--- Sequenced Collections ---");
        demonstrateSequencedCollections();
    }

    /**
     * Virtual Threads（虚拟线程）：JDK 21 正式特性
     * 轻量级线程，由 JVM 调度，不绑定操作系统线程
     * 适合 IO 密集型任务，可以创建百万级虚拟线程
     */
    static void demonstrateVirtualThreads() throws Exception {
        // 创建单个虚拟线程
        Thread vThread = Thread.ofVirtual().name("my-virtual-thread").start(() -> {
            System.out.println("  虚拟线程: " + Thread.currentThread());
            System.out.println("  是否虚拟线程: " + Thread.currentThread().isVirtual());
        });
        vThread.join();

        // 使用 ExecutorService 创建虚拟线程池
        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 创建 10000 个虚拟线程
            List<Future<String>> futures = IntStream.range(0, 10_000)
                    .mapToObj(i -> executor.submit(() -> {
                        Thread.sleep(10); // 模拟 IO 操作
                        return "Task-" + i;
                    }))
                    .toList();

            // 等待所有任务完成
            int completed = 0;
            for (Future<String> f : futures) {
                f.get();
                completed++;
            }
            System.out.println("  完成 " + completed + " 个虚拟线程任务");
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  10000 个虚拟线程耗时: " + elapsed + " ms");

        // 对比：平台线程做同样的事情会慢很多（线程创建开销大）
        System.out.println("  虚拟线程优势: 轻量级，适合 IO 密集型，可创建百万级");
    }

    /**
     * Record Patterns（JDK 21）：解构 Record
     */
    static void demonstrateRecordPatterns() {
        record Coordinate(int x, int y) {}
        record Line(Coordinate start, Coordinate end) {}

        Object obj = new Line(new Coordinate(0, 0), new Coordinate(3, 4));

        // Record Pattern：直接解构嵌套 Record
        if (obj instanceof Line(Coordinate(int x1, int y1), Coordinate(int x2, int y2))) {
            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            System.out.println("  线段: (" + x1 + "," + y1 + ") → (" + x2 + "," + y2 + ")");
            System.out.println("  长度: " + length);
        }
    }

    /**
     * Sequenced Collections（JDK 21）：有序集合的统一接口
     * 新增 getFirst()、getLast()、reversed() 等方法
     */
    static void demonstrateSequencedCollections() {
        // SequencedCollection：有序集合
        SequencedCollection<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
        System.out.println("  第一个: " + list.getFirst());
        System.out.println("  最后一个: " + list.getLast());
        System.out.println("  反转: " + list.reversed());

        // addFirst / addLast
        list.addFirst("Z");
        list.addLast("E");
        System.out.println("  添加后: " + list);

        // SequencedMap
        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        System.out.println("  Map 第一个: " + map.firstEntry());
        System.out.println("  Map 最后一个: " + map.lastEntry());
        System.out.println("  Map 反转: " + map.reversed());
    }
}

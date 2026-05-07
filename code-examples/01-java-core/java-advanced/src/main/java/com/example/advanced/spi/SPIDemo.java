package com.example.advanced.spi;

/**
 * Java SPI 机制演示 — 完整的 ServiceLoader 加载流程
 *
 * <p>本示例用纯 Java 演示 SPI（Service Provider Interface）机制：
 * <ul>
 *   <li>SPI 核心流程：定义接口 → 实现类 → META-INF/services 配置 → ServiceLoader 加载</li>
 *   <li>手写 ServiceLoader 理解加载原理</li>
 *   <li>JDBC Driver 的 SPI 加载过程</li>
 *   <li>SPI 的优缺点与改进（Spring SPI、Dubbo SPI）</li>
 * </ul>
 *
 * <h3>SPI 工作流程：</h3>
 * <pre>
 *  1. 定义接口：public interface Logger { void log(String msg); }
 *  2. 实现类：  public class ConsoleLogger implements Logger { ... }
 *  3. 配置文件：META-INF/services/com.example.Logger
 *              内容：com.example.ConsoleLogger
 *  4. 加载：   ServiceLoader.load(Logger.class).forEach(logger -> logger.log("hello"));
 *
 *  实际应用：
 *  - JDBC Driver 自动发现（java.sql.Driver）
 *  - SLF4J 日志框架绑定
 *  - Dubbo 扩展点加载
 *  - Spring Boot 自动配置（spring.factories，类似 SPI）
 * </pre>
 */
public class SPIDemo {

    // ==================== SPI 接口定义 ====================

    /** 日志接口（SPI 接口） */
    interface Logger {
        void log(String message);
        String name();
    }

    /** 序列化接口（SPI 接口） */
    interface Serializer {
        byte[] serialize(Object obj);
        Object deserialize(byte[] data);
        String name();
    }

    // ==================== SPI 实现类 ====================

    /** 控制台日志实现 */
    static class ConsoleLogger implements Logger {
        @Override
        public void log(String message) {
            System.out.printf("      [Console] %s%n", message);
        }
        @Override
        public String name() { return "ConsoleLogger"; }
    }

    /** 文件日志实现 */
    static class FileLogger implements Logger {
        @Override
        public void log(String message) {
            System.out.printf("      [File] %s → 写入 app.log%n", message);
        }
        @Override
        public String name() { return "FileLogger"; }
    }

    /** JSON 序列化实现 */
    static class JsonSerializer implements Serializer {
        @Override
        public byte[] serialize(Object obj) {
            String json = "{\"data\":\"" + obj.toString() + "\"}";
            return json.getBytes();
        }
        @Override
        public Object deserialize(byte[] data) {
            return new String(data);
        }
        @Override
        public String name() { return "JsonSerializer"; }
    }

    // ==================== 手写 ServiceLoader ====================

    /**
     * 手写 ServiceLoader，理解 SPI 加载原理。
     * 实际的 ServiceLoader 从 META-INF/services/ 目录读取配置文件，
     * 这里用 Map 模拟配置文件。
     */
    static class SimpleServiceLoader<T> {
        private final Class<T> serviceInterface;
        // 模拟 META-INF/services/ 配置文件：接口全限定名 → 实现类列表
        private static final java.util.Map<String, java.util.List<java.util.function.Supplier<?>>> registry =
                new java.util.LinkedHashMap<>();

        static {
            // 模拟 META-INF/services/com.example.advanced.spi.SPIDemo$Logger
            registry.put("Logger", java.util.Arrays.asList(
                    ConsoleLogger::new, FileLogger::new));
            // 模拟 META-INF/services/com.example.advanced.spi.SPIDemo$Serializer
            registry.put("Serializer", java.util.Arrays.asList(
                    JsonSerializer::new));
        }

        SimpleServiceLoader(Class<T> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        /** 加载所有实现（对应 ServiceLoader.load()） */
        @SuppressWarnings("unchecked")
        java.util.List<T> loadAll() {
            java.util.List<T> instances = new java.util.ArrayList<>();
            String key = serviceInterface.getSimpleName();
            java.util.List<java.util.function.Supplier<?>> suppliers = registry.get(key);
            if (suppliers != null) {
                for (java.util.function.Supplier<?> supplier : suppliers) {
                    instances.add((T) supplier.get());
                }
            }
            return instances;
        }

        /** 加载第一个实现 */
        T loadFirst() {
            java.util.List<T> all = loadAll();
            return all.isEmpty() ? null : all.get(0);
        }

        static <T> SimpleServiceLoader<T> load(Class<T> serviceInterface) {
            return new SimpleServiceLoader<>(serviceInterface);
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：SPI 基本使用 */
    static void demoBasicSPI() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：SPI 基本使用 — ServiceLoader 加载");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【加载 Logger 的所有实现】");
        java.util.List<Logger> loggers = SimpleServiceLoader.load(Logger.class).loadAll();
        System.out.printf("    发现 %d 个 Logger 实现：%n", loggers.size());
        for (Logger logger : loggers) {
            System.out.printf("    → %s%n", logger.name());
            logger.log("Hello SPI!");
        }

        System.out.println("\n  【加载 Serializer 的第一个实现】");
        Serializer serializer = SimpleServiceLoader.load(Serializer.class).loadFirst();
        if (serializer != null) {
            System.out.printf("    使用: %s%n", serializer.name());
            byte[] data = serializer.serialize("测试数据");
            System.out.printf("    序列化: %s%n", new String(data));
            System.out.printf("    反序列化: %s%n", serializer.deserialize(data));
        }
        System.out.println();
    }

    /** 演示2：SPI 加载原理 */
    static void demoSPIPrinciple() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：SPI 加载原理（ServiceLoader 源码解读）");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  ServiceLoader.load(Logger.class) 内部流程：");
        System.out.println("    1. 获取线程上下文类加载器（Thread.currentThread().getContextClassLoader()）");
        System.out.println("    2. 拼接配置文件路径：META-INF/services/ + 接口全限定名");
        System.out.println("    3. 用类加载器读取配置文件（可能有多个 JAR 中都有）");
        System.out.println("    4. 逐行读取实现类的全限定名");
        System.out.println("    5. 通过 Class.forName() 加载实现类");
        System.out.println("    6. 通过 newInstance() 创建实例");
        System.out.println("    7. 返回迭代器（懒加载，用到时才实例化）");

        // 演示实际的 ServiceLoader（JDBC Driver）
        System.out.println("\n  【实际案例：JDBC Driver SPI】");
        System.out.println("    配置文件：mysql-connector-j.jar!/META-INF/services/java.sql.Driver");
        System.out.println("    内容：com.mysql.cj.jdbc.Driver");
        System.out.println("    DriverManager 启动时自动调用 ServiceLoader.load(Driver.class)");
        System.out.println("    所以 JDBC 4.0+ 不需要手动 Class.forName(\"com.mysql.cj.jdbc.Driver\")");
        System.out.println();
    }

    /** 演示3：SPI 的优缺点与改进 */
    static void demoSPIComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：SPI 优缺点与改进方案");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"特性",       "Java SPI",              "Spring SPI",           "Dubbo SPI"},
                {"配置文件",   "META-INF/services/",    "META-INF/spring.factories", "META-INF/dubbo/"},
                {"加载方式",   "全部加载",               "全部加载",              "按需加载（key=value）"},
                {"依赖注入",   "❌ 不支持",              "❌ 不支持",             "✅ 支持（@Adaptive）"},
                {"AOP",       "❌ 不支持",              "❌ 不支持",             "✅ 支持（Wrapper）"},
                {"缓存",      "❌ 每次重新加载",         "✅ 缓存",              "✅ 缓存"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-10s %-25s %-25s %-25s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2], comparison[i][3]);
            if (i == 0) System.out.println("  " + "─".repeat(85));
        }

        System.out.println("\n  Java SPI 的缺点：");
        System.out.println("    1. 全部加载：不能按需加载某个实现");
        System.out.println("    2. 无法传参：实现类必须有无参构造器");
        System.out.println("    3. 线程不安全：ServiceLoader 不是线程安全的");
        System.out.println("    4. 无异常处理：某个实现加载失败会影响其他实现");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Java SPI 机制演示 — ServiceLoader 加载原理           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoBasicSPI();
        demoSPIPrinciple();
        demoSPIComparison();
    }
}

package com.example.middleware.config.apollo;

/**
 * Apollo 配置中心演示 — 模拟长轮询热更新 + 多环境 + 命名空间
 *
 * <p>本示例用纯 Java 模拟 Apollo 配置中心的核心机制：
 * <ul>
 *   <li>Namespace（命名空间）：application / 公共 / 私有</li>
 *   <li>多环境管理：DEV / FAT / UAT / PRO</li>
 *   <li>灰度发布：按 IP / 标签灰度推送</li>
 *   <li>长轮询热更新：配置变更实时推送到客户端</li>
 *   <li>配置发布审核流程</li>
 * </ul>
 *
 * <p>如需连接真实 Apollo，使用：
 * {@code docker compose -f docker/docker-compose.apollo.yml up -d}
 * <p>Portal 地址：http://localhost:8070（apollo/admin）
 *
 * <h3>Apollo 架构：</h3>
 * <pre>
 *  Portal（管理界面）→ Admin Service → Config DB
 *                                        ↓
 *  Client ←── 长轮询 ←── Config Service ←─┘
 *
 *  核心概念：
 *  - AppId：应用标识
 *  - Cluster：集群（如 default / shanghai / beijing）
 *  - Namespace：命名空间（如 application / datasource / redis）
 *  - Environment：环境（DEV / FAT / UAT / PRO）
 * </pre>
 */
public class ApolloDemo {

    /** 配置项 */
    static class ConfigEntry {
        final String key;
        volatile String value;
        volatile boolean published; // 是否已发布（Apollo 有发布审核流程）
        final String modifiedBy;
        final long modifiedTime;

        ConfigEntry(String key, String value, String modifiedBy) {
            this.key = key;
            this.value = value;
            this.published = false;
            this.modifiedBy = modifiedBy;
            this.modifiedTime = System.currentTimeMillis();
        }
    }

    /** 命名空间 */
    static class Namespace {
        final String name;
        final boolean isPublic; // 公共命名空间可被其他应用继承
        private final java.util.Map<String, ConfigEntry> configs = new java.util.LinkedHashMap<>();
        private final java.util.Map<String, ConfigEntry> publishedConfigs = new java.util.LinkedHashMap<>();
        private final java.util.List<java.util.function.Consumer<String>> listeners = new java.util.ArrayList<>();

        Namespace(String name, boolean isPublic) {
            this.name = name;
            this.isPublic = isPublic;
        }

        /** 修改配置（未发布状态） */
        void modify(String key, String value, String operator) {
            configs.put(key, new ConfigEntry(key, value, operator));
        }

        /** 发布配置（审核通过后） */
        void publish() {
            for (var entry : configs.entrySet()) {
                entry.getValue().published = true;
                publishedConfigs.put(entry.getKey(), entry.getValue());
            }
            // 通知监听者
            for (var listener : listeners) {
                listener.accept(name);
            }
        }

        /** 读取已发布的配置 */
        String getPublished(String key) {
            ConfigEntry entry = publishedConfigs.get(key);
            return entry != null ? entry.value : null;
        }

        /** 注册变更监听 */
        void addChangeListener(java.util.function.Consumer<String> listener) {
            listeners.add(listener);
        }

        java.util.Map<String, String> getAllPublished() {
            java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
            publishedConfigs.forEach((k, v) -> result.put(k, v.value));
            return result;
        }
    }

    /** 模拟 Apollo Config Service */
    static class SimulatedApollo {
        // appId → env → namespace → Namespace
        private final java.util.Map<String, java.util.Map<String, java.util.Map<String, Namespace>>> store =
                new java.util.LinkedHashMap<>();

        Namespace getOrCreateNamespace(String appId, String env, String nsName, boolean isPublic) {
            return store.computeIfAbsent(appId, k -> new java.util.LinkedHashMap<>())
                    .computeIfAbsent(env, k -> new java.util.LinkedHashMap<>())
                    .computeIfAbsent(nsName, k -> new Namespace(nsName, isPublic));
        }

        Namespace getNamespace(String appId, String env, String nsName) {
            var envMap = store.getOrDefault(appId, java.util.Collections.emptyMap());
            var nsMap = envMap.getOrDefault(env, java.util.Collections.emptyMap());
            return nsMap.get(nsName);
        }
    }

    // ==================== 演示方法 ====================

    static void demoNamespaceAndEnvironment() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：命名空间 + 多环境管理");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedApollo apollo = new SimulatedApollo();

        // DEV 环境配置
        Namespace devApp = apollo.getOrCreateNamespace("user-service", "DEV", "application", false);
        devApp.modify("server.port", "8080", "developer");
        devApp.modify("spring.datasource.url", "jdbc:mysql://localhost:3306/dev_db", "developer");
        devApp.modify("logging.level.root", "DEBUG", "developer");
        devApp.publish();

        // PRO 环境配置
        Namespace proApp = apollo.getOrCreateNamespace("user-service", "PRO", "application", false);
        proApp.modify("server.port", "80", "ops");
        proApp.modify("spring.datasource.url", "jdbc:mysql://rds.prod.com:3306/prod_db", "ops");
        proApp.modify("logging.level.root", "WARN", "ops");
        proApp.publish();

        // 公共命名空间（多应用共享）
        Namespace publicDs = apollo.getOrCreateNamespace("common", "DEV", "datasource", true);
        publicDs.modify("db.pool.maxSize", "20", "dba");
        publicDs.modify("db.pool.minIdle", "5", "dba");
        publicDs.publish();

        System.out.println("\n  DEV 环境 application 配置：");
        devApp.getAllPublished().forEach((k, v) -> System.out.printf("    %s = %s%n", k, v));

        System.out.println("\n  PRO 环境 application 配置：");
        proApp.getAllPublished().forEach((k, v) -> System.out.printf("    %s = %s%n", k, v));

        System.out.println("\n  公共命名空间 datasource（所有应用共享）：");
        publicDs.getAllPublished().forEach((k, v) -> System.out.printf("    %s = %s%n", k, v));
        System.out.println();
    }

    static void demoHotUpdate() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：长轮询热更新");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedApollo apollo = new SimulatedApollo();
        Namespace ns = apollo.getOrCreateNamespace("user-service", "DEV", "application", false);
        ns.modify("cache.ttl", "3600", "developer");
        ns.publish();

        // 注册变更监听（模拟 Apollo Client 的 ConfigChangeListener）
        System.out.println("\n  注册配置变更监听器：");
        ns.addChangeListener(nsName -> {
            System.out.printf("    [ConfigChangeListener] 命名空间 %s 配置变更！%n", nsName);
            System.out.printf("    最新配置: %s%n", ns.getAllPublished());
            System.out.println("    → 触发 @ApolloConfigChangeListener 回调");
        });

        // 修改并发布
        System.out.println("\n  在 Portal 中修改 cache.ttl: 3600 → 7200，然后发布：");
        ns.modify("cache.ttl", "7200", "developer");
        ns.publish();

        System.out.println("\n  Apollo 热更新流程：");
        System.out.println("    1. 运维在 Portal 修改配置");
        System.out.println("    2. 点击发布（可配置审核流程）");
        System.out.println("    3. Config Service 通知长轮询的客户端");
        System.out.println("    4. 客户端拉取最新配置");
        System.out.println("    5. 触发 ConfigChangeListener 回调");
        System.out.println("    6. @ApolloConfigChangeListener 注解的方法被调用");
        System.out.println();
    }

    static void demoGrayRelease() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：灰度发布");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  Apollo 灰度发布流程：");
        System.out.println("    1. 创建灰度规则（按 IP 或 Label）");
        System.out.println("    2. 在灰度分支修改配置");
        System.out.println("    3. 灰度发布 → 只有匹配规则的实例收到新配置");
        System.out.println("    4. 验证无问题后 → 全量发布");
        System.out.println("    5. 放弃灰度 → 回滚到主分支配置");

        // 模拟灰度
        java.util.Map<String, String> mainConfig = new java.util.LinkedHashMap<>();
        mainConfig.put("feature.newUI", "false");

        java.util.Map<String, String> grayConfig = new java.util.LinkedHashMap<>(mainConfig);
        grayConfig.put("feature.newUI", "true");

        java.util.Set<String> grayIps = new java.util.HashSet<>(java.util.Arrays.asList("10.0.0.1", "10.0.0.2"));

        System.out.println("\n  灰度规则：IP in [10.0.0.1, 10.0.0.2]");
        String[] clientIps = {"10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.4"};
        for (String ip : clientIps) {
            boolean isGray = grayIps.contains(ip);
            String config = isGray ? grayConfig.get("feature.newUI") : mainConfig.get("feature.newUI");
            System.out.printf("    %s → feature.newUI=%s %s%n", ip, config, isGray ? "(灰度)" : "(主分支)");
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Apollo 配置中心演示（纯内存模拟）                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoNamespaceAndEnvironment();
        demoHotUpdate();
        demoGrayRelease();
    }
}

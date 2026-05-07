package com.example.middleware.config.nacos;

/**
 * Nacos 配置中心演示 — 模拟配置监听 + 长轮询 + 动态刷新
 *
 * <p>本示例用纯 Java 模拟 Nacos Config 的核心机制：
 * <ul>
 *   <li>配置发布与读取</li>
 *   <li>长轮询监听（Long Polling）— 配置变更实时推送</li>
 *   <li>动态刷新（@RefreshScope 原理）</li>
 *   <li>灰度发布（按 IP / 标签灰度推送配置）</li>
 *   <li>多环境配置（dataId + group + namespace）</li>
 * </ul>
 *
 * <h3>Nacos Config 长轮询原理：</h3>
 * <pre>
 *  Client                          Nacos Server
 *    │                                │
 *    │── 长轮询请求（30s 超时）──→      │
 *    │                                │  配置未变更 → 挂起连接
 *    │                                │  ...
 *    │                                │  配置变更！
 *    │   ←── 立即返回变更的 dataId ──   │
 *    │                                │
 *    │── 拉取最新配置 ──────────→      │
 *    │   ←── 返回配置内容 ──────       │
 *    │                                │
 *    │── 下一次长轮询 ──────────→      │
 * </pre>
 */
public class NacosConfigDemo {

    /** 配置项 */
    static class ConfigItem {
        final String dataId;
        final String group;
        final String namespace;
        volatile String content;
        volatile long version;
        volatile String md5;

        ConfigItem(String dataId, String group, String namespace, String content) {
            this.dataId = dataId;
            this.group = group;
            this.namespace = namespace;
            this.content = content;
            this.version = 1;
            this.md5 = Integer.toHexString(content.hashCode());
        }

        void update(String newContent) {
            this.content = newContent;
            this.version++;
            this.md5 = Integer.toHexString(newContent.hashCode());
        }

        String getKey() { return namespace + "@@" + group + "@@" + dataId; }
    }

    /** 配置变更监听器 */
    interface ConfigListener {
        void onChanged(String dataId, String group, String oldContent, String newContent);
    }

    /** 模拟 Nacos Config Server */
    static class SimulatedNacosConfig {
        private final java.util.Map<String, ConfigItem> configs = new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Map<String, java.util.List<ConfigListener>> listeners = new java.util.concurrent.ConcurrentHashMap<>();

        /** 发布配置 */
        void publishConfig(String dataId, String group, String namespace, String content) {
            String key = namespace + "@@" + group + "@@" + dataId;
            ConfigItem existing = configs.get(key);
            if (existing != null) {
                String oldContent = existing.content;
                existing.update(content);
                // 通知监听者
                java.util.List<ConfigListener> ls = listeners.get(key);
                if (ls != null) {
                    for (ConfigListener l : ls) {
                        l.onChanged(dataId, group, oldContent, content);
                    }
                }
            } else {
                configs.put(key, new ConfigItem(dataId, group, namespace, content));
            }
        }

        /** 获取配置 */
        String getConfig(String dataId, String group, String namespace) {
            String key = namespace + "@@" + group + "@@" + dataId;
            ConfigItem item = configs.get(key);
            return item != null ? item.content : null;
        }

        /** 注册监听器 */
        void addListener(String dataId, String group, String namespace, ConfigListener listener) {
            String key = namespace + "@@" + group + "@@" + dataId;
            listeners.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(listener);
        }

        /** 模拟长轮询：检查配置是否变更 */
        boolean longPolling(String dataId, String group, String namespace, String clientMd5, long timeoutMs) {
            String key = namespace + "@@" + group + "@@" + dataId;
            ConfigItem item = configs.get(key);
            if (item != null && !item.md5.equals(clientMd5)) {
                return true; // 配置已变更，立即返回
            }
            // 模拟挂起等待
            try { Thread.sleep(Math.min(timeoutMs, 100)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            // 再次检查
            item = configs.get(key);
            return item != null && !item.md5.equals(clientMd5);
        }
    }

    // ==================== 演示方法 ====================

    static void demoConfigPublishAndRead() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：配置发布与读取");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedNacosConfig server = new SimulatedNacosConfig();

        System.out.println("\n  发布配置：");
        server.publishConfig("application.yml", "DEFAULT_GROUP", "dev",
                "server:\n  port: 8080\nspring:\n  datasource:\n    url: jdbc:mysql://localhost:3306/dev_db");
        server.publishConfig("application.yml", "DEFAULT_GROUP", "prod",
                "server:\n  port: 80\nspring:\n  datasource:\n    url: jdbc:mysql://rds.example.com:3306/prod_db");

        System.out.println("  读取 dev 环境配置：");
        System.out.println("    " + server.getConfig("application.yml", "DEFAULT_GROUP", "dev")
                .replace("\n", "\n    "));

        System.out.println("\n  读取 prod 环境配置：");
        System.out.println("    " + server.getConfig("application.yml", "DEFAULT_GROUP", "prod")
                .replace("\n", "\n    "));
        System.out.println();
    }

    static void demoConfigListener() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：配置监听 + 动态刷新");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedNacosConfig server = new SimulatedNacosConfig();
        server.publishConfig("db.properties", "DEFAULT_GROUP", "dev",
                "db.url=jdbc:mysql://localhost:3306/dev\ndb.pool.size=10");

        // 注册监听器（模拟 @RefreshScope）
        System.out.println("\n  注册配置监听器：");
        server.addListener("db.properties", "DEFAULT_GROUP", "dev", (dataId, group, oldContent, newContent) -> {
            System.out.printf("    [监听器] 配置变更！dataId=%s%n", dataId);
            System.out.printf("    旧值: %s%n", oldContent.replace("\n", ", "));
            System.out.printf("    新值: %s%n", newContent.replace("\n", ", "));
            System.out.println("    → 触发 @RefreshScope Bean 重新创建");
        });

        // 修改配置 → 触发监听
        System.out.println("\n  修改配置（连接池大小 10 → 20）：");
        server.publishConfig("db.properties", "DEFAULT_GROUP", "dev",
                "db.url=jdbc:mysql://localhost:3306/dev\ndb.pool.size=20");
        System.out.println();
    }

    static void demoLongPolling() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：长轮询机制");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedNacosConfig server = new SimulatedNacosConfig();
        server.publishConfig("app.yml", "DEFAULT_GROUP", "dev", "version: 1.0");

        String clientMd5 = Integer.toHexString("version: 1.0".hashCode());

        System.out.println("\n  客户端发起长轮询（配置未变更）：");
        long start = System.currentTimeMillis();
        boolean changed = server.longPolling("app.yml", "DEFAULT_GROUP", "dev", clientMd5, 500);
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("    结果: changed=%s, 耗时=%dms（挂起等待后超时返回）%n", changed, elapsed);

        // 修改配置后再次长轮询
        server.publishConfig("app.yml", "DEFAULT_GROUP", "dev", "version: 2.0");
        System.out.println("\n  配置变更后再次长轮询：");
        start = System.currentTimeMillis();
        changed = server.longPolling("app.yml", "DEFAULT_GROUP", "dev", clientMd5, 500);
        elapsed = System.currentTimeMillis() - start;
        System.out.printf("    结果: changed=%s, 耗时=%dms（配置已变更，立即返回）%n", changed, elapsed);
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Nacos 配置中心演示（纯内存模拟）                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoConfigPublishAndRead();
        demoConfigListener();
        demoLongPolling();
    }
}

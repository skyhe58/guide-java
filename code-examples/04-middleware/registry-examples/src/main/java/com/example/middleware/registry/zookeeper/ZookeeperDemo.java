package com.example.middleware.registry.zookeeper;

/**
 * ZooKeeper 演示（混合模式）
 *
 * <p>Part A：用 TreeMap 模拟 ZNode 树 + 临时节点 + Watcher（直接运行）
 * <ul>
 *   <li>ZNode 树形结构（持久节点 / 临时节点 / 顺序节点）</li>
 *   <li>Watcher 监听机制</li>
 *   <li>分布式锁（临时顺序节点实现）</li>
 *   <li>服务注册发现（临时节点实现）</li>
 * </ul>
 *
 * <p>Part B：用 Curator Framework 连接真实 ZooKeeper
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d zookeeper}
 *
 * <h3>ZooKeeper 数据模型：</h3>
 * <pre>
 *  /                           ← 根节点
 *  ├── /services               ← 持久节点（服务注册根路径）
 *  │   ├── /services/user-service
 *  │   │   ├── /services/user-service/instance-0001  ← 临时顺序节点
 *  │   │   └── /services/user-service/instance-0002  ← 临时顺序节点
 *  │   └── /services/order-service
 *  ├── /locks                  ← 分布式锁根路径
 *  │   └── /locks/order-lock
 *  │       ├── /locks/order-lock/lock-0001  ← 临时顺序节点
 *  │       └── /locks/order-lock/lock-0002
 *  └── /config                 ← 配置根路径
 *
 *  节点类型：
 *  PERSISTENT          — 持久节点（客户端断开后不删除）
 *  EPHEMERAL           — 临时节点（客户端断开后自动删除）
 *  PERSISTENT_SEQUENTIAL — 持久顺序节点（自动追加递增序号）
 *  EPHEMERAL_SEQUENTIAL  — 临时顺序节点（分布式锁的基础）
 * </pre>
 */
public class ZookeeperDemo {

    // ==================== Part A：模拟 ZooKeeper ====================

    enum NodeType { PERSISTENT, EPHEMERAL, PERSISTENT_SEQUENTIAL, EPHEMERAL_SEQUENTIAL }

    /** 模拟 ZNode */
    static class ZNode {
        final String path;
        final NodeType type;
        String data;
        final String owner; // 创建者（临时节点的 session）
        final java.util.Map<String, ZNode> children = new java.util.LinkedHashMap<>();
        final java.util.List<WatcherCallback> watchers = new java.util.ArrayList<>();

        ZNode(String path, NodeType type, String data, String owner) {
            this.path = path;
            this.type = type;
            this.data = data;
            this.owner = owner;
        }
    }

    /** Watcher 回调 */
    interface WatcherCallback {
        void onEvent(String eventType, String path);
    }

    /** 模拟 ZooKeeper 服务 */
    static class SimulatedZK {
        private final ZNode root = new ZNode("/", NodeType.PERSISTENT, "", "system");
        private int sequenceCounter = 0;

        /** 创建节点 */
        String create(String path, String data, NodeType type, String session) {
            String actualPath = path;
            if (type == NodeType.PERSISTENT_SEQUENTIAL || type == NodeType.EPHEMERAL_SEQUENTIAL) {
                actualPath = path + String.format("%010d", ++sequenceCounter);
            }

            String parentPath = actualPath.substring(0, actualPath.lastIndexOf('/'));
            if (parentPath.isEmpty()) parentPath = "/";
            ZNode parent = getNode(parentPath);
            if (parent == null) throw new RuntimeException("父节点不存在: " + parentPath);

            String nodeName = actualPath.substring(actualPath.lastIndexOf('/') + 1);
            ZNode node = new ZNode(actualPath, type, data, session);
            parent.children.put(nodeName, node);

            // 触发父节点的 Watcher
            triggerWatchers(parent, "NodeChildrenChanged", parentPath);
            return actualPath;
        }

        /** 获取节点数据 */
        String getData(String path) {
            ZNode node = getNode(path);
            return node != null ? node.data : null;
        }

        /** 设置节点数据 */
        void setData(String path, String data) {
            ZNode node = getNode(path);
            if (node == null) throw new RuntimeException("节点不存在: " + path);
            node.data = data;
            triggerWatchers(node, "NodeDataChanged", path);
        }

        /** 获取子节点列表 */
        java.util.List<String> getChildren(String path) {
            ZNode node = getNode(path);
            if (node == null) return java.util.Collections.emptyList();
            return new java.util.ArrayList<>(node.children.keySet());
        }

        /** 删除节点 */
        void delete(String path) {
            String parentPath = path.substring(0, path.lastIndexOf('/'));
            if (parentPath.isEmpty()) parentPath = "/";
            ZNode parent = getNode(parentPath);
            if (parent != null) {
                String nodeName = path.substring(path.lastIndexOf('/') + 1);
                ZNode removed = parent.children.remove(nodeName);
                if (removed != null) {
                    triggerWatchers(removed, "NodeDeleted", path);
                    triggerWatchers(parent, "NodeChildrenChanged", parentPath);
                }
            }
        }

        /** 注册 Watcher */
        void addWatcher(String path, WatcherCallback callback) {
            ZNode node = getNode(path);
            if (node != null) {
                node.watchers.add(callback);
            }
        }

        /** 模拟客户端断开 → 删除该 session 的所有临时节点 */
        int sessionExpired(String session) {
            return deleteEphemeralNodes(root, session);
        }

        private int deleteEphemeralNodes(ZNode node, String session) {
            int deleted = 0;
            java.util.List<String> toRemove = new java.util.ArrayList<>();
            for (var entry : node.children.entrySet()) {
                ZNode child = entry.getValue();
                if ((child.type == NodeType.EPHEMERAL || child.type == NodeType.EPHEMERAL_SEQUENTIAL)
                        && session.equals(child.owner)) {
                    toRemove.add(entry.getKey());
                    deleted++;
                } else {
                    deleted += deleteEphemeralNodes(child, session);
                }
            }
            for (String name : toRemove) {
                ZNode removed = node.children.remove(name);
                if (removed != null) triggerWatchers(node, "NodeChildrenChanged", node.path);
            }
            return deleted;
        }

        private ZNode getNode(String path) {
            if ("/".equals(path)) return root;
            String[] parts = path.substring(1).split("/");
            ZNode current = root;
            for (String part : parts) {
                current = current.children.get(part);
                if (current == null) return null;
            }
            return current;
        }

        private void triggerWatchers(ZNode node, String eventType, String path) {
            for (WatcherCallback w : node.watchers) {
                w.onEvent(eventType, path);
            }
            node.watchers.clear(); // ZK Watcher 是一次性的
        }

        /** 打印树形结构 */
        void printTree() {
            printNode(root, "", true);
        }

        private void printNode(ZNode node, String prefix, boolean isLast) {
            String connector = isLast ? "└── " : "├── ";
            String name = node.path.equals("/") ? "/" : node.path.substring(node.path.lastIndexOf('/') + 1);
            String typeStr = node.type == NodeType.EPHEMERAL || node.type == NodeType.EPHEMERAL_SEQUENTIAL
                    ? " (临时)" : "";
            System.out.printf("    %s%s%s data=%s%n", prefix, connector, name + typeStr,
                    node.data.isEmpty() ? "(空)" : node.data);

            java.util.List<String> childNames = new java.util.ArrayList<>(node.children.keySet());
            for (int i = 0; i < childNames.size(); i++) {
                String childPrefix = prefix + (isLast ? "    " : "│   ");
                printNode(node.children.get(childNames.get(i)), childPrefix, i == childNames.size() - 1);
            }
        }
    }

    // ==================== Part A 演示方法 ====================

    static void demoZNodeTree() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：ZNode 树形结构 + 节点类型");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedZK zk = new SimulatedZK();

        System.out.println("\n  创建节点：");
        zk.create("/services", "", NodeType.PERSISTENT, "system");
        zk.create("/services/user-service", "", NodeType.PERSISTENT, "system");
        zk.create("/services/user-service/instance-", "10.0.0.1:8080",
                NodeType.EPHEMERAL_SEQUENTIAL, "session-1");
        zk.create("/services/user-service/instance-", "10.0.0.2:8080",
                NodeType.EPHEMERAL_SEQUENTIAL, "session-2");
        zk.create("/config", "", NodeType.PERSISTENT, "system");
        zk.create("/config/db.url", "jdbc:mysql://localhost:3306/demo", NodeType.PERSISTENT, "system");

        System.out.println("  ZNode 树：");
        zk.printTree();

        // 获取子节点
        System.out.println("\n  getChildren(/services/user-service)：");
        for (String child : zk.getChildren("/services/user-service")) {
            System.out.printf("    %s → %s%n", child, zk.getData("/services/user-service/" + child));
        }
        System.out.println();
    }

    static void demoWatcher() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Watcher 监听机制");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedZK zk = new SimulatedZK();
        zk.create("/config", "", NodeType.PERSISTENT, "system");
        zk.create("/config/db.url", "old-url", NodeType.PERSISTENT, "system");

        // 注册 Watcher
        System.out.println("\n  注册 Watcher 监听 /config/db.url：");
        zk.addWatcher("/config/db.url", (eventType, path) ->
                System.out.printf("    [Watcher] 事件=%s, 路径=%s%n", eventType, path));

        // 修改数据 → 触发 Watcher
        System.out.println("  修改 /config/db.url 的数据：");
        zk.setData("/config/db.url", "new-url");

        // Watcher 是一次性的，再次修改不会触发
        System.out.println("  再次修改（Watcher 已消费，不会再触发）：");
        zk.setData("/config/db.url", "newer-url");
        System.out.println("    （无 Watcher 输出 — ZK Watcher 是一次性的，需要重新注册）");
        System.out.println();
    }

    static void demoDistributedLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：分布式锁（临时顺序节点）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedZK zk = new SimulatedZK();
        zk.create("/locks", "", NodeType.PERSISTENT, "system");
        zk.create("/locks/order-lock", "", NodeType.PERSISTENT, "system");

        // 三个客户端竞争锁
        System.out.println("\n  三个客户端竞争 /locks/order-lock：");
        String lock1 = zk.create("/locks/order-lock/lock-", "", NodeType.EPHEMERAL_SEQUENTIAL, "client-A");
        String lock2 = zk.create("/locks/order-lock/lock-", "", NodeType.EPHEMERAL_SEQUENTIAL, "client-B");
        String lock3 = zk.create("/locks/order-lock/lock-", "", NodeType.EPHEMERAL_SEQUENTIAL, "client-C");

        System.out.printf("    client-A 创建: %s%n", lock1);
        System.out.printf("    client-B 创建: %s%n", lock2);
        System.out.printf("    client-C 创建: %s%n", lock3);

        // 序号最小的获得锁
        java.util.List<String> children = zk.getChildren("/locks/order-lock");
        java.util.Collections.sort(children);
        System.out.printf("\n  排序后: %s%n", children);
        System.out.printf("  获得锁: %s（序号最小）%n", children.get(0));
        System.out.printf("  等待中: %s（监听前一个节点的删除事件）%n", children.subList(1, children.size()));

        // client-A 释放锁（断开连接 → 临时节点自动删除）
        System.out.println("\n  client-A 释放锁（断开连接）：");
        int deleted = zk.sessionExpired("client-A");
        System.out.printf("  删除 %d 个临时节点%n", deleted);

        children = zk.getChildren("/locks/order-lock");
        java.util.Collections.sort(children);
        System.out.printf("  现在获得锁: %s%n", children.isEmpty() ? "无" : children.get(0));

        System.out.println("\n  ZK 分布式锁原理：");
        System.out.println("    1. 在锁路径下创建临时顺序节点");
        System.out.println("    2. 获取所有子节点并排序");
        System.out.println("    3. 序号最小的获得锁");
        System.out.println("    4. 未获得锁的监听前一个节点的删除事件（避免惊群）");
        System.out.println("    5. 客户端断开 → 临时节点自动删除 → 下一个获得锁");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  ZooKeeper 演示（混合模式）                             ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("══════════ Part A：模拟 ZooKeeper ══════════");
        System.out.println();
        demoZNodeTree();
        demoWatcher();
        demoDistributedLock();

        // Part B：连接真实 ZooKeeper，启动命令：docker compose -f docker/docker-compose.mq.yml up -d zookeeper
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：Curator 连接真实 ZooKeeper ══════════");
            System.out.println();
            RealZooKeeper.run();
        }
    }

    // ==================== Part B：真实 ZooKeeper ====================

    static class RealZooKeeper {

        static final String ZK_CONNECT_STRING = "localhost:2181";

        static void run() throws Exception {
            org.apache.curator.framework.CuratorFramework client = org.apache.curator.framework.CuratorFrameworkFactory.builder()
                    .connectString(ZK_CONNECT_STRING)
                    .sessionTimeoutMs(5000)
                    .retryPolicy(new org.apache.curator.retry.ExponentialBackoffRetry(1000, 3))
                    .build();
            client.start();
            client.blockUntilConnected();
            System.out.println("  连接 ZooKeeper: " + ZK_CONNECT_STRING);

            try {
                // 1. 创建节点
                System.out.println("\n  【创建节点】");
                if (client.checkExists().forPath("/demo") != null) {
                    // 递归删除旧数据
                    client.delete().deletingChildrenIfNeeded().forPath("/demo");
                }
                client.create().creatingParentsIfNeeded().forPath("/demo/config/db.url",
                        "jdbc:mysql://localhost:3306/demo".getBytes());
                client.create().forPath("/demo/config/db.username", "root".getBytes());
                System.out.println("    创建 /demo/config/db.url 和 /demo/config/db.username");

                // 2. 读取数据
                System.out.println("\n  【读取数据】");
                byte[] data = client.getData().forPath("/demo/config/db.url");
                System.out.printf("    /demo/config/db.url = %s%n", new String(data));

                // 3. 获取子节点
                System.out.println("\n  【获取子节点】");
                java.util.List<String> children = client.getChildren().forPath("/demo/config");
                System.out.printf("    /demo/config 子节点: %s%n", children);

                // 4. 分布式锁（Curator InterProcessMutex）
                System.out.println("\n  【分布式锁 — InterProcessMutex】");
                org.apache.curator.framework.recipes.locks.InterProcessMutex lock =
                        new org.apache.curator.framework.recipes.locks.InterProcessMutex(client, "/demo/locks/order");

                boolean acquired = lock.acquire(5, java.util.concurrent.TimeUnit.SECONDS);
                System.out.printf("    获取锁: %s%n", acquired ? "✓ 成功" : "✗ 超时");
                if (acquired) {
                    System.out.println("    执行业务逻辑...");
                    Thread.sleep(100);
                    lock.release();
                    System.out.println("    释放锁 ✓");
                }

                // 5. 临时节点（服务注册）
                System.out.println("\n  【临时节点 — 服务注册】");
                client.create().withMode(org.apache.zookeeper.CreateMode.EPHEMERAL_SEQUENTIAL)
                        .forPath("/demo/services/user-service/instance-", "10.0.0.1:8080".getBytes());
                client.create().withMode(org.apache.zookeeper.CreateMode.EPHEMERAL_SEQUENTIAL)
                        .forPath("/demo/services/user-service/instance-", "10.0.0.2:8080".getBytes());

                java.util.List<String> instances = client.getChildren().forPath("/demo/services/user-service");
                System.out.printf("    注册 2 个临时实例: %s%n", instances);
                System.out.println("    客户端断开后这些节点会自动删除");

            } finally {
                // 清理（如需保留在 ZK 中查看，注释掉 cleanup 调用即可）
                cleanup(client);
                client.close();
            }
        }

        static void cleanup(org.apache.curator.framework.CuratorFramework client) throws Exception {
            if (client.checkExists().forPath("/demo") != null) {
                client.delete().deletingChildrenIfNeeded().forPath("/demo");
            }
            System.out.println("\n  清理：已删除 /demo 节点树");
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }
}

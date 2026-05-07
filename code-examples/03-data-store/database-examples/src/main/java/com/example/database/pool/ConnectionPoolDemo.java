package com.example.database.pool;

/**
 * 数据库连接池演示（混合模式）
 *
 * <p>Part A：用 Semaphore + BlockingQueue 实现简易连接池（直接运行）
 * <p>Part B：用 HikariCP 连接真实 MySQL，体验生产级连接池配置
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}
 *
 * <p>本示例用纯 Java 并发工具实现一个功能完整的连接池：
 * <ul>
 *   <li>核心连接池：获取/归还/超时等待</li>
 *   <li>连接健康检查（心跳检测）</li>
 *   <li>连接泄漏检测</li>
 *   <li>动态扩缩容</li>
 *   <li>HikariCP vs Druid 参数对比</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>连接池工作原理：</h3>
 * <pre>
 *  应用线程                    连接池                      数据库
 *  ┌──────┐   getConnection  ┌──────────┐   TCP 连接    ┌──────┐
 *  │线程 1 │ ──────────────→ │ 空闲连接队列 │ ←─────────→ │ MySQL│
 *  │线程 2 │ ──────────────→ │ [conn1]   │              │      │
 *  │线程 3 │   等待...       │ [conn2]   │              │      │
 *  └──────┘   returnConnection│ [conn3]   │              └──────┘
 *            ←────────────── └──────────┘
 * </pre>
 */
public class ConnectionPoolDemo {

    // ==================== 模拟数据库连接 ====================

    /** 模拟一个数据库连接 */
    static class Connection {
        private static final java.util.concurrent.atomic.AtomicInteger ID_GEN =
                new java.util.concurrent.atomic.AtomicInteger(0);

        final int id;
        final long createTime;
        private volatile long lastActiveTime;
        private volatile boolean valid = true;
        private volatile boolean inUse = false;

        Connection() {
            this.id = ID_GEN.incrementAndGet();
            this.createTime = System.currentTimeMillis();
            this.lastActiveTime = createTime;
        }

        /** 模拟执行 SQL */
        String execute(String sql) {
            if (!valid) throw new IllegalStateException("连接已关闭: conn-" + id);
            lastActiveTime = System.currentTimeMillis();
            // 模拟查询耗时
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return String.format("conn-%d 执行: %s → OK", id, sql);
        }

        /** 心跳检测 */
        boolean isValid() {
            if (!valid) return false;
            // 模拟 SELECT 1 心跳
            lastActiveTime = System.currentTimeMillis();
            return true;
        }

        void close() {
            valid = false;
        }

        long getIdleTime() {
            return System.currentTimeMillis() - lastActiveTime;
        }

        @Override
        public String toString() {
            return String.format("conn-%d [%s, idle=%dms]", id,
                    inUse ? "使用中" : "空闲", getIdleTime());
        }
    }

    // ==================== 连接池实现 ====================

    /**
     * 简易连接池实现。
     * 核心组件：
     * - BlockingQueue：存储空闲连接
     * - Semaphore：控制最大连接数
     * - 后台线程：健康检查 + 泄漏检测
     */
    static class SimpleConnectionPool {
        private final int minSize;          // 最小连接数（核心连接）
        private final int maxSize;          // 最大连接数
        private final long maxWaitMillis;   // 获取连接最大等待时间
        private final long maxIdleMillis;   // 连接最大空闲时间
        private final long leakThreshold;   // 泄漏检测阈值

        private final java.util.concurrent.BlockingQueue<Connection> idlePool;
        private final java.util.concurrent.Semaphore semaphore;
        private final java.util.Set<Connection> activeConnections =
                java.util.Collections.synchronizedSet(new java.util.HashSet<>());
        private final java.util.concurrent.atomic.AtomicInteger totalCreated =
                new java.util.concurrent.atomic.AtomicInteger(0);
        private volatile boolean closed = false;

        // 统计信息
        private final java.util.concurrent.atomic.AtomicLong acquireCount = new java.util.concurrent.atomic.AtomicLong(0);
        private final java.util.concurrent.atomic.AtomicLong timeoutCount = new java.util.concurrent.atomic.AtomicLong(0);
        private final java.util.concurrent.atomic.AtomicLong totalWaitNanos = new java.util.concurrent.atomic.AtomicLong(0);

        SimpleConnectionPool(int minSize, int maxSize, long maxWaitMillis,
                             long maxIdleMillis, long leakThreshold) {
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.maxWaitMillis = maxWaitMillis;
            this.maxIdleMillis = maxIdleMillis;
            this.leakThreshold = leakThreshold;
            this.idlePool = new java.util.concurrent.LinkedBlockingQueue<>(maxSize);
            this.semaphore = new java.util.concurrent.Semaphore(maxSize);

            // 预创建最小连接数
            for (int i = 0; i < minSize; i++) {
                Connection conn = createConnection();
                idlePool.offer(conn);
            }
        }

        /** 获取连接（带超时） */
        Connection getConnection() throws InterruptedException {
            if (closed) throw new IllegalStateException("连接池已关闭");

            long start = System.nanoTime();

            // 1. 尝试获取信号量（控制最大连接数）
            boolean acquired = semaphore.tryAcquire(maxWaitMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!acquired) {
                timeoutCount.incrementAndGet();
                throw new RuntimeException("获取连接超时（" + maxWaitMillis + "ms），活跃连接数: " + activeConnections.size());
            }

            // 2. 从空闲池取连接
            Connection conn = idlePool.poll();
            if (conn == null || !conn.isValid()) {
                // 空闲池为空或连接失效，创建新连接
                if (conn != null) conn.close();
                conn = createConnection();
            }

            conn.inUse = true;
            activeConnections.add(conn);
            acquireCount.incrementAndGet();
            totalWaitNanos.addAndGet(System.nanoTime() - start);

            return conn;
        }

        /** 归还连接 */
        void returnConnection(Connection conn) {
            if (conn == null) return;
            conn.inUse = false;
            activeConnections.remove(conn);

            if (!closed && conn.isValid()) {
                idlePool.offer(conn);
            } else {
                conn.close();
            }
            semaphore.release();
        }

        /** 健康检查：移除失效连接，补充到最小连接数 */
        int healthCheck() {
            int removed = 0;
            java.util.List<Connection> toCheck = new java.util.ArrayList<>();
            idlePool.drainTo(toCheck);

            for (Connection conn : toCheck) {
                if (!conn.isValid() || conn.getIdleTime() > maxIdleMillis) {
                    conn.close();
                    semaphore.release();
                    removed++;
                } else {
                    idlePool.offer(conn);
                }
            }

            // 补充到最小连接数
            while (idlePool.size() < minSize && totalCreated.get() < maxSize) {
                Connection conn = createConnection();
                idlePool.offer(conn);
            }

            return removed;
        }

        /** 泄漏检测：检查使用时间超过阈值的连接 */
        java.util.List<String> detectLeaks() {
            java.util.List<String> leaks = new java.util.ArrayList<>();
            for (Connection conn : activeConnections) {
                long useTime = System.currentTimeMillis() - conn.lastActiveTime;
                if (useTime > leakThreshold) {
                    leaks.add(String.format("conn-%d 使用时间 %dms 超过阈值 %dms，疑似泄漏",
                            conn.id, useTime, leakThreshold));
                }
            }
            return leaks;
        }

        private Connection createConnection() {
            totalCreated.incrementAndGet();
            return new Connection();
        }

        void close() {
            closed = true;
            Connection conn;
            while ((conn = idlePool.poll()) != null) {
                conn.close();
            }
        }

        String getStats() {
            long avgWait = acquireCount.get() > 0 ? totalWaitNanos.get() / acquireCount.get() / 1000 : 0;
            return String.format("空闲:%d 活跃:%d 总创建:%d 获取次数:%d 超时次数:%d 平均等待:%dμs",
                    idlePool.size(), activeConnections.size(), totalCreated.get(),
                    acquireCount.get(), timeoutCount.get(), avgWait);
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：连接池基本使用 */
    static void demoBasicUsage() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：连接池基本使用 — 获取/执行/归还");
        System.out.println("═══════════════════════════════════════════════════");

        SimpleConnectionPool pool = new SimpleConnectionPool(3, 5, 3000, 30000, 5000);
        System.out.printf("\n  初始化连接池: min=%d, max=%d%n", 3, 5);
        System.out.println("  " + pool.getStats());

        // 获取连接并执行 SQL
        System.out.println("\n  获取连接并执行 SQL：");
        Connection conn1 = pool.getConnection();
        System.out.println("    " + conn1.execute("SELECT * FROM user WHERE id = 1"));
        System.out.println("  " + pool.getStats());

        Connection conn2 = pool.getConnection();
        System.out.println("    " + conn2.execute("INSERT INTO user VALUES (2, '李四')"));
        System.out.println("  " + pool.getStats());

        // 归还连接
        pool.returnConnection(conn1);
        pool.returnConnection(conn2);
        System.out.println("\n  归还连接后：");
        System.out.println("  " + pool.getStats());

        pool.close();
        System.out.println();
    }

    /** 演示2：并发获取连接 + 超时等待 */
    static void demoConcurrentAccess() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：并发获取连接 + 超时等待");
        System.out.println("═══════════════════════════════════════════════════");

        // 最大 3 个连接，5 个线程同时请求
        SimpleConnectionPool pool = new SimpleConnectionPool(2, 3, 500, 30000, 5000);
        System.out.println("\n  连接池: max=3, 超时=500ms");
        System.out.println("  5 个线程同时请求连接（每个持有 200ms）：");

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(5);
        for (int i = 1; i <= 5; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    Connection conn = pool.getConnection();
                    System.out.printf("    线程-%d 获取 %s%n", threadId, conn);
                    Thread.sleep(200); // 模拟使用
                    pool.returnConnection(conn);
                    System.out.printf("    线程-%d 归还 conn-%d%n", threadId, conn.id);
                } catch (Exception e) {
                    System.out.printf("    线程-%d %s%n", threadId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("\n  最终统计: " + pool.getStats());
        pool.close();
        System.out.println();
    }

    /** 演示3：健康检查 */
    static void demoHealthCheck() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：连接健康检查");
        System.out.println("═══════════════════════════════════════════════════");

        // maxIdleMillis 设为 100ms，方便演示
        SimpleConnectionPool pool = new SimpleConnectionPool(3, 5, 3000, 100, 5000);
        System.out.println("\n  连接池: min=3, maxIdle=100ms");
        System.out.println("  初始: " + pool.getStats());

        // 等待连接超过空闲时间
        Thread.sleep(150);
        System.out.println("  等待 150ms 后...");

        // 执行健康检查
        int removed = pool.healthCheck();
        System.out.printf("  健康检查: 移除 %d 个过期连接，补充到最小连接数%n", removed);
        System.out.println("  检查后: " + pool.getStats());

        pool.close();
        System.out.println();
    }

    /** 演示4：连接泄漏检测 */
    static void demoLeakDetection() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：连接泄漏检测");
        System.out.println("═══════════════════════════════════════════════════");

        // leakThreshold 设为 200ms
        SimpleConnectionPool pool = new SimpleConnectionPool(3, 5, 3000, 30000, 200);
        System.out.println("\n  泄漏检测阈值: 200ms");

        // 获取连接但"忘记"归还
        Connection leaked = pool.getConnection();
        System.out.printf("  获取 conn-%d 但不归还（模拟泄漏）%n", leaked.id);

        Thread.sleep(300);

        // 检测泄漏
        java.util.List<String> leaks = pool.detectLeaks();
        System.out.println("\n  泄漏检测结果：");
        if (leaks.isEmpty()) {
            System.out.println("    无泄漏");
        } else {
            for (String leak : leaks) {
                System.out.println("    ⚠ " + leak);
            }
        }

        System.out.println("\n  预防连接泄漏的最佳实践：");
        System.out.println("    1. 使用 try-with-resources 自动关闭连接");
        System.out.println("    2. 开启连接池的泄漏检测（HikariCP: leakDetectionThreshold）");
        System.out.println("    3. 设置连接最大生命周期（maxLifetime）");

        pool.returnConnection(leaked);
        pool.close();
        System.out.println();
    }

    /** 演示5：HikariCP vs Druid 参数对比 */
    static void demoPoolComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：HikariCP vs Druid 参数对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"参数",                "HikariCP",                    "Druid"},
                {"最小空闲连接",         "minimumIdle=10",              "minIdle=10"},
                {"最大连接数",           "maximumPoolSize=20",          "maxActive=20"},
                {"获取超时",            "connectionTimeout=30000",      "maxWait=30000"},
                {"空闲超时",            "idleTimeout=600000",           "minEvictableIdleTimeMillis=300000"},
                {"最大生命周期",         "maxLifetime=1800000",          "maxEvictableIdleTimeMillis=900000"},
                {"心跳检测",            "connectionTestQuery=SELECT 1", "validationQuery=SELECT 1"},
                {"泄漏检测",            "leakDetectionThreshold=60000", "removeAbandoned=true"},
                {"监控",               "无内置监控",                    "内置 StatFilter + Web 监控页"},
                {"性能",               "★★★★★（最快）",               "★★★★☆（功能更全）"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-16s %-35s %-35s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2]);
            if (i == 0) {
                System.out.println("  " + "─".repeat(86));
            }
        }

        System.out.println("\n  推荐：");
        System.out.println("    Spring Boot 默认使用 HikariCP（性能最优）");
        System.out.println("    需要监控和 SQL 防火墙时选择 Druid");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  数据库连接池演示（混合模式）                           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：手写连接池模拟 ══════════");
        System.out.println();
        demoBasicUsage();
        demoConcurrentAccess();
        demoHealthCheck();
        demoLeakDetection();
        demoPoolComparison();

        // ===== Part B：HikariCP 真实连接池 =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：HikariCP 连接真实 MySQL ══════════");
            System.out.println();
            RealHikariPool.run();
        } else {
            System.out.println("提示：运行 Part B（HikariCP 真实连接池）请传入参数 'real'");
            System.out.println("  java -cp ... com.example.database.pool.ConnectionPoolDemo real");
        }
    }

    // ==================== Part B：HikariCP 真实连接池 ====================

    /**
     * Part B：使用 HikariCP 连接真实 MySQL，演示生产级连接池配置和监控。
     * 需要先启动 MySQL：docker compose -f docker/docker-compose.yml up -d mysql
     */
    static class RealHikariPool {

        static void run() throws Exception {
            // 1. 配置 HikariCP
            com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
            config.setUsername("root");
            config.setPassword("root123");

            // 核心参数
            config.setMinimumIdle(2);           // 最小空闲连接
            config.setMaximumPoolSize(5);       // 最大连接数
            config.setConnectionTimeout(3000);  // 获取连接超时（ms）
            config.setIdleTimeout(60000);       // 空闲连接超时（ms）
            config.setMaxLifetime(300000);      // 连接最大生命周期（ms）
            config.setLeakDetectionThreshold(5000); // 泄漏检测阈值（ms）
            config.setPoolName("DemoPool");

            System.out.println("  HikariCP 配置：");
            System.out.printf("    minimumIdle=%d, maximumPoolSize=%d%n", 2, 5);
            System.out.printf("    connectionTimeout=%dms, idleTimeout=%dms%n", 3000, 60000);
            System.out.printf("    leakDetectionThreshold=%dms%n", 5000);

            com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource(config);

            try {
                // 2. 基本使用
                System.out.println("\n  【基本使用】获取连接执行 SQL：");
                try (java.sql.Connection conn = ds.getConnection()) {
                    java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT VERSION()");
                    if (rs.next()) {
                        System.out.println("    MySQL 版本: " + rs.getString(1));
                    }
                }

                // 3. 连接池状态
                System.out.println("\n  【连接池状态】");
                com.zaxxer.hikari.HikariPoolMXBean poolMXBean = ds.getHikariPoolMXBean();
                System.out.printf("    总连接数: %d%n", poolMXBean.getTotalConnections());
                System.out.printf("    活跃连接: %d%n", poolMXBean.getActiveConnections());
                System.out.printf("    空闲连接: %d%n", poolMXBean.getIdleConnections());
                System.out.printf("    等待线程: %d%n", poolMXBean.getThreadsAwaitingConnection());

                // 4. 并发压测
                System.out.println("\n  【并发压测】10 个线程同时获取连接（最大 5 个）：");
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(10);
                long start = System.currentTimeMillis();

                for (int i = 0; i < 10; i++) {
                    final int threadId = i + 1;
                    new Thread(() -> {
                        try (java.sql.Connection conn = ds.getConnection()) {
                            conn.createStatement().executeQuery("SELECT 1");
                            Thread.sleep(100);
                            System.out.printf("    线程-%d 完成%n", threadId);
                        } catch (Exception e) {
                            System.out.printf("    线程-%d 失败: %s%n", threadId, e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }).start();
                }

                latch.await();
                long elapsed = System.currentTimeMillis() - start;
                System.out.printf("    总耗时: %d ms%n", elapsed);
                System.out.printf("    最终状态: 总=%d, 活跃=%d, 空闲=%d%n",
                        poolMXBean.getTotalConnections(),
                        poolMXBean.getActiveConnections(),
                        poolMXBean.getIdleConnections());

            } finally {
                ds.close();
                System.out.println("\n  连接池已关闭");
            }
        }
    }
}

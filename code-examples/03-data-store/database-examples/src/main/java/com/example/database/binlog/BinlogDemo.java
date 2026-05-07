package com.example.database.binlog;

/**
 * MySQL Binlog 与主从复制演示（混合模式）
 *
 * <p>Part A：用 ArrayList 模拟 Binlog 写入、回放、数据恢复（直接运行）
 * <p>Part B：用 mysql-binlog-connector 监听真实 MySQL Binlog + JDBC 触发变更
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}
 * <p>需要开启 MySQL Binlog（Row 格式）：
 * <pre>
 * [mysqld]
 * log-bin=mysql-bin
 * binlog-format=ROW
 * server-id=1
 * </pre>
 *
 * <p>本示例用纯 Java 模拟 MySQL 的 Binlog 机制：
 * <ul>
 *   <li>三种 Binlog 格式：STATEMENT / ROW / MIXED</li>
 *   <li>Binlog 事件写入与回放</li>
 *   <li>基于 Binlog 的主从复制流程</li>
 *   <li>基于 Binlog 的数据恢复（Point-in-Time Recovery）</li>
 *   <li>Canal 模拟：解析 Binlog 实现数据同步</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>MySQL 主从复制架构：</h3>
 * <pre>
 *  Master                          Slave
 *  ┌──────────┐                   ┌──────────┐
 *  │ 客户端写入 │                   │          │
 *  │    ↓     │   Binlog Dump     │          │
 *  │ Binlog   │ ──────────────→   │ Relay Log│
 *  │          │   (I/O Thread)    │    ↓     │
 *  │          │                   │ SQL Thread│
 *  │          │                   │    ↓     │
 *  │  数据文件  │                   │  数据文件  │
 *  └──────────┘                   └──────────┘
 * </pre>
 */
public class BinlogDemo {

    // ==================== Binlog 事件模型 ====================

    /** Binlog 格式 */
    enum BinlogFormat {
        STATEMENT,  // 记录 SQL 语句（可能导致主从不一致，如 NOW()、UUID()）
        ROW,        // 记录行数据变更（数据量大但最安全）
        MIXED       // 混合模式（默认 STATEMENT，不安全时自动切换 ROW）
    }

    /** Binlog 事件类型 */
    enum EventType {
        QUERY,          // DDL 或 STATEMENT 格式的 DML
        TABLE_MAP,      // ROW 格式：表映射事件
        WRITE_ROWS,     // ROW 格式：INSERT
        UPDATE_ROWS,    // ROW 格式：UPDATE
        DELETE_ROWS,    // ROW 格式：DELETE
        XID,            // 事务提交标记
        GTID            // 全局事务 ID
    }

    /** 一条 Binlog 事件 */
    static class BinlogEvent {
        final long position;        // 在 Binlog 文件中的偏移量
        final long timestamp;       // 事件时间戳
        final EventType type;
        final String database;
        final String table;
        final String sql;           // STATEMENT 格式的 SQL
        final String beforeData;    // ROW 格式：修改前的数据
        final String afterData;     // ROW 格式：修改后的数据
        final long transactionId;

        BinlogEvent(long position, EventType type, String database, String table,
                    String sql, String beforeData, String afterData, long transactionId) {
            this.position = position;
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.database = database;
            this.table = table;
            this.sql = sql;
            this.beforeData = beforeData;
            this.afterData = afterData;
            this.transactionId = transactionId;
        }

        @Override
        public String toString() {
            if (type == EventType.QUERY) {
                return String.format("[pos=%d] %s | %s.%s | SQL: %s", position, type, database, table, sql);
            }
            return String.format("[pos=%d] %s | %s.%s | before=%s → after=%s",
                    position, type, database, table,
                    beforeData == null ? "null" : beforeData,
                    afterData == null ? "null" : afterData);
        }
    }

    // ==================== Binlog 文件 ====================

    /** 模拟一个 Binlog 文件 */
    static class BinlogFile {
        final String fileName;
        private final java.util.List<BinlogEvent> events = new java.util.ArrayList<>();
        private long nextPosition = 4; // Binlog 文件头占 4 字节

        BinlogFile(String fileName) {
            this.fileName = fileName;
        }

        void appendEvent(BinlogEvent event) {
            events.add(event);
        }

        long getNextPosition() {
            return nextPosition += 100; // 简化：每个事件占 100 字节
        }

        java.util.List<BinlogEvent> getEvents() {
            return java.util.Collections.unmodifiableList(events);
        }

        /** 从指定位置开始读取事件（模拟 mysqlbinlog --start-position） */
        java.util.List<BinlogEvent> readFrom(long startPosition) {
            java.util.List<BinlogEvent> result = new java.util.ArrayList<>();
            for (BinlogEvent event : events) {
                if (event.position >= startPosition) {
                    result.add(event);
                }
            }
            return result;
        }
    }

    // ==================== 模拟数据库 ====================

    /** 简化的数据库表，用于演示 Binlog 回放 */
    static class SimpleTable {
        final String name;
        private final java.util.Map<Integer, String> rows = new java.util.LinkedHashMap<>();

        SimpleTable(String name) {
            this.name = name;
        }

        void insert(int id, String data) {
            rows.put(id, data);
        }

        void update(int id, String newData) {
            rows.put(id, newData);
        }

        void delete(int id) {
            rows.remove(id);
        }

        String get(int id) {
            return rows.get(id);
        }

        void printAll() {
            for (var entry : rows.entrySet()) {
                System.out.printf("      id=%d → %s%n", entry.getKey(), entry.getValue());
            }
        }

        int size() {
            return rows.size();
        }
    }

    // ==================== 主从复制模拟 ====================

    /** 模拟 Slave 的 I/O Thread + SQL Thread */
    static class SlaveReplication {
        private final SimpleTable slaveTable;
        private long lastPosition = 0;
        private final java.util.List<BinlogEvent> relayLog = new java.util.ArrayList<>();

        SlaveReplication(SimpleTable slaveTable) {
            this.slaveTable = slaveTable;
        }

        /** I/O Thread：从 Master 拉取 Binlog 事件写入 Relay Log */
        void ioThread(BinlogFile masterBinlog) {
            java.util.List<BinlogEvent> newEvents = masterBinlog.readFrom(lastPosition);
            for (BinlogEvent event : newEvents) {
                relayLog.add(event);
                lastPosition = event.position + 1;
            }
        }

        /** SQL Thread：回放 Relay Log 中的事件 */
        int sqlThread() {
            int replayed = 0;
            for (BinlogEvent event : relayLog) {
                applyEvent(event);
                replayed++;
            }
            relayLog.clear();
            return replayed;
        }

        private void applyEvent(BinlogEvent event) {
            switch (event.type) {
                case WRITE_ROWS:
                    // 解析 afterData 格式："id=1,name=张三"
                    parseAndInsert(event.afterData);
                    break;
                case UPDATE_ROWS:
                    parseAndUpdate(event.afterData);
                    break;
                case DELETE_ROWS:
                    parseAndDelete(event.beforeData);
                    break;
                default:
                    break;
            }
        }

        private void parseAndInsert(String data) {
            String[] parts = data.split(",");
            int id = Integer.parseInt(parts[0].split("=")[1]);
            String name = parts[1].split("=")[1];
            slaveTable.insert(id, name);
        }

        private void parseAndUpdate(String data) {
            String[] parts = data.split(",");
            int id = Integer.parseInt(parts[0].split("=")[1]);
            String name = parts[1].split("=")[1];
            slaveTable.update(id, name);
        }

        private void parseAndDelete(String data) {
            String[] parts = data.split(",");
            int id = Integer.parseInt(parts[0].split("=")[1]);
            slaveTable.delete(id);
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：三种 Binlog 格式对比 */
    static void demoBinlogFormats() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：三种 Binlog 格式对比");
        System.out.println("═══════════════════════════════════════════════════");

        BinlogFile statementLog = new BinlogFile("mysql-bin.000001");
        BinlogFile rowLog = new BinlogFile("mysql-bin.000002");

        // 同一条 UPDATE 在不同格式下的记录方式
        System.out.println("\n  SQL: UPDATE user SET age = age + 1 WHERE city = '北京'（影响 3 行）");

        // STATEMENT 格式：记录 SQL
        long pos1 = statementLog.getNextPosition();
        statementLog.appendEvent(new BinlogEvent(pos1, EventType.QUERY, "mydb", "user",
                "UPDATE user SET age = age + 1 WHERE city = '北京'", null, null, 1));

        System.out.println("\n  【STATEMENT 格式】记录原始 SQL");
        for (BinlogEvent e : statementLog.getEvents()) {
            System.out.println("    " + e);
        }
        System.out.println("    优点：日志量小");
        System.out.println("    缺点：NOW()/UUID()/RAND() 等函数在从库执行结果可能不同");

        // ROW 格式：记录每行变更
        String[][] rowChanges = {
                {"id=1,name=张三,age=25,city=北京", "id=1,name=张三,age=26,city=北京"},
                {"id=3,name=王五,age=25,city=北京", "id=3,name=王五,age=26,city=北京"},
                {"id=6,name=周八,age=22,city=北京", "id=6,name=周八,age=23,city=北京"},
        };

        System.out.println("\n  【ROW 格式】记录每行数据的前后变化");
        for (String[] change : rowChanges) {
            long pos = rowLog.getNextPosition();
            rowLog.appendEvent(new BinlogEvent(pos, EventType.UPDATE_ROWS, "mydb", "user",
                    null, change[0], change[1], 1));
        }
        for (BinlogEvent e : rowLog.getEvents()) {
            System.out.println("    " + e);
        }
        System.out.println("    优点：数据一致性最好，是 Canal 等工具的基础");
        System.out.println("    缺点：日志量大（每行都记录完整前后数据）");

        System.out.println("\n  【MIXED 格式】默认用 STATEMENT，遇到不安全函数自动切换 ROW");
        System.out.println("    推荐：生产环境建议使用 ROW 格式（binlog_format=ROW）");
        System.out.println();
    }

    /** 演示2：主从复制流程 */
    static void demoMasterSlaveReplication() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：主从复制流程模拟");
        System.out.println("═══════════════════════════════════════════════════");

        // Master 端
        SimpleTable masterTable = new SimpleTable("user");
        BinlogFile masterBinlog = new BinlogFile("mysql-bin.000001");

        // Slave 端
        SimpleTable slaveTable = new SimpleTable("user");
        SlaveReplication slave = new SlaveReplication(slaveTable);

        // Master 执行写操作，同时写 Binlog
        System.out.println("\n  【Master 写入数据】");
        masterTable.insert(1, "张三");
        masterBinlog.appendEvent(new BinlogEvent(masterBinlog.getNextPosition(),
                EventType.WRITE_ROWS, "mydb", "user", null, null, "id=1,name=张三", 1));

        masterTable.insert(2, "李四");
        masterBinlog.appendEvent(new BinlogEvent(masterBinlog.getNextPosition(),
                EventType.WRITE_ROWS, "mydb", "user", null, null, "id=2,name=李四", 2));

        masterTable.update(1, "张三丰");
        masterBinlog.appendEvent(new BinlogEvent(masterBinlog.getNextPosition(),
                EventType.UPDATE_ROWS, "mydb", "user", null, "id=1,name=张三", "id=1,name=张三丰", 3));

        System.out.println("    Master 数据:");
        masterTable.printAll();

        // Slave 复制
        System.out.println("\n  【Slave I/O Thread 拉取 Binlog】");
        slave.ioThread(masterBinlog);
        System.out.printf("    拉取了 %d 个事件到 Relay Log%n", masterBinlog.getEvents().size());

        System.out.println("\n  【Slave SQL Thread 回放 Relay Log】");
        int replayed = slave.sqlThread();
        System.out.printf("    回放了 %d 个事件%n", replayed);

        System.out.println("\n    Slave 数据（应与 Master 一致）:");
        slaveTable.printAll();

        boolean consistent = masterTable.get(1).equals(slaveTable.get(1))
                && masterTable.get(2).equals(slaveTable.get(2));
        System.out.printf("\n    主从数据一致性: %s%n", consistent ? "✓ 一致" : "✗ 不一致");
        System.out.println();
    }

    /** 演示3：基于 Binlog 的数据恢复 */
    static void demoPointInTimeRecovery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：基于 Binlog 的数据恢复（PITR）");
        System.out.println("═══════════════════════════════════════════════════");

        BinlogFile binlog = new BinlogFile("mysql-bin.000001");
        SimpleTable table = new SimpleTable("user");

        // 模拟一系列操作
        System.out.println("\n  操作序列：");
        long pos1 = binlog.getNextPosition();
        table.insert(1, "张三");
        binlog.appendEvent(new BinlogEvent(pos1, EventType.WRITE_ROWS, "mydb", "user",
                null, null, "id=1,name=张三", 1));
        System.out.printf("    [pos=%d] INSERT id=1, name=张三%n", pos1);

        long pos2 = binlog.getNextPosition();
        table.insert(2, "李四");
        binlog.appendEvent(new BinlogEvent(pos2, EventType.WRITE_ROWS, "mydb", "user",
                null, null, "id=2,name=李四", 2));
        System.out.printf("    [pos=%d] INSERT id=2, name=李四%n", pos2);

        long pos3 = binlog.getNextPosition();
        table.update(1, "张三丰");
        binlog.appendEvent(new BinlogEvent(pos3, EventType.UPDATE_ROWS, "mydb", "user",
                null, "id=1,name=张三", "id=1,name=张三丰", 3));
        System.out.printf("    [pos=%d] UPDATE id=1, name=张三→张三丰%n", pos3);

        // 误操作：删除了数据
        long pos4 = binlog.getNextPosition();
        table.delete(2);
        binlog.appendEvent(new BinlogEvent(pos4, EventType.DELETE_ROWS, "mydb", "user",
                null, "id=2,name=李四", null, 4));
        System.out.printf("    [pos=%d] DELETE id=2 ← 误操作！%n", pos4);

        System.out.println("\n    当前数据（误删后）:");
        table.printAll();

        // 恢复：从全量备份 + 回放 Binlog 到误操作之前
        System.out.printf("\n  【恢复方案】回放 Binlog 到 pos=%d 之前（跳过误操作）%n", pos4);
        System.out.println("  命令：mysqlbinlog --start-position=4 --stop-position=" + pos4 + " mysql-bin.000001 | mysql");

        SimpleTable recovered = new SimpleTable("user");
        java.util.List<BinlogEvent> replayEvents = binlog.readFrom(0);
        for (BinlogEvent event : replayEvents) {
            if (event.position >= pos4) break; // 停在误操作之前
            // 简化回放
            if (event.type == EventType.WRITE_ROWS && event.afterData != null) {
                String[] parts = event.afterData.split(",");
                int id = Integer.parseInt(parts[0].split("=")[1]);
                String name = parts[1].split("=")[1];
                recovered.insert(id, name);
            } else if (event.type == EventType.UPDATE_ROWS && event.afterData != null) {
                String[] parts = event.afterData.split(",");
                int id = Integer.parseInt(parts[0].split("=")[1]);
                String name = parts[1].split("=")[1];
                recovered.update(id, name);
            }
        }

        System.out.println("\n    恢复后数据:");
        recovered.printAll();
        System.out.printf("    恢复结果: %d 行数据（误删的 id=2 已恢复）%n", recovered.size());
        System.out.println();
    }

    /** 演示4：Canal 数据同步模拟 */
    static void demoCanalSync() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Canal 数据同步模拟");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  Canal 原理：伪装成 MySQL Slave，接收 Master 的 Binlog 并解析");
        System.out.println("  典型用途：MySQL → ES 同步、MySQL → Redis 缓存更新、数据审计");

        // 模拟 Canal 解析 Binlog 事件
        BinlogFile binlog = new BinlogFile("mysql-bin.000001");
        binlog.appendEvent(new BinlogEvent(binlog.getNextPosition(),
                EventType.WRITE_ROWS, "mydb", "user", null, null, "id=1,name=张三,age=25", 1));
        binlog.appendEvent(new BinlogEvent(binlog.getNextPosition(),
                EventType.UPDATE_ROWS, "mydb", "user", null, "id=1,name=张三,age=25", "id=1,name=张三,age=26", 2));
        binlog.appendEvent(new BinlogEvent(binlog.getNextPosition(),
                EventType.DELETE_ROWS, "mydb", "user", null, "id=1,name=张三,age=26", null, 3));

        // Canal 消费者：同步到 ES
        System.out.println("\n  【Canal 消费者 — 同步到 Elasticsearch】");
        for (BinlogEvent event : binlog.getEvents()) {
            switch (event.type) {
                case WRITE_ROWS:
                    System.out.printf("    ES Index: PUT /user/_doc/? → %s%n", event.afterData);
                    break;
                case UPDATE_ROWS:
                    System.out.printf("    ES Update: POST /user/_update/? → %s%n", event.afterData);
                    break;
                case DELETE_ROWS:
                    System.out.printf("    ES Delete: DELETE /user/_doc/? → %s%n", event.beforeData);
                    break;
                default:
                    break;
            }
        }

        // Canal 消费者：更新 Redis 缓存
        System.out.println("\n  【Canal 消费者 — 更新 Redis 缓存】");
        java.util.Map<String, String> redisCache = new java.util.LinkedHashMap<>();
        for (BinlogEvent event : binlog.getEvents()) {
            switch (event.type) {
                case WRITE_ROWS:
                case UPDATE_ROWS:
                    String[] parts = event.afterData.split(",");
                    String id = parts[0].split("=")[1];
                    redisCache.put("user:" + id, event.afterData);
                    System.out.printf("    Redis SET user:%s → %s%n", id, event.afterData);
                    break;
                case DELETE_ROWS:
                    String[] delParts = event.beforeData.split(",");
                    String delId = delParts[0].split("=")[1];
                    redisCache.remove("user:" + delId);
                    System.out.printf("    Redis DEL user:%s%n", delId);
                    break;
                default:
                    break;
            }
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MySQL Binlog 与主从复制演示（混合模式）                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：内存模拟 Binlog 机制 ══════════");
        System.out.println();
        demoBinlogFormats();
        demoMasterSlaveReplication();
        demoPointInTimeRecovery();
        demoCanalSync();

        // ===== Part B：连接真实 MySQL 监听 Binlog =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：监听真实 MySQL Binlog ══════════");
            System.out.println();
            RealBinlogListener.run();
        } else {
            System.out.println("提示：运行 Part B（真实 Binlog 监听）请传入参数 'real'");
            System.out.println("  java -cp ... com.example.database.binlog.BinlogDemo real");
        }
    }

    // ==================== Part B：真实 MySQL Binlog 监听 ====================

    /**
     * Part B：用 mysql-binlog-connector 监听真实 MySQL 的 Binlog 变更事件，
     * 同时用 JDBC 执行 DML 触发 Binlog 事件。
     *
     * <p>需要先启动 MySQL：docker compose -f docker/docker-compose.yml up -d mysql
     * <p>MySQL 需要开启 Binlog（Row 格式），Docker 镜像默认已开启。
     */
    static class RealBinlogListener {

        static final String JDBC_URL = "jdbc:mysql://localhost:3306/demo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        static final String USER = "root";
        static final String PASSWORD = "root123";

        static void run() throws Exception {
            // 1. 先用 JDBC 创建测试表
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
                java.sql.Statement stmt = conn.createStatement();
                stmt.execute("CREATE DATABASE IF NOT EXISTS demo_db");
                stmt.execute("USE demo_db");
                stmt.execute("DROP TABLE IF EXISTS binlog_demo");
                stmt.execute("CREATE TABLE binlog_demo (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50), age INT) ENGINE=InnoDB");
                System.out.println("  准备：创建测试表 binlog_demo");
            }

            // 2. 启动 Binlog 监听器（后台线程）
            com.github.shyiko.mysql.binlog.BinaryLogClient client =
                    new com.github.shyiko.mysql.binlog.BinaryLogClient("localhost", 3306, USER, PASSWORD);

            java.util.concurrent.CountDownLatch eventsReceived = new java.util.concurrent.CountDownLatch(3);

            client.registerEventListener(event -> {
                com.github.shyiko.mysql.binlog.event.EventData data = event.getData();

                if (data instanceof com.github.shyiko.mysql.binlog.event.WriteRowsEventData) {
                    com.github.shyiko.mysql.binlog.event.WriteRowsEventData writeData =
                            (com.github.shyiko.mysql.binlog.event.WriteRowsEventData) data;
                    System.out.printf("  [Binlog INSERT] tableId=%d, rows=%s%n",
                            writeData.getTableId(), writeData.getRows());
                    eventsReceived.countDown();
                } else if (data instanceof com.github.shyiko.mysql.binlog.event.UpdateRowsEventData) {
                    com.github.shyiko.mysql.binlog.event.UpdateRowsEventData updateData =
                            (com.github.shyiko.mysql.binlog.event.UpdateRowsEventData) data;
                    System.out.printf("  [Binlog UPDATE] tableId=%d, rows=%s%n",
                            updateData.getTableId(), updateData.getRows());
                    eventsReceived.countDown();
                } else if (data instanceof com.github.shyiko.mysql.binlog.event.DeleteRowsEventData) {
                    com.github.shyiko.mysql.binlog.event.DeleteRowsEventData deleteData =
                            (com.github.shyiko.mysql.binlog.event.DeleteRowsEventData) data;
                    System.out.printf("  [Binlog DELETE] tableId=%d, rows=%s%n",
                            deleteData.getTableId(), deleteData.getRows());
                    eventsReceived.countDown();
                }
            });

            // 后台线程启动监听
            Thread listenerThread = new Thread(() -> {
                try { client.connect(); } catch (Exception e) { e.printStackTrace(); }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

            // 等待连接建立
            Thread.sleep(2000);
            System.out.println("  Binlog 监听器已启动，开始执行 DML 触发事件...\n");

            // 3. 用 JDBC 执行 DML，触发 Binlog 事件
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
                java.sql.Statement stmt = conn.createStatement();

                System.out.println("  执行 INSERT...");
                stmt.execute("INSERT INTO binlog_demo (name, age) VALUES ('张三', 25)");

                System.out.println("  执行 UPDATE...");
                stmt.execute("UPDATE binlog_demo SET age = 26 WHERE name = '张三'");

                System.out.println("  执行 DELETE...");
                stmt.execute("DELETE FROM binlog_demo WHERE name = '张三'");
            }

            // 等待 Binlog 事件接收
            boolean received = eventsReceived.await(10, java.util.concurrent.TimeUnit.SECONDS);
            System.out.printf("\n  Binlog 事件接收: %s%n", received ? "✓ 全部收到" : "⚠ 超时（部分未收到）");

            // 4. 清理（如需保留测试表，注释掉 cleanup 调用即可）
            client.disconnect();
            cleanup();
        }

        /** 清理测试表。如需保留数据，注释掉 cleanup() 调用即可 */
        static void cleanup() throws Exception {
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
                conn.createStatement().execute("DROP TABLE IF EXISTS binlog_demo");
                System.out.println("  清理：已删除测试表 binlog_demo");
                System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
            }
        }
    }
}

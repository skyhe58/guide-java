package com.example.database.optimization;

/**
 * MySQL SQL 优化演示（混合模式）
 *
 * <p>Part A：用纯 Java 模拟查询优化器行为（直接运行）
 * <p>Part B：用 JDBC 连接真实 MySQL 执行 EXPLAIN（需要 Docker）
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}
 *
 * <p>本示例用纯 Java 模拟 MySQL 查询优化器的核心行为：
 * <ul>
 *   <li>慢查询日志检测与分析</li>
 *   <li>EXPLAIN 执行计划各字段含义</li>
 *   <li>索引选择策略（全表扫描 vs 索引扫描）</li>
 *   <li>常见 SQL 优化技巧</li>
 *   <li>查询成本估算模型</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 */
public class OptimizationDemo {

    // ==================== EXPLAIN 执行计划模型 ====================

    /**
     * 模拟 EXPLAIN 输出的一行，对应 MySQL EXPLAIN 的各个字段
     */
    static class ExplainRow {
        final int id;               // 查询序号
        final String selectType;    // 查询类型：SIMPLE/PRIMARY/SUBQUERY/DERIVED/UNION
        final String table;         // 访问的表
        final String type;          // 访问类型：system > const > eq_ref > ref > range > index > ALL
        final String possibleKeys;  // 可能使用的索引
        final String key;           // 实际使用的索引
        final int keyLen;           // 索引使用的字节数
        final String ref;           // 索引查找时使用的列或常量
        final long rows;            // 预估扫描行数
        final double filtered;      // 过滤后剩余行的百分比
        final String extra;         // 额外信息：Using index / Using where / Using filesort 等

        ExplainRow(int id, String selectType, String table, String type,
                   String possibleKeys, String key, int keyLen, String ref,
                   long rows, double filtered, String extra) {
            this.id = id;
            this.selectType = selectType;
            this.table = table;
            this.type = type;
            this.possibleKeys = possibleKeys;
            this.key = key;
            this.keyLen = keyLen;
            this.ref = ref;
            this.rows = rows;
            this.filtered = filtered;
            this.extra = extra;
        }
    }

    /**
     * 访问类型性能排序（从好到差）
     */
    static final String[] ACCESS_TYPE_ORDER = {
            "system",   // 表只有一行（系统表）
            "const",    // 主键或唯一索引等值查询，最多一行
            "eq_ref",   // JOIN 时使用主键或唯一索引，每次关联一行
            "ref",      // 非唯一索引等值查询，可能多行
            "range",    // 索引范围扫描（BETWEEN, >, <, IN）
            "index",    // 全索引扫描（遍历整个索引树）
            "ALL"       // 全表扫描（最差）
    };

    // ==================== 慢查询日志模型 ====================

    /**
     * 模拟慢查询日志条目
     */
    static class SlowQueryLog {
        final String sql;
        final double queryTime;     // 查询耗时（秒）
        final long rowsExamined;    // 扫描行数
        final long rowsSent;        // 返回行数
        final String timestamp;

        SlowQueryLog(String sql, double queryTime, long rowsExamined, long rowsSent, String timestamp) {
            this.sql = sql;
            this.queryTime = queryTime;
            this.rowsExamined = rowsExamined;
            this.rowsSent = rowsSent;
            this.timestamp = timestamp;
        }
    }

    // ==================== 查询成本估算 ====================

    /**
     * 模拟 MySQL 查询优化器的成本估算模型。
     * MySQL 优化器通过比较不同执行计划的成本来选择最优方案。
     * 成本 = I/O 成本 + CPU 成本
     */
    static class CostEstimator {
        // MySQL 默认成本常量
        static final double IO_COST_PER_PAGE = 1.0;        // 读取一个数据页的 I/O 成本
        static final double CPU_COST_PER_ROW = 0.2;        // 处理一行数据的 CPU 成本
        static final int ROWS_PER_PAGE = 100;               // 每页存储的行数（简化）

        /**
         * 估算全表扫描成本
         */
        static double estimateFullScanCost(long totalRows) {
            double pages = Math.ceil((double) totalRows / ROWS_PER_PAGE);
            double ioCost = pages * IO_COST_PER_PAGE;
            double cpuCost = totalRows * CPU_COST_PER_ROW;
            return ioCost + cpuCost;
        }

        /**
         * 估算索引扫描成本（含回表）
         */
        static double estimateIndexScanCost(long matchedRows, long totalRows) {
            // 索引扫描 I/O：索引页 + 回表页
            double indexPages = Math.ceil((double) matchedRows / ROWS_PER_PAGE);
            double backToTablePages = matchedRows; // 最坏情况：每行回表一次
            double ioCost = (indexPages + backToTablePages) * IO_COST_PER_PAGE;
            double cpuCost = matchedRows * CPU_COST_PER_ROW;
            return ioCost + cpuCost;
        }

        /**
         * 估算覆盖索引扫描成本（无回表）
         */
        static double estimateCoveringIndexCost(long matchedRows) {
            double indexPages = Math.ceil((double) matchedRows / ROWS_PER_PAGE);
            double ioCost = indexPages * IO_COST_PER_PAGE;
            double cpuCost = matchedRows * CPU_COST_PER_ROW;
            return ioCost + cpuCost;
        }
    }

    // ==================== 演示方法 ====================

    /**
     * 演示1：慢查询日志分析
     */
    static void demoSlowQueryAnalysis() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：慢查询日志分析");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟慢查询日志
        SlowQueryLog[] logs = {
                new SlowQueryLog(
                        "SELECT * FROM orders WHERE user_id = 12345",
                        0.003, 1, 1, "2024-01-15 10:30:00"),
                new SlowQueryLog(
                        "SELECT * FROM orders WHERE status = 'pending'",
                        2.5, 500000, 1200, "2024-01-15 10:31:00"),
                new SlowQueryLog(
                        "SELECT * FROM orders WHERE DATE(create_time) = '2024-01-15'",
                        5.2, 1000000, 3500, "2024-01-15 10:32:00"),
                new SlowQueryLog(
                        "SELECT * FROM orders o JOIN order_items oi ON o.id = oi.order_id WHERE o.amount > 100",
                        8.1, 2000000, 50000, "2024-01-15 10:33:00"),
        };

        System.out.println("\n  慢查询阈值：long_query_time = 1 秒");
        System.out.println("  开启方式：SET GLOBAL slow_query_log = ON;");
        System.out.println();

        for (SlowQueryLog log : logs) {
            boolean isSlow = log.queryTime > 1.0;
            String icon = isSlow ? "✗ 慢查询" : "✓ 正常";
            System.out.printf("  %s [%.3fs] 扫描:%d行 返回:%d行%n",
                    icon, log.queryTime, log.rowsExamined, log.rowsSent);
            System.out.printf("    SQL: %s%n", log.sql);

            if (isSlow) {
                // 分析慢查询原因
                double ratio = (double) log.rowsSent / log.rowsExamined;
                if (ratio < 0.01) {
                    System.out.printf("    诊断: 扫描/返回比 = %.4f（极低），大量无效扫描，建议添加索引%n", ratio);
                }
                if (log.sql.contains("DATE(") || log.sql.contains("UPPER(")) {
                    System.out.println("    诊断: 对索引列使用函数，导致索引失效");
                }
                if (log.sql.contains("SELECT *")) {
                    System.out.println("    诊断: SELECT * 返回所有列，无法使用覆盖索引");
                }
            }
        }
        System.out.println();
    }

    /**
     * 演示2：EXPLAIN 执行计划详解
     */
    static void demoExplainAnalysis() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：EXPLAIN 执行计划详解");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟不同查询的 EXPLAIN 结果
        ExplainRow[] explains = {
                // 主键查询 → const
                new ExplainRow(1, "SIMPLE", "user", "const",
                        "PRIMARY", "PRIMARY", 4, "const",
                        1, 100.0, ""),
                // 唯一索引查询 → const
                new ExplainRow(1, "SIMPLE", "user", "const",
                        "uk_email", "uk_email", 402, "const",
                        1, 100.0, ""),
                // 普通索引查询 → ref
                new ExplainRow(1, "SIMPLE", "user", "ref",
                        "idx_city", "idx_city", 202, "const",
                        150, 100.0, ""),
                // 范围查询 → range
                new ExplainRow(1, "SIMPLE", "order", "range",
                        "idx_amount", "idx_amount", 8, null,
                        5000, 100.0, "Using where; Using index"),
                // 无索引 → ALL（全表扫描）
                new ExplainRow(1, "SIMPLE", "order", "ALL",
                        null, null, 0, null,
                        1000000, 10.0, "Using where"),
        };

        String[] sqls = {
                "SELECT * FROM user WHERE id = 1",
                "SELECT * FROM user WHERE email = 'test@example.com'",
                "SELECT * FROM user WHERE city = '北京'",
                "SELECT amount FROM orders WHERE amount > 100",
                "SELECT * FROM orders WHERE status = 'pending'（status 无索引）",
        };

        System.out.println();
        for (int i = 0; i < explains.length; i++) {
            ExplainRow e = explains[i];
            System.out.printf("  【查询%d】%s%n", i + 1, sqls[i]);
            System.out.printf("    type=%-8s key=%-12s rows=%-10d extra=%s%n",
                    e.type, e.key == null ? "NULL" : e.key, e.rows,
                    e.extra.isEmpty() ? "(无)" : e.extra);

            // 给出优化建议
            switch (e.type) {
                case "const":
                    System.out.println("    评价: ★★★★★ 最优，主键/唯一索引等值查询");
                    break;
                case "ref":
                    System.out.println("    评价: ★★★★☆ 良好，普通索引等值查询");
                    break;
                case "range":
                    System.out.println("    评价: ★★★☆☆ 一般，索引范围扫描");
                    break;
                case "ALL":
                    System.out.println("    评价: ★☆☆☆☆ 最差，全表扫描！需要优化");
                    System.out.println("    建议: 给 status 列添加索引 → ALTER TABLE orders ADD INDEX idx_status(status)");
                    break;
            }
        }

        // Extra 字段含义
        System.out.println("\n  EXPLAIN Extra 字段常见值：");
        String[][] extras = {
                {"Using index", "覆盖索引，无需回表（好）"},
                {"Using where", "在存储引擎返回后还需要 Server 层过滤"},
                {"Using filesort", "需要额外排序，未利用索引排序（需优化）"},
                {"Using temporary", "使用临时表（常见于 GROUP BY/DISTINCT，需优化）"},
                {"Using index condition", "索引下推 ICP，在存储引擎层过滤（好）"},
        };
        for (String[] extra : extras) {
            System.out.printf("    %-25s → %s%n", extra[0], extra[1]);
        }
        System.out.println();
    }

    /**
     * 演示3：查询优化器成本估算
     */
    static void demoCostEstimation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：查询优化器成本估算");
        System.out.println("═══════════════════════════════════════════════════");

        long totalRows = 1000000;

        System.out.println("\n  表 orders 共 1,000,000 行");
        System.out.println("  查询: SELECT * FROM orders WHERE city = '北京'");
        System.out.println("  idx_city 索引预估匹配 50,000 行（5%）");

        // 方案1：全表扫描
        double fullScanCost = CostEstimator.estimateFullScanCost(totalRows);
        System.out.printf("\n  方案1 — 全表扫描:%n");
        System.out.printf("    I/O 成本: %.0f 页 × %.1f = %.1f%n",
                Math.ceil((double) totalRows / CostEstimator.ROWS_PER_PAGE),
                CostEstimator.IO_COST_PER_PAGE,
                Math.ceil((double) totalRows / CostEstimator.ROWS_PER_PAGE) * CostEstimator.IO_COST_PER_PAGE);
        System.out.printf("    CPU 成本: %d 行 × %.1f = %.1f%n",
                totalRows, CostEstimator.CPU_COST_PER_ROW, totalRows * CostEstimator.CPU_COST_PER_ROW);
        System.out.printf("    总成本: %.1f%n", fullScanCost);

        // 方案2：索引扫描 + 回表
        long matchedRows = 50000;
        double indexScanCost = CostEstimator.estimateIndexScanCost(matchedRows, totalRows);
        System.out.printf("\n  方案2 — 索引扫描 + 回表:%n");
        System.out.printf("    索引 I/O: %.0f 页%n", Math.ceil((double) matchedRows / CostEstimator.ROWS_PER_PAGE));
        System.out.printf("    回表 I/O: %d 次（最坏情况每行一次随机 I/O）%n", matchedRows);
        System.out.printf("    CPU 成本: %d 行 × %.1f = %.1f%n",
                matchedRows, CostEstimator.CPU_COST_PER_ROW, matchedRows * CostEstimator.CPU_COST_PER_ROW);
        System.out.printf("    总成本: %.1f%n", indexScanCost);

        // 方案3：覆盖索引
        double coveringCost = CostEstimator.estimateCoveringIndexCost(matchedRows);
        System.out.printf("\n  方案3 — 覆盖索引（SELECT city, age FROM orders WHERE city='北京'）:%n");
        System.out.printf("    总成本: %.1f（无回表，成本最低）%n", coveringCost);

        // 优化器选择
        System.out.println("\n  优化器选择：");
        if (fullScanCost < indexScanCost) {
            System.out.printf("    全表扫描(%.1f) < 索引扫描(%.1f) → 选择全表扫描%n", fullScanCost, indexScanCost);
            System.out.println("    原因：匹配行数占比较高时，回表的随机 I/O 成本超过顺序全表扫描");
        } else {
            System.out.printf("    索引扫描(%.1f) < 全表扫描(%.1f) → 选择索引扫描%n", indexScanCost, fullScanCost);
        }
        System.out.printf("    覆盖索引(%.1f) 始终是最优选择（如果查询字段都在索引中）%n", coveringCost);

        // 不同匹配比例下的选择
        System.out.println("\n  【不同匹配比例下优化器的选择】");
        System.out.printf("    %-10s %-15s %-15s %-10s%n", "匹配比例", "全表扫描成本", "索引扫描成本", "优化器选择");
        System.out.println("    " + "─".repeat(55));
        for (int pct : new int[]{1, 5, 10, 20, 30, 50}) {
            long matched = totalRows * pct / 100;
            double fsCost = CostEstimator.estimateFullScanCost(totalRows);
            double isCost = CostEstimator.estimateIndexScanCost(matched, totalRows);
            String choice = fsCost < isCost ? "全表扫描" : "索引扫描";
            System.out.printf("    %-10s %-15.1f %-15.1f %-10s%n",
                    pct + "%", fsCost, isCost, choice);
        }
        System.out.println("    结论：当匹配行数超过总行数的 ~20% 时，优化器倾向于全表扫描");
        System.out.println();
    }

    /**
     * 演示4：常见 SQL 优化技巧
     */
    static void demoOptimizationTips() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：常见 SQL 优化技巧");
        System.out.println("═══════════════════════════════════════════════════");

        // 用 Map 模拟一张表
        java.util.Map<Integer, String[]> table = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= 10; i++) {
            table.put(i, new String[]{"user" + i, String.valueOf(20 + i), "city" + (i % 3)});
        }

        System.out.println("\n  【技巧1】避免 SELECT *，只查需要的列");
        System.out.println("    ✗ SELECT * FROM user WHERE id = 1");
        System.out.println("    ✓ SELECT name, age FROM user WHERE id = 1");
        System.out.println("    原因：减少网络传输 + 可能命中覆盖索引");

        System.out.println("\n  【技巧2】避免对索引列使用函数");
        System.out.println("    ✗ WHERE DATE(create_time) = '2024-01-15'");
        System.out.println("    ✓ WHERE create_time >= '2024-01-15' AND create_time < '2024-01-16'");
        System.out.println("    原因：函数会破坏索引的有序性");

        System.out.println("\n  【技巧3】用 EXISTS 替代 IN（大子查询）");
        System.out.println("    ✗ SELECT * FROM orders WHERE user_id IN (SELECT id FROM users WHERE city='北京')");
        System.out.println("    ✓ SELECT * FROM orders o WHERE EXISTS (SELECT 1 FROM users u WHERE u.id=o.user_id AND u.city='北京')");
        System.out.println("    原因：EXISTS 在找到第一条匹配后就停止，IN 需要先执行完子查询");

        System.out.println("\n  【技巧4】分页优化 — 深分页问题");
        System.out.println("    ✗ SELECT * FROM orders LIMIT 1000000, 10（扫描 100 万行丢弃）");
        System.out.println("    ✓ SELECT * FROM orders WHERE id > 1000000 LIMIT 10（利用主键定位）");
        System.out.println("    ✓ SELECT * FROM orders o JOIN (SELECT id FROM orders LIMIT 1000000, 10) t ON o.id = t.id");
        System.out.println("    原因：延迟关联，子查询只扫描索引，减少回表");

        // 模拟深分页对比
        System.out.println("\n    【模拟】深分页性能对比：");
        java.util.List<Integer> bigTable = new java.util.ArrayList<>();
        for (int i = 1; i <= 100000; i++) bigTable.add(i);

        long start = System.nanoTime();
        // 方式1：LIMIT offset, count（需要跳过 offset 行）
        java.util.List<Integer> result1 = new java.util.ArrayList<>();
        int skip = 90000, count = 10;
        for (int i = skip; i < Math.min(skip + count, bigTable.size()); i++) {
            result1.add(bigTable.get(i));
        }
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        // 方式2：WHERE id > lastId LIMIT count（直接定位）
        int lastId = 90000;
        java.util.List<Integer> result2 = new java.util.ArrayList<>();
        int found = 0;
        for (int val : bigTable) {
            if (val > lastId && found < count) {
                result2.add(val);
                found++;
            }
        }
        long time2 = System.nanoTime() - start;

        System.out.printf("      LIMIT %d, %d: %d ns%n", skip, count, time1);
        System.out.printf("      WHERE id > %d LIMIT %d: %d ns%n", lastId, count, time2);

        System.out.println("\n  【技巧5】批量操作替代循环单条");
        System.out.println("    ✗ 循环执行 INSERT INTO user VALUES (1,'a'), (2,'b'), ...");
        System.out.println("    ✓ INSERT INTO user VALUES (1,'a'), (2,'b'), (3,'c'), ...（批量插入）");
        System.out.println("    原因：减少网络往返和事务提交次数");

        System.out.println("\n  【技巧6】合理使用 FORCE INDEX");
        System.out.println("    当优化器选错索引时：SELECT * FROM orders FORCE INDEX(idx_city) WHERE city='北京'");
        System.out.println("    注意：优先分析为什么优化器选错，而不是直接 FORCE INDEX");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MySQL SQL 优化演示（混合模式）                        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟查询优化器行为 ══════════");
        System.out.println();
        demoSlowQueryAnalysis();
        demoExplainAnalysis();
        demoCostEstimation();
        demoOptimizationTips();

        // ===== Part B：连接真实 MySQL =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：连接真实 MySQL 执行 EXPLAIN ══════════");
            System.out.println();
            RealMySQLExplain.run();
        } else {
            System.out.println("提示：运行 Part B（真实 EXPLAIN）请传入参数 'real'");
            System.out.println("  java -cp ... com.example.database.optimization.OptimizationDemo real");
        }
    }

    // ==================== Part B：真实 MySQL EXPLAIN ====================

    /**
     * Part B：用 JDBC 连接真实 MySQL，执行 EXPLAIN 并解读结果。
     * 需要先启动 MySQL：docker compose -f docker/docker-compose.yml up -d mysql
     * 默认连接：localhost:3306，用户 root，密码 root123
     */
    static class RealMySQLExplain {

        static final String URL = "jdbc:mysql://localhost:3306/demo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        static final String USER = "root";
        static final String PASSWORD = "root123";

        static void run() throws Exception {
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASSWORD)) {
                // 1. 创建测试表和数据
                prepareTestData(conn);

                // 2. 执行各种 EXPLAIN
                explainPrimaryKey(conn);
                explainSecondaryIndex(conn);
                explainFullScan(conn);
                explainCoveringIndex(conn);

                // 3. 清理测试数据（可注释掉以保留数据，方便用 Navicat 等工具查看）
                // conn.createStatement().execute("DROP TABLE IF EXISTS demo_user");
                // System.out.println("  清理：已删除测试表 demo_user");
                cleanup(conn);
            }
        }

        /**
         * 清理测试表。如需保留数据用 Navicat 等工具查看，注释掉 cleanup() 调用即可
         */
        static void cleanup(java.sql.Connection conn) throws Exception {
            conn.createStatement().execute("DROP TABLE IF EXISTS demo_user");
            System.out.println("  清理：已删除测试表 demo_user");
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }

    static void prepareTestData(java.sql.Connection conn) throws Exception {
        java.sql.Statement stmt = conn.createStatement();
        stmt.execute("CREATE DATABASE IF NOT EXISTS demo_db");
        stmt.execute("USE demo_db");
        stmt.execute("DROP TABLE IF EXISTS demo_user");
        stmt.execute("CREATE TABLE demo_user (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(50)," +
                "age INT," +
                "city VARCHAR(50)," +
                "email VARCHAR(100)," +
                "INDEX idx_name(name)," +
                "INDEX idx_city_age(city, age)" +
                ") ENGINE=InnoDB");

        // 插入测试数据
        java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO demo_user(name, age, city, email) VALUES(?,?,?,?)");
        String[] cities = {"北京", "上海", "广州", "深圳"};
        for (int i = 1; i <= 1000; i++) {
            ps.setString(1, "用户" + i);
            ps.setInt(2, 20 + (i % 30));
            ps.setString(3, cities[i % 4]);
            ps.setString(4, "user" + i + "@example.com");
            ps.addBatch();
            if (i % 200 == 0) ps.executeBatch();
        }
        ps.executeBatch();
        System.out.println("  准备数据：创建 demo_user 表，插入 1000 行");
        System.out.println();
    }

    /**
     * 主键查询 EXPLAIN
     */
    static void explainPrimaryKey(java.sql.Connection conn) throws Exception {
        System.out.println("  【EXPLAIN 1】主键查询");
        printExplain(conn, "EXPLAIN SELECT * FROM demo_user WHERE id = 500");
    }

    /**
     * 二级索引查询 EXPLAIN
     */
    static void explainSecondaryIndex(java.sql.Connection conn) throws Exception {
        System.out.println("  【EXPLAIN 2】二级索引查询");
        printExplain(conn, "EXPLAIN SELECT * FROM demo_user WHERE name = '用户100'");
    }

    /**
     * 全表扫描 EXPLAIN
     */
    static void explainFullScan(java.sql.Connection conn) throws Exception {
        System.out.println("  【EXPLAIN 3】无索引列查询（全表扫描）");
        printExplain(conn, "EXPLAIN SELECT * FROM demo_user WHERE email = 'user100@example.com'");
    }

    /**
     * 覆盖索引 EXPLAIN
     */
    static void explainCoveringIndex(java.sql.Connection conn) throws Exception {
        System.out.println("  【EXPLAIN 4】覆盖索引（Using index）");
        printExplain(conn, "EXPLAIN SELECT city, age FROM demo_user WHERE city = '北京'");
    }

    /**
     * 执行 EXPLAIN 并格式化输出
     */
    static void printExplain(java.sql.Connection conn, String sql) throws Exception {
        System.out.println("    SQL: " + sql.replace("EXPLAIN ", ""));
        java.sql.ResultSet rs = conn.createStatement().executeQuery(sql);
        java.sql.ResultSetMetaData meta = rs.getMetaData();

        while (rs.next()) {
            System.out.printf("    type=%-8s key=%-15s rows=%-6s Extra=%s%n",
                    rs.getString("type"),
                    rs.getString("key") == null ? "NULL" : rs.getString("key"),
                    rs.getString("rows"),
                    rs.getString("Extra") == null ? "(无)" : rs.getString("Extra"));
        }
        rs.close();
        System.out.println();
    }
}

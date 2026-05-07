package com.example.database.index_demo;

/**
 * MySQL 索引原理演示 — 用纯 Java 模拟 B+树索引的核心行为
 *
 * <p>本示例用 TreeMap 模拟 B+树有序索引，演示以下核心概念：
 * <ul>
 *   <li>主键索引（聚簇索引）vs 二级索引（非聚簇索引）+ 回表查询</li>
 *   <li>联合索引 + 最左前缀匹配原则</li>
 *   <li>覆盖索引（避免回表）</li>
 *   <li>索引失效的常见场景</li>
 *   <li>范围查询在索引上的行为</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>B+树索引结构（简化）：</h3>
 * <pre>
 *  聚簇索引（主键）：叶子节点存储完整行数据
 *  ┌─────────────────────────────────────────┐
 *  │  PK=1 → {id=1, name="张三", age=25, city="北京"}  │
 *  │  PK=2 → {id=2, name="李四", age=30, city="上海"}  │
 *  │  PK=3 → {id=3, name="王五", age=25, city="北京"}  │
 *  └─────────────────────────────────────────┘
 *
 *  二级索引（name）：叶子节点存储主键值，需要回表
 *  ┌──────────────────────────┐
 *  │  "张三" → PK=1           │
 *  │  "李四" → PK=2           │
 *  │  "王五" → PK=3           │
 *  └──────────────────────────┘
 * </pre>
 */
public class IndexDemo {

    // ==================== 数据行模型 ====================

    /** 模拟一行数据记录，对应 MySQL 表中的一行 */
    static class Row {
        final int id;
        final String name;
        final int age;
        final String city;
        final String email;

        Row(int id, String name, int age, String city, String email) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.city = city;
            this.email = email;
        }

        @Override
        public String toString() {
            return String.format("{id=%d, name=%s, age=%d, city=%s, email=%s}",
                    id, name, age, city, email);
        }
    }

    // ==================== 联合索引键 ====================

    /**
     * 联合索引键，模拟 INDEX(city, age, name) 的排序规则。
     * MySQL 联合索引按字段声明顺序排序：先按 city，city 相同按 age，age 相同按 name。
     */
    static class CompositeKey implements Comparable<CompositeKey> {
        final String city;
        final int age;
        final String name;

        CompositeKey(String city, int age, String name) {
            this.city = city;
            this.age = age;
            this.name = name;
        }

        @Override
        public int compareTo(CompositeKey o) {
            // 最左前缀排序：city → age → name
            int c = this.city.compareTo(o.city);
            if (c != 0) return c;
            c = Integer.compare(this.age, o.age);
            if (c != 0) return c;
            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return String.format("(%s, %d, %s)", city, age, name);
        }
    }

    // ==================== 模拟数据库表 ====================

    /**
     * 模拟一张 MySQL 表，包含聚簇索引、二级索引、联合索引。
     *
     * <p>对应 SQL：
     * <pre>
     * CREATE TABLE user (
     *     id INT PRIMARY KEY,          -- 聚簇索引
     *     name VARCHAR(50),
     *     age INT,
     *     city VARCHAR(50),
     *     email VARCHAR(100),
     *     INDEX idx_name(name),         -- 二级索引
     *     INDEX idx_city_age_name(city, age, name)  -- 联合索引
     * );
     * </pre>
     */
    static class SimulatedTable {
        // 聚簇索引：主键 → 完整行数据（InnoDB 中数据就存在主键 B+树的叶子节点）
        private final java.util.TreeMap<Integer, Row> clusteredIndex = new java.util.TreeMap<>();

        // 二级索引：name → 主键列表（二级索引叶子节点只存主键值）
        private final java.util.TreeMap<String, java.util.List<Integer>> nameIndex = new java.util.TreeMap<>();

        // 联合索引：(city, age, name) → 主键列表
        private final java.util.TreeMap<CompositeKey, java.util.List<Integer>> compositeIndex = new java.util.TreeMap<>();

        /** 插入数据：同时维护三个索引 */
        void insert(Row row) {
            // 聚簇索引
            clusteredIndex.put(row.id, row);

            // 二级索引 idx_name
            nameIndex.computeIfAbsent(row.name, k -> new java.util.ArrayList<>()).add(row.id);

            // 联合索引 idx_city_age_name
            CompositeKey ck = new CompositeKey(row.city, row.age, row.name);
            compositeIndex.computeIfAbsent(ck, k -> new java.util.ArrayList<>()).add(row.id);
        }

        // ---------- 查询方式演示 ----------

        /**
         * 主键查询：直接在聚簇索引上定位，O(log n)，无需回表。
         * 对应 SQL: SELECT * FROM user WHERE id = ?
         */
        Row queryByPrimaryKey(int id) {
            return clusteredIndex.get(id);
        }

        /**
         * 二级索引查询 + 回表。
         * 步骤：1) 在 name 索引中找到主键  2) 用主键回聚簇索引取完整行
         * 对应 SQL: SELECT * FROM user WHERE name = '张三'
         *
         * @return 查询结果和回表次数
         */
        QueryResult queryByNameWithBackToTable(String name) {
            int backToTableCount = 0;
            java.util.List<Row> results = new java.util.ArrayList<>();

            // 步骤1：在二级索引中查找 → 得到主键列表
            java.util.List<Integer> pks = nameIndex.get(name);
            if (pks != null) {
                for (int pk : pks) {
                    // 步骤2：回表 — 用主键去聚簇索引取完整行数据
                    Row row = clusteredIndex.get(pk);
                    if (row != null) {
                        results.add(row);
                        backToTableCount++;
                    }
                }
            }
            return new QueryResult(results, backToTableCount, false);
        }

        /**
         * 覆盖索引查询：查询的字段全部在索引中，无需回表。
         * 联合索引 (city, age, name) 已经包含了 city 和 age，直接从索引返回。
         * 对应 SQL: SELECT city, age FROM user WHERE city = '北京'
         * EXPLAIN 中会显示 Using index（覆盖索引标志）
         */
        QueryResult queryWithCoveringIndex(String city) {
            java.util.List<Row> results = new java.util.ArrayList<>();

            // 利用 TreeMap 的 subMap 模拟范围扫描
            CompositeKey from = new CompositeKey(city, Integer.MIN_VALUE, "");
            CompositeKey to = new CompositeKey(city, Integer.MAX_VALUE, "\uffff");

            // 直接从联合索引中获取数据，不需要回表
            for (java.util.Map.Entry<CompositeKey, java.util.List<Integer>> entry :
                    compositeIndex.subMap(from, true, to, true).entrySet()) {
                CompositeKey key = entry.getKey();
                for (int pk : entry.getValue()) {
                    // 覆盖索引：只需要 city 和 age，索引键中已包含，构造部分结果
                    results.add(new Row(pk, key.name, key.age, key.city, "N/A(覆盖索引)"));
                }
            }
            return new QueryResult(results, 0, true);
        }

        /**
         * 最左前缀匹配演示。
         * 联合索引 (city, age, name) 支持以下查询：
         *   ✓ WHERE city = ?                    → 使用索引（最左第一列）
         *   ✓ WHERE city = ? AND age = ?        → 使用索引（最左两列）
         *   ✓ WHERE city = ? AND age = ? AND name = ?  → 使用索引（全部三列）
         *   ✗ WHERE age = ?                     → 不使用索引（跳过了最左列 city）
         *   ✗ WHERE name = ?                    → 不使用索引（跳过了最左列 city）
         *   △ WHERE city = ? AND name = ?       → 只用 city 列，name 无法利用索引
         */
        java.util.List<LeftmostPrefixResult> demonstrateLeftmostPrefix() {
            java.util.List<LeftmostPrefixResult> results = new java.util.ArrayList<>();

            // 场景1：WHERE city = '北京' → 命中最左前缀
            results.add(new LeftmostPrefixResult(
                    "WHERE city = '北京'",
                    true, "命中索引（最左第一列 city）",
                    queryCompositeByCity("北京").size()));

            // 场景2：WHERE city = '北京' AND age = 25 → 命中最左两列
            results.add(new LeftmostPrefixResult(
                    "WHERE city = '北京' AND age = 25",
                    true, "命中索引（最左两列 city + age）",
                    queryCompositeByCityAndAge("北京", 25).size()));

            // 场景3：WHERE age = 25 → 跳过最左列，索引失效
            results.add(new LeftmostPrefixResult(
                    "WHERE age = 25",
                    false, "索引失效（跳过最左列 city，需全表扫描）",
                    queryByAgeFullScan(25).size()));

            // 场景4：WHERE city = '北京' AND name = '张三' → 只用 city，name 无法连续匹配
            results.add(new LeftmostPrefixResult(
                    "WHERE city = '北京' AND name = '张三'",
                    false, "部分命中（只用 city 列，中间跳过 age，name 无法利用索引）",
                    queryCompositeByCityAndName("北京", "张三").size()));

            return results;
        }

        // ---------- 辅助查询方法 ----------

        /** 联合索引查询：只按 city */
        private java.util.List<Integer> queryCompositeByCity(String city) {
            java.util.List<Integer> result = new java.util.ArrayList<>();
            CompositeKey from = new CompositeKey(city, Integer.MIN_VALUE, "");
            CompositeKey to = new CompositeKey(city, Integer.MAX_VALUE, "\uffff");
            for (java.util.List<Integer> pks : compositeIndex.subMap(from, true, to, true).values()) {
                result.addAll(pks);
            }
            return result;
        }

        /** 联合索引查询：按 city + age */
        private java.util.List<Integer> queryCompositeByCityAndAge(String city, int age) {
            java.util.List<Integer> result = new java.util.ArrayList<>();
            CompositeKey from = new CompositeKey(city, age, "");
            CompositeKey to = new CompositeKey(city, age, "\uffff");
            for (java.util.List<Integer> pks : compositeIndex.subMap(from, true, to, true).values()) {
                result.addAll(pks);
            }
            return result;
        }

        /** 全表扫描：按 age 查询（联合索引无法使用，因为跳过了最左列） */
        private java.util.List<Integer> queryByAgeFullScan(int age) {
            java.util.List<Integer> result = new java.util.ArrayList<>();
            for (Row row : clusteredIndex.values()) {
                if (row.age == age) {
                    result.add(row.id);
                }
            }
            return result;
        }

        /** 联合索引部分命中：city + name（跳过 age） */
        private java.util.List<Integer> queryCompositeByCityAndName(String city, String name) {
            java.util.List<Integer> result = new java.util.ArrayList<>();
            // 只能用 city 做索引范围扫描，name 需要逐行过滤
            CompositeKey from = new CompositeKey(city, Integer.MIN_VALUE, "");
            CompositeKey to = new CompositeKey(city, Integer.MAX_VALUE, "\uffff");
            for (java.util.Map.Entry<CompositeKey, java.util.List<Integer>> entry :
                    compositeIndex.subMap(from, true, to, true).entrySet()) {
                if (entry.getKey().name.equals(name)) {
                    result.addAll(entry.getValue());
                }
            }
            return result;
        }

        /**
         * 范围查询演示：WHERE city = '北京' AND age > 20 AND age < 35
         * 联合索引可以高效处理范围查询，但范围条件之后的列无法使用索引
         */
        java.util.List<Row> rangeQuery(String city, int ageFrom, int ageTo) {
            java.util.List<Row> results = new java.util.ArrayList<>();
            CompositeKey from = new CompositeKey(city, ageFrom, "");
            CompositeKey to = new CompositeKey(city, ageTo, "\uffff");
            for (java.util.Map.Entry<CompositeKey, java.util.List<Integer>> entry :
                    compositeIndex.subMap(from, false, to, false).entrySet()) {
                for (int pk : entry.getValue()) {
                    results.add(clusteredIndex.get(pk));
                }
            }
            return results;
        }

        int totalRows() {
            return clusteredIndex.size();
        }
    }

    // ==================== 查询结果封装 ====================

    static class QueryResult {
        final java.util.List<Row> rows;
        final int backToTableCount;
        final boolean coveringIndex;

        QueryResult(java.util.List<Row> rows, int backToTableCount, boolean coveringIndex) {
            this.rows = rows;
            this.backToTableCount = backToTableCount;
            this.coveringIndex = coveringIndex;
        }
    }

    static class LeftmostPrefixResult {
        final String sql;
        final boolean indexUsed;
        final String explanation;
        final int matchCount;

        LeftmostPrefixResult(String sql, boolean indexUsed, String explanation, int matchCount) {
            this.sql = sql;
            this.indexUsed = indexUsed;
            this.explanation = explanation;
            this.matchCount = matchCount;
        }
    }

    // ==================== 演示方法 ====================

    /** 初始化测试数据 */
    static SimulatedTable prepareData() {
        SimulatedTable table = new SimulatedTable();
        table.insert(new Row(1,  "张三", 25, "北京", "zhangsan@example.com"));
        table.insert(new Row(2,  "李四", 30, "上海", "lisi@example.com"));
        table.insert(new Row(3,  "王五", 25, "北京", "wangwu@example.com"));
        table.insert(new Row(4,  "赵六", 28, "广州", "zhaoliu@example.com"));
        table.insert(new Row(5,  "孙七", 35, "上海", "sunqi@example.com"));
        table.insert(new Row(6,  "周八", 22, "北京", "zhouba@example.com"));
        table.insert(new Row(7,  "吴九", 30, "深圳", "wujiu@example.com"));
        table.insert(new Row(8,  "郑十", 27, "北京", "zhengshi@example.com"));
        table.insert(new Row(9,  "张三", 32, "上海", "zhangsan2@example.com"));
        table.insert(new Row(10, "钱十一", 25, "广州", "qian11@example.com"));
        return table;
    }

    /** 演示1：主键查询 vs 二级索引查询（回表） */
    static void demoPrimaryKeyVsSecondaryIndex() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：主键查询 vs 二级索引查询（回表）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedTable table = prepareData();

        // 主键查询：直接定位，无回表
        System.out.println("\n【主键查询】SELECT * FROM user WHERE id = 3");
        Row row = table.queryByPrimaryKey(3);
        System.out.println("  结果: " + row);
        System.out.println("  回表次数: 0（聚簇索引直接包含完整行数据）");

        // 二级索引查询：先查索引得到主键，再回表取完整数据
        System.out.println("\n【二级索引查询】SELECT * FROM user WHERE name = '张三'");
        QueryResult qr = table.queryByNameWithBackToTable("张三");
        System.out.println("  结果:");
        for (Row r : qr.rows) {
            System.out.println("    " + r);
        }
        System.out.printf("  回表次数: %d（每个匹配的主键都要回聚簇索引取一次完整行）%n", qr.backToTableCount);
        System.out.println();
    }

    /** 演示2：覆盖索引（避免回表） */
    static void demoCoveringIndex() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：覆盖索引 — 查询字段全在索引中，无需回表");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedTable table = prepareData();

        // 需要回表的查询
        System.out.println("\n【需要回表】SELECT * FROM user WHERE name = '张三'");
        QueryResult qr1 = table.queryByNameWithBackToTable("张三");
        System.out.printf("  回表次数: %d，覆盖索引: %s%n", qr1.backToTableCount, qr1.coveringIndex ? "是" : "否");

        // 覆盖索引查询：SELECT city, age FROM user WHERE city = '北京'
        // 联合索引 (city, age, name) 已包含 city 和 age，无需回表
        System.out.println("\n【覆盖索引】SELECT city, age, name FROM user WHERE city = '北京'");
        System.out.println("  索引 idx_city_age_name(city, age, name) 已包含所有查询字段");
        QueryResult qr2 = table.queryWithCoveringIndex("北京");
        System.out.println("  结果:");
        for (Row r : qr2.rows) {
            System.out.printf("    city=%s, age=%d, name=%s%n", r.city, r.age, r.name);
        }
        System.out.printf("  回表次数: %d，覆盖索引: %s（EXPLAIN 显示 Using index）%n",
                qr2.backToTableCount, qr2.coveringIndex ? "是" : "否");
        System.out.println();
    }

    /** 演示3：最左前缀匹配原则 */
    static void demoLeftmostPrefix() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：最左前缀匹配原则 — 联合索引 (city, age, name)");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedTable table = prepareData();
        java.util.List<LeftmostPrefixResult> results = table.demonstrateLeftmostPrefix();

        System.out.println();
        for (LeftmostPrefixResult r : results) {
            String icon = r.indexUsed ? "✓" : "✗";
            System.out.printf("  %s %s%n", icon, r.sql);
            System.out.printf("    → %s（匹配 %d 行）%n", r.explanation, r.matchCount);
        }

        System.out.println("\n  总结：联合索引必须从最左列开始连续使用，中间不能跳列");
        System.out.println("  口诀：带头大哥不能死，中间兄弟不能断");
        System.out.println();
    }

    /** 演示4：范围查询对索引的影响 */
    static void demoRangeQuery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：范围查询对联合索引的影响");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedTable table = prepareData();

        // 范围查询：WHERE city = '北京' AND age > 20 AND age < 30
        System.out.println("\n  SELECT * FROM user WHERE city = '北京' AND age > 20 AND age < 30");
        System.out.println("  索引使用情况：city 列精确匹配 + age 列范围扫描");
        System.out.println("  注意：范围条件（>、<、BETWEEN）之后的列（name）无法使用索引");

        java.util.List<Row> results = table.rangeQuery("北京", 20, 30);
        System.out.println("  结果:");
        for (Row r : results) {
            System.out.println("    " + r);
        }
        System.out.printf("  匹配行数: %d / 总行数: %d%n", results.size(), table.totalRows());
        System.out.println();
    }

    /** 演示5：索引失效的常见场景 */
    static void demoIndexInvalidation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：索引失效的常见场景");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedTable table = prepareData();

        System.out.println();
        // 场景1：对索引列使用函数
        System.out.println("  ✗ 场景1：对索引列使用函数");
        System.out.println("    WHERE UPPER(name) = '张三'  → 索引失效，全表扫描");
        System.out.println("    原因：函数改变了索引列的值，B+树无法定位");
        System.out.println("    优化：WHERE name = '张三'（避免对索引列使用函数）");

        // 场景2：隐式类型转换
        System.out.println("\n  ✗ 场景2：隐式类型转换");
        System.out.println("    WHERE name = 123  → 索引失效（name 是 VARCHAR，123 是 INT）");
        System.out.println("    原因：MySQL 会对 name 列做 CAST(name AS SIGNED)，等同于对列使用函数");
        System.out.println("    优化：WHERE name = '123'（保持类型一致）");

        // 场景3：LIKE 以通配符开头
        System.out.println("\n  ✗ 场景3：LIKE 以通配符开头");
        System.out.println("    WHERE name LIKE '%三'  → 索引失效");
        System.out.println("    WHERE name LIKE '张%'  → 索引有效（前缀匹配）");
        System.out.println("    原因：B+树按前缀排序，'%三' 无法确定扫描起点");

        // 场景4：OR 条件中有非索引列
        System.out.println("\n  ✗ 场景4：OR 条件中有非索引列");
        System.out.println("    WHERE name = '张三' OR email = 'test@example.com'");
        System.out.println("    → 如果 email 没有索引，整个查询退化为全表扫描");
        System.out.println("    优化：给 email 也建索引，或改用 UNION");

        // 场景5：NOT NULL / IS NULL
        System.out.println("\n  △ 场景5：IS NULL / IS NOT NULL");
        System.out.println("    WHERE name IS NOT NULL  → 可能索引失效（取决于数据分布）");
        System.out.println("    原因：如果大部分行都不为 NULL，优化器认为全表扫描更快");

        // 用代码模拟全表扫描 vs 索引扫描的对比
        System.out.println("\n  【模拟对比】全表扫描 vs 索引扫描：");
        long start = System.nanoTime();
        int fullScanCount = 0;
        for (Row row : table.clusteredIndex.values()) {
            if (row.name.equals("张三")) fullScanCount++;
        }
        long fullScanTime = System.nanoTime() - start;

        start = System.nanoTime();
        QueryResult indexResult = table.queryByNameWithBackToTable("张三");
        long indexTime = System.nanoTime() - start;

        System.out.printf("    全表扫描: 遍历 %d 行，找到 %d 条，耗时 %d ns%n",
                table.totalRows(), fullScanCount, fullScanTime);
        System.out.printf("    索引查询: 直接定位，找到 %d 条，耗时 %d ns%n",
                indexResult.rows.size(), indexTime);
        System.out.println("    注意：小数据量下差异不明显，数据量越大索引优势越显著");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MySQL 索引原理演示 — B+树索引行为模拟（纯内存）       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoPrimaryKeyVsSecondaryIndex();
        demoCoveringIndex();
        demoLeftmostPrefix();
        demoRangeQuery();
        demoIndexInvalidation();
    }
}

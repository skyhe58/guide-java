package com.example.database.id;

/**
 * 分布式 ID 生成方案演示 — 实现 UUID/自增/雪花/号段四种方案对比
 *
 * <p>本示例用纯 Java 实现四种常见的分布式 ID 生成方案：
 * <ul>
 *   <li>UUID — 简单但无序，不适合做主键</li>
 *   <li>数据库自增 — 简单有序，但有单点瓶颈</li>
 *   <li>雪花算法（Snowflake）— 趋势递增，高性能，业界主流</li>
 *   <li>号段模式（Leaf-Segment）— 批量获取，减少数据库压力</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>雪花算法 ID 结构（64 bit）：</h3>
 * <pre>
 *  0 | 0000000000 0000000000 0000000000 0000000000 0 | 00000 | 00000 | 000000000000
 *  ↑ |←─────────── 41 bit 时间戳 ──────────────────→|←5bit→|←5bit→|←── 12 bit ──→|
 *  符号位          毫秒级时间差                        数据中心  机器ID    序列号
 *
 *  - 时间戳：41 bit，可用约 69 年
 *  - 数据中心 ID：5 bit，最多 32 个数据中心
 *  - 机器 ID：5 bit，每个数据中心最多 32 台机器
 *  - 序列号：12 bit，每毫秒最多 4096 个 ID
 *  - 理论 QPS：4096 × 1000 = 409.6 万/秒
 * </pre>
 */
public class DistributedIdDemo {

    // ==================== 方案1：UUID ====================

    /**
     * UUID 生成器。
     * 优点：简单，无需中心化服务
     * 缺点：无序（B+树频繁页分裂）、字符串占用空间大（36字符）、不可读
     */
    static class UUIDGenerator {
        static String generate() {
            return java.util.UUID.randomUUID().toString();
        }

        /** 去掉横线的紧凑格式 */
        static String generateCompact() {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    // ==================== 方案2：数据库自增 ====================

    /**
     * 模拟数据库自增 ID。
     * 优点：简单、有序、可读
     * 缺点：单点瓶颈、分库分表后 ID 冲突、暴露业务量
     */
    static class DatabaseAutoIncrement {
        private final java.util.concurrent.atomic.AtomicLong counter;
        private final long step; // 步长（多库时每个库步长不同）

        DatabaseAutoIncrement(long startId, long step) {
            this.counter = new java.util.concurrent.atomic.AtomicLong(startId);
            this.step = step;
        }

        long nextId() {
            return counter.getAndAdd(step);
        }
    }

    // ==================== 方案3：雪花算法 ====================

    /**
     * 雪花算法（Snowflake）实现。
     * Twitter 开源的分布式 ID 生成算法，生成 64 bit 的 long 型 ID。
     */
    static class SnowflakeIdGenerator {
        // 起始时间戳（2024-01-01 00:00:00 UTC）
        private static final long EPOCH = 1704067200000L;

        // 各部分位数
        private static final long DATACENTER_BITS = 5L;
        private static final long WORKER_BITS = 5L;
        private static final long SEQUENCE_BITS = 12L;

        // 最大值
        private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_BITS); // 31
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_BITS);         // 31
        private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);         // 4095

        // 位移量
        private static final long WORKER_SHIFT = SEQUENCE_BITS;                           // 12
        private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;         // 17
        private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS; // 22

        private final long datacenterId;
        private final long workerId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;

        SnowflakeIdGenerator(long datacenterId, long workerId) {
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException("datacenterId 超出范围: " + datacenterId);
            }
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("workerId 超出范围: " + workerId);
            }
            this.datacenterId = datacenterId;
            this.workerId = workerId;
        }

        /** 生成下一个 ID（线程安全） */
        synchronized long nextId() {
            long timestamp = System.currentTimeMillis();

            // 时钟回拨检测
            if (timestamp < lastTimestamp) {
                throw new RuntimeException("时钟回拨！拒绝生成 ID，回拨时间: "
                        + (lastTimestamp - timestamp) + "ms");
            }

            if (timestamp == lastTimestamp) {
                // 同一毫秒内，序列号递增
                sequence = (sequence + 1) & MAX_SEQUENCE;
                if (sequence == 0) {
                    // 序列号溢出，等待下一毫秒
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                // 新的毫秒，序列号归零
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            // 组装 64 bit ID
            return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                    | (datacenterId << DATACENTER_SHIFT)
                    | (workerId << WORKER_SHIFT)
                    | sequence;
        }

        /** 解析 ID 中的各个字段 */
        static String parseId(long id) {
            long timestamp = (id >> TIMESTAMP_SHIFT) + EPOCH;
            long dcId = (id >> DATACENTER_SHIFT) & MAX_DATACENTER_ID;
            long wkId = (id >> WORKER_SHIFT) & MAX_WORKER_ID;
            long seq = id & MAX_SEQUENCE;

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return String.format("时间=%s, 数据中心=%d, 机器=%d, 序列=%d",
                    sdf.format(new java.util.Date(timestamp)), dcId, wkId, seq);
        }

        private long waitNextMillis(long lastTs) {
            long ts = System.currentTimeMillis();
            while (ts <= lastTs) {
                ts = System.currentTimeMillis();
            }
            return ts;
        }
    }

    // ==================== 方案4：号段模式 ====================

    /**
     * 号段模式（Leaf-Segment）实现。
     * 原理：每次从数据库批量获取一段 ID（如 1~1000），用完再取下一段。
     * 优点：减少数据库访问频率，支持高并发
     * 缺点：需要数据库支持，ID 不连续（服务重启会浪费号段）
     *
     * <pre>
     * 数据库表：
     * CREATE TABLE id_alloc (
     *     biz_tag VARCHAR(128) PRIMARY KEY,  -- 业务标识
     *     max_id BIGINT NOT NULL,            -- 当前最大 ID
     *     step INT NOT NULL,                 -- 号段步长
     *     update_time TIMESTAMP              -- 更新时间
     * );
     * </pre>
     */
    static class LeafSegmentGenerator {
        private final String bizTag;
        private final int step;

        // 模拟数据库中的 max_id
        private final java.util.concurrent.atomic.AtomicLong dbMaxId;

        // 当前号段
        private volatile long currentId;
        private volatile long maxId;
        private final Object lock = new Object();

        LeafSegmentGenerator(String bizTag, int step, long initMaxId) {
            this.bizTag = bizTag;
            this.step = step;
            this.dbMaxId = new java.util.concurrent.atomic.AtomicLong(initMaxId);
            // 初始化第一个号段
            loadNextSegment();
        }

        long nextId() {
            synchronized (lock) {
                if (currentId >= maxId) {
                    // 当前号段用完，加载下一段
                    loadNextSegment();
                }
                return currentId++;
            }
        }

        /** 模拟从数据库获取下一个号段 */
        private void loadNextSegment() {
            // 对应 SQL: UPDATE id_alloc SET max_id = max_id + step WHERE biz_tag = ?
            long newMaxId = dbMaxId.addAndGet(step);
            this.currentId = newMaxId - step;
            this.maxId = newMaxId;
        }

        String getStatus() {
            return String.format("[%s] 当前=%d, 号段上限=%d, 步长=%d",
                    bizTag, currentId, maxId, step);
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：四种方案生成 ID 对比 */
    static void demoIdGeneration() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：四种分布式 ID 方案对比");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【UUID】");
        for (int i = 0; i < 3; i++) {
            System.out.println("    " + UUIDGenerator.generate());
        }
        System.out.println("    特点：无序、36字符、不适合做 B+树主键");

        System.out.println("\n  【数据库自增】");
        DatabaseAutoIncrement dbGen = new DatabaseAutoIncrement(1, 1);
        for (int i = 0; i < 5; i++) {
            System.out.printf("    id = %d%n", dbGen.nextId());
        }
        System.out.println("    特点：有序、简单、单点瓶颈");

        System.out.println("\n  【雪花算法】datacenterId=1, workerId=1");
        SnowflakeIdGenerator snowflake = new SnowflakeIdGenerator(1, 1);
        for (int i = 0; i < 5; i++) {
            long id = snowflake.nextId();
            System.out.printf("    id = %d → %s%n", id, SnowflakeIdGenerator.parseId(id));
        }
        System.out.println("    特点：趋势递增、高性能、可反解时间和机器信息");

        System.out.println("\n  【号段模式】bizTag=order, step=1000");
        LeafSegmentGenerator leaf = new LeafSegmentGenerator("order", 1000, 0);
        System.out.println("    " + leaf.getStatus());
        for (int i = 0; i < 5; i++) {
            System.out.printf("    id = %d%n", leaf.nextId());
        }
        System.out.println("    " + leaf.getStatus());
        System.out.println("    特点：批量获取减少 DB 压力、ID 连续但可能有空洞");
        System.out.println();
    }

    /** 演示2：雪花算法性能测试 */
    static void demoSnowflakePerformance() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：雪花算法性能测试");
        System.out.println("═══════════════════════════════════════════════════");

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int count = 100000;

        // 单线程性能
        long start = System.nanoTime();
        java.util.Set<Long> ids = new java.util.HashSet<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(generator.nextId());
        }
        long elapsed = System.nanoTime() - start;

        System.out.printf("\n  生成 %d 个 ID:%n", count);
        System.out.printf("    耗时: %.2f ms%n", elapsed / 1_000_000.0);
        System.out.printf("    QPS: %.0f/s%n", count * 1_000_000_000.0 / elapsed);
        System.out.printf("    唯一性: %s（生成 %d 个，去重后 %d 个）%n",
                ids.size() == count ? "✓ 全部唯一" : "✗ 有重复", count, ids.size());

        // 验证趋势递增
        long prev = 0;
        boolean ordered = true;
        SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(1, 1);
        for (int i = 0; i < 1000; i++) {
            long id = gen2.nextId();
            if (id <= prev) { ordered = false; break; }
            prev = id;
        }
        System.out.printf("    趋势递增: %s%n", ordered ? "✓ 是" : "✗ 否");
        System.out.println();
    }

    /** 演示3：多库自增 ID 冲突与解决方案 */
    static void demoMultiDbAutoIncrement() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：多库自增 ID 冲突与解决方案");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【问题】两个数据库都从 1 开始自增 → ID 冲突");
        DatabaseAutoIncrement db1 = new DatabaseAutoIncrement(1, 1);
        DatabaseAutoIncrement db2 = new DatabaseAutoIncrement(1, 1);
        System.out.printf("    DB1: %d, %d, %d%n", db1.nextId(), db1.nextId(), db1.nextId());
        System.out.printf("    DB2: %d, %d, %d  ← ID 冲突！%n", db2.nextId(), db2.nextId(), db2.nextId());

        System.out.println("\n  【解决方案】设置不同的起始值和步长");
        System.out.println("    DB1: auto_increment_offset=1, auto_increment_increment=2（奇数）");
        System.out.println("    DB2: auto_increment_offset=2, auto_increment_increment=2（偶数）");
        DatabaseAutoIncrement db1Fixed = new DatabaseAutoIncrement(1, 2);
        DatabaseAutoIncrement db2Fixed = new DatabaseAutoIncrement(2, 2);
        System.out.printf("    DB1: %d, %d, %d（奇数）%n",
                db1Fixed.nextId(), db1Fixed.nextId(), db1Fixed.nextId());
        System.out.printf("    DB2: %d, %d, %d（偶数）%n",
                db2Fixed.nextId(), db2Fixed.nextId(), db2Fixed.nextId());
        System.out.println("    缺点：扩容到 3 个库时需要调整步长，不灵活");
        System.out.println();
    }

    /** 演示4：号段模式双 Buffer 优化 */
    static void demoDoubleBuffer() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：号段模式双 Buffer 优化");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  普通号段模式的问题：号段用完时需要同步等待数据库分配");
        System.out.println("  双 Buffer 优化：当前号段使用到一定比例时，异步加载下一个号段");

        LeafSegmentGenerator gen = new LeafSegmentGenerator("user", 100, 0);
        System.out.println("\n  模拟号段切换过程（step=100）：");
        for (int i = 0; i < 250; i++) {
            long id = gen.nextId();
            if (i % 100 == 0 || i % 100 == 99) {
                System.out.printf("    第 %d 个 ID = %d  %s%n", i + 1, id,
                        i % 100 == 99 ? "← 号段即将用完" : "← 新号段开始");
            }
        }

        System.out.println("\n  双 Buffer 工作流程：");
        System.out.println("    1. 初始加载号段 [0, 100) 到 Buffer A");
        System.out.println("    2. 当 Buffer A 使用到 70% 时，异步加载 [100, 200) 到 Buffer B");
        System.out.println("    3. Buffer A 用完后，无缝切换到 Buffer B");
        System.out.println("    4. 当 Buffer B 使用到 70% 时，异步加载 [200, 300) 到 Buffer A");
        System.out.println("    → 避免了号段切换时的同步等待");
        System.out.println();
    }

    /** 演示5：四种方案综合对比 */
    static void demoComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：四种方案综合对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"方案",       "有序性",    "性能",     "可用性",   "适用场景"},
                {"UUID",      "无序",     "★★★★★",  "★★★★★",  "不要求有序的场景（如 traceId）"},
                {"数据库自增",  "严格递增",  "★★☆☆☆",  "★★☆☆☆",  "单库小规模系统"},
                {"雪花算法",   "趋势递增",  "★★★★★",  "★★★★☆",  "分布式系统主流方案"},
                {"号段模式",   "趋势递增",  "★★★★☆",  "★★★★☆",  "美团 Leaf、滴滴 TinyId"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-10s %-10s %-10s %-10s %-30s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2],
                    comparison[i][3], comparison[i][4]);
            if (i == 0) {
                System.out.println("  " + "─".repeat(75));
            }
        }

        System.out.println("\n  推荐：");
        System.out.println("    中小项目 → 雪花算法（简单高效）");
        System.out.println("    大型项目 → 号段模式 + 双 Buffer（美团 Leaf）");
        System.out.println("    需要全局唯一但不要求有序 → UUID");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  分布式 ID 生成方案演示 — 四种方案对比（纯内存）        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoIdGeneration();
        demoSnowflakePerformance();
        demoMultiDbAutoIncrement();
        demoDoubleBuffer();
        demoComparison();
    }
}

package com.example.database.id;

/**
 * 雪花算法（Snowflake）ID 生成器
 *
 * <p>生成 64 位全局唯一、趋势递增的 long 类型 ID。</p>
 *
 * <p>64 位结构：</p>
 * <pre>
 * [0] [时间戳 41bit] [数据中心ID 5bit] [机器ID 5bit] [序列号 12bit]
 * </pre>
 *
 * <ul>
 *   <li>时间戳：毫秒级，可用约 69 年（从自定义 epoch 开始）</li>
 *   <li>数据中心 ID：0-31，最多 32 个数据中心</li>
 *   <li>机器 ID：0-31，每个数据中心最多 32 台机器</li>
 *   <li>序列号：0-4095，同一毫秒内最多生成 4096 个 ID</li>
 * </ul>
 *
 * @see <a href="https://github.com/twitter-archive/snowflake">Twitter Snowflake</a>
 */
public class SnowflakeIdGenerator {

    // 自定义 epoch：2024-01-01 00:00:00 UTC
    private static final long EPOCH = 1704067200000L;

    // 各部分占用的位数
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    // 最大值
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);       // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);          // 4095

    // 位移量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                              // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;         // 17
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造雪花 ID 生成器
     *
     * @param workerId     机器 ID（0-31）
     * @param datacenterId 数据中心 ID（0-31）
     * @throws IllegalArgumentException 如果 ID 超出范围
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("机器 ID 不能大于 %d 或小于 0，当前值: %d", MAX_WORKER_ID, workerId));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("数据中心 ID 不能大于 %d 或小于 0，当前值: %d", MAX_DATACENTER_ID, datacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个全局唯一 ID
     *
     * @return 64 位 long 类型 ID
     * @throws RuntimeException 如果检测到时钟回拨
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 回拨时间较短，等待追上
                try {
                    Thread.sleep(offset << 1);
                    timestamp = currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(
                                String.format("时钟回拨 %d 毫秒，拒绝生成 ID", lastTimestamp - timestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待时钟追上时被中断", e);
                }
            } else {
                throw new RuntimeException(
                        String.format("时钟回拨 %d 毫秒，拒绝生成 ID", offset));
            }
        }

        // 同一毫秒内，序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装 64 位 ID
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 获取机器 ID
     */
    public long getWorkerId() {
        return workerId;
    }

    /**
     * 获取数据中心 ID
     */
    public long getDatacenterId() {
        return datacenterId;
    }

    /**
     * 等待直到下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     * 独立方法便于测试时 mock
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}

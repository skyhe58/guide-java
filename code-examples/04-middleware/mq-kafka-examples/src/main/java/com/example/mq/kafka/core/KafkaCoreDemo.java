package com.example.mq.kafka.core;

/**
 * Kafka 核心概念演示（混合模式）
 *
 * <p>Part A：用 BlockingQueue + 分区数组模拟 Kafka 核心模型（直接运行）
 * <ul>
 *   <li>Topic / Partition / Offset 模型</li>
 *   <li>Producer 分区策略（轮询 / Key Hash / 自定义）</li>
 *   <li>Consumer / Consumer Group / Rebalance</li>
 *   <li>消息顺序性保证</li>
 * </ul>
 *
 * <p>Part B：用 kafka-clients 连接真实 Kafka 生产消费
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d kafka}
 *
 * <h3>Kafka 核心架构：</h3>
 * <pre>
 *  Producer → Topic（多个 Partition）→ Consumer Group
 *
 *  Topic: orders
 *  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
 *  │ Partition 0  │  │ Partition 1  │  │ Partition 2  │
 *  │ offset: 0→5  │  │ offset: 0→3  │  │ offset: 0→4  │
 *  └─────────────┘  └─────────────┘  └─────────────┘
 *       ↓                ↓                ↓
 *   Consumer-0       Consumer-1       Consumer-2
 *   └──────── Consumer Group: order-service ────────┘
 *
 *  关键概念：
 *  - 一个 Partition 只能被同一 Group 中的一个 Consumer 消费
 *  - 不同 Group 可以独立消费同一 Topic（发布订阅模式）
 *  - 消息在 Partition 内有序，跨 Partition 无序
 * </pre>
 */
public class KafkaCoreDemo {

    // ==================== Part A：内存模拟 Kafka ====================

    /** 模拟一条 Kafka 消息（ProducerRecord / ConsumerRecord） */
    static class Record {
        final String key;       // 消息键（决定分区路由）
        final String value;     // 消息值
        final long offset;      // 在 Partition 中的偏移量
        final int partition;    // 所在分区
        final long timestamp;

        Record(String key, String value, int partition, long offset) {
            this.key = key;
            this.value = value;
            this.partition = partition;
            this.offset = offset;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("[P%d@%d] key=%s, value=%s", partition, offset, key, value);
        }
    }

    /** 模拟 Kafka Partition：有序的消息日志 */
    static class Partition {
        final int id;
        private final java.util.List<Record> log = new java.util.ArrayList<>();
        private long nextOffset = 0;

        Partition(int id) { this.id = id; }

        /** 追加消息到分区末尾（Kafka 的核心：append-only log） */
        synchronized long append(String key, String value) {
            long offset = nextOffset++;
            log.add(new Record(key, value, id, offset));
            return offset;
        }

        /** 从指定 offset 开始读取消息 */
        java.util.List<Record> fetch(long fromOffset, int maxRecords) {
            java.util.List<Record> result = new java.util.ArrayList<>();
            for (int i = (int) fromOffset; i < log.size() && result.size() < maxRecords; i++) {
                result.add(log.get(i));
            }
            return result;
        }

        long latestOffset() { return nextOffset; }
        int size() { return log.size(); }
    }

    /** 模拟 Kafka Topic：包含多个 Partition */
    static class Topic {
        final String name;
        final Partition[] partitions;

        Topic(String name, int numPartitions) {
            this.name = name;
            this.partitions = new Partition[numPartitions];
            for (int i = 0; i < numPartitions; i++) {
                partitions[i] = new Partition(i);
            }
        }

        /** 根据 key 选择分区（模拟 DefaultPartitioner） */
        int selectPartition(String key) {
            if (key == null) {
                // 无 key：轮询分区
                return (int) (System.nanoTime() % partitions.length);
            }
            // 有 key：hash 取模（保证相同 key 进同一分区 → 保证顺序）
            return Math.abs(key.hashCode()) % partitions.length;
        }

        /** 发送消息 */
        Record send(String key, String value) {
            int partitionId = selectPartition(key);
            long offset = partitions[partitionId].append(key, value);
            return new Record(key, value, partitionId, offset);
        }

        /** 发送到指定分区 */
        Record sendToPartition(int partitionId, String key, String value) {
            long offset = partitions[partitionId].append(key, value);
            return new Record(key, value, partitionId, offset);
        }
    }

    /** 模拟 Consumer：维护每个 Partition 的消费 offset */
    static class SimulatedConsumer {
        final String groupId;
        final String consumerId;
        // 分配给该 Consumer 的分区列表
        private final java.util.List<Integer> assignedPartitions = new java.util.ArrayList<>();
        // 每个分区的消费 offset
        private final java.util.Map<Integer, Long> offsets = new java.util.concurrent.ConcurrentHashMap<>();

        SimulatedConsumer(String groupId, String consumerId) {
            this.groupId = groupId;
            this.consumerId = consumerId;
        }

        void assign(java.util.List<Integer> partitions) {
            assignedPartitions.clear();
            assignedPartitions.addAll(partitions);
            for (int p : partitions) {
                offsets.putIfAbsent(p, 0L);
            }
        }

        /** 拉取消息（poll 模式） */
        java.util.List<Record> poll(Topic topic, int maxRecords) {
            java.util.List<Record> result = new java.util.ArrayList<>();
            for (int partitionId : assignedPartitions) {
                long offset = offsets.getOrDefault(partitionId, 0L);
                java.util.List<Record> records = topic.partitions[partitionId].fetch(offset, maxRecords);
                result.addAll(records);
                if (!records.isEmpty()) {
                    // 更新 offset（自动提交模式）
                    offsets.put(partitionId, records.get(records.size() - 1).offset + 1);
                }
            }
            return result;
        }

        String getAssignment() {
            return String.format("%s → partitions=%s", consumerId, assignedPartitions);
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：Topic / Partition / Offset 基本模型 */
    static void demoBasicModel() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Topic / Partition / Offset 基本模型");
        System.out.println("═══════════════════════════════════════════════════");

        Topic topic = new Topic("orders", 3);

        System.out.println("\n  创建 Topic: orders, 3 个分区");
        System.out.println("  发送消息（key 决定分区路由）：");

        // 相同 key 的消息进同一分区（保证顺序）
        String[] keys = {"user-001", "user-002", "user-001", "user-003", "user-002", "user-001"};
        String[] values = {"下单", "下单", "支付", "下单", "支付", "发货"};

        for (int i = 0; i < keys.length; i++) {
            Record r = topic.send(keys[i], values[i]);
            System.out.printf("    send(key=%s, value=%s) → %s%n", keys[i], values[i], r);
        }

        System.out.println("\n  各分区消息数：");
        for (Partition p : topic.partitions) {
            System.out.printf("    Partition %d: %d 条消息%n", p.id, p.size());
        }

        System.out.println("\n  关键点：相同 key 的消息在同一分区内有序");
        System.out.println("  user-001 的消息顺序：下单 → 支付 → 发货（保证有序）");
        System.out.println();
    }

    /** 演示2：Consumer Group 分区分配 */
    static void demoConsumerGroup() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Consumer Group 分区分配与 Rebalance");
        System.out.println("═══════════════════════════════════════════════════");

        Topic topic = new Topic("events", 4);
        // 写入一些数据
        for (int i = 0; i < 12; i++) {
            topic.sendToPartition(i % 4, "k" + i, "event-" + i);
        }

        // 场景1：3 个 Consumer 消费 4 个 Partition
        System.out.println("\n  【场景1】3 个 Consumer 消费 4 个 Partition（Range 分配）");
        SimulatedConsumer c1 = new SimulatedConsumer("group-1", "consumer-1");
        SimulatedConsumer c2 = new SimulatedConsumer("group-1", "consumer-2");
        SimulatedConsumer c3 = new SimulatedConsumer("group-1", "consumer-3");

        // Range 分配策略：按范围均匀分配
        c1.assign(java.util.Arrays.asList(0, 1));  // 多分一个
        c2.assign(java.util.Arrays.asList(2));
        c3.assign(java.util.Arrays.asList(3));

        System.out.println("    " + c1.getAssignment());
        System.out.println("    " + c2.getAssignment());
        System.out.println("    " + c3.getAssignment());

        // 各 Consumer 拉取消息
        System.out.println("\n  各 Consumer 拉取消息：");
        for (SimulatedConsumer c : new SimulatedConsumer[]{c1, c2, c3}) {
            java.util.List<Record> records = c.poll(topic, 10);
            System.out.printf("    %s 拉取 %d 条: %s%n", c.consumerId, records.size(),
                    records.stream().map(r -> r.value).collect(java.util.stream.Collectors.joining(", ")));
        }

        // 场景2：Consumer 数量 > Partition 数量
        System.out.println("\n  【场景2】5 个 Consumer 消费 4 个 Partition");
        System.out.println("    consumer-1 → [P0]");
        System.out.println("    consumer-2 → [P1]");
        System.out.println("    consumer-3 → [P2]");
        System.out.println("    consumer-4 → [P3]");
        System.out.println("    consumer-5 → [] ← 空闲！无分区可消费");
        System.out.println("    结论：Consumer 数量不应超过 Partition 数量");

        // 场景3：Rebalance
        System.out.println("\n  【场景3】Rebalance 触发条件：");
        System.out.println("    1. Consumer 加入/离开 Group");
        System.out.println("    2. Consumer 心跳超时（session.timeout.ms）");
        System.out.println("    3. Topic 的 Partition 数量变化");
        System.out.println("    4. Consumer 调用 subscribe() 订阅新 Topic");
        System.out.println();
    }

    /** 演示3：消息顺序性保证 */
    static void demoOrdering() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：消息顺序性保证");
        System.out.println("═══════════════════════════════════════════════════");

        Topic topic = new Topic("order-events", 3);

        // 同一订单的事件用订单 ID 作为 key → 进同一分区 → 保证顺序
        System.out.println("\n  用订单 ID 作为 key，保证同一订单的事件有序：");
        String[][] events = {
                {"order-1001", "创建"}, {"order-1002", "创建"},
                {"order-1001", "支付"}, {"order-1001", "发货"},
                {"order-1002", "支付"}, {"order-1002", "取消"},
        };

        for (String[] e : events) {
            Record r = topic.send(e[0], e[1]);
            System.out.printf("    send(key=%s, value=%s) → P%d@%d%n", e[0], e[1], r.partition, r.offset);
        }

        // 验证同一 key 的消息在同一分区内有序
        System.out.println("\n  验证分区内顺序：");
        for (Partition p : topic.partitions) {
            if (p.size() > 0) {
                java.util.List<Record> records = p.fetch(0, 100);
                System.out.printf("    Partition %d: ", p.id);
                for (Record r : records) {
                    System.out.printf("[%s:%s] ", r.key, r.value);
                }
                System.out.println();
            }
        }

        System.out.println("\n  顺序性总结：");
        System.out.println("    ✓ 单 Partition 内消息严格有序");
        System.out.println("    ✗ 跨 Partition 无法保证全局顺序");
        System.out.println("    方案：用业务 key（如订单 ID）做分区路由");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Kafka 核心概念演示（混合模式）                         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟 Kafka 核心模型 ══════════");
        System.out.println();
        demoBasicModel();
        demoConsumerGroup();
        demoOrdering();

        // ===== Part B：连接真实 Kafka =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：连接真实 Kafka ══════════");
            System.out.println();
            RealKafka.run();
        } else {
            System.out.println("提示：运行 Part B（真实 Kafka）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 Kafka ====================

    static class RealKafka {

        static final String BOOTSTRAP_SERVERS = "localhost:9092";
        static final String TOPIC = "demo-topic";

        static void run() throws Exception {
            // 1. Producer 发送消息
            System.out.println("  【Producer 发送消息】");
            java.util.Properties producerProps = new java.util.Properties();
            producerProps.put("bootstrap.servers", BOOTSTRAP_SERVERS);
            producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producerProps.put("acks", "all");

            org.apache.kafka.clients.producer.KafkaProducer<String, String> producer =
                    new org.apache.kafka.clients.producer.KafkaProducer<>(producerProps);

            // 发送 5 条消息
            for (int i = 1; i <= 5; i++) {
                String key = "user-" + (i % 3);
                String value = "事件-" + i;
                org.apache.kafka.clients.producer.ProducerRecord<String, String> record =
                        new org.apache.kafka.clients.producer.ProducerRecord<>(TOPIC, key, value);

                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        System.out.printf("    发送成功: key=%s, partition=%d, offset=%d%n",
                                key, metadata.partition(), metadata.offset());
                    } else {
                        System.out.printf("    发送失败: %s%n", exception.getMessage());
                    }
                });
            }
            producer.flush();
            producer.close();

            // 2. Consumer 消费消息
            System.out.println("\n  【Consumer 消费消息】");
            java.util.Properties consumerProps = new java.util.Properties();
            consumerProps.put("bootstrap.servers", BOOTSTRAP_SERVERS);
            consumerProps.put("group.id", "demo-group");
            consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            consumerProps.put("auto.offset.reset", "earliest");
            consumerProps.put("enable.auto.commit", "true");

            org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer =
                    new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
            consumer.subscribe(java.util.Collections.singletonList(TOPIC));

            // 拉取消息（最多等 10 秒）
            long deadline = System.currentTimeMillis() + 10000;
            int totalConsumed = 0;
            while (System.currentTimeMillis() < deadline && totalConsumed < 5) {
                org.apache.kafka.clients.consumer.ConsumerRecords<String, String> records =
                        consumer.poll(java.time.Duration.ofMillis(1000));
                for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> r : records) {
                    System.out.printf("    消费: partition=%d, offset=%d, key=%s, value=%s%n",
                            r.partition(), r.offset(), r.key(), r.value());
                    totalConsumed++;
                }
            }
            consumer.close();

            System.out.printf("\n  共消费 %d 条消息%n", totalConsumed);
            // 提示：Topic 不自动删除，可在 Kafka 管理工具中查看
            System.out.println("  提示：如需删除 Topic，执行 kafka-topics.sh --delete --topic " + TOPIC);
        }
    }
}

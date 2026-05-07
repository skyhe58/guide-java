package com.example.mq.kafka.advanced;

/**
 * Kafka 高级特性演示（混合模式）
 *
 * <p>Part A：用 ConcurrentHashMap 模拟分区分配和消费者组（直接运行）
 * <ul>
 *   <li>分区分配策略：Range / RoundRobin / Sticky</li>
 *   <li>Consumer Group Rebalance 过程</li>
 *   <li>消息积压与消费者扩容</li>
 *   <li>Exactly-Once 语义（事务）</li>
 * </ul>
 *
 * <p>Part B：用 kafka-clients 演示消费者组行为
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d kafka}
 */
public class KafkaAdvancedDemo {

    // ==================== 分区分配策略模拟 ====================

    /** 分区分配策略接口 */
    interface PartitionAssignor {
        String name();
        java.util.Map<String, java.util.List<Integer>> assign(
                java.util.List<String> consumers, int numPartitions);
    }

    /** Range 分配：按范围均匀分配（Kafka 默认） */
    static class RangeAssignor implements PartitionAssignor {
        @Override
        public String name() { return "Range"; }

        @Override
        public java.util.Map<String, java.util.List<Integer>> assign(
                java.util.List<String> consumers, int numPartitions) {
            java.util.Map<String, java.util.List<Integer>> result = new java.util.LinkedHashMap<>();
            consumers.forEach(c -> result.put(c, new java.util.ArrayList<>()));

            int perConsumer = numPartitions / consumers.size();
            int extra = numPartitions % consumers.size();

            int partitionIdx = 0;
            for (int i = 0; i < consumers.size(); i++) {
                int count = perConsumer + (i < extra ? 1 : 0);
                for (int j = 0; j < count; j++) {
                    result.get(consumers.get(i)).add(partitionIdx++);
                }
            }
            return result;
        }
    }

    /** RoundRobin 分配：轮询分配 */
    static class RoundRobinAssignor implements PartitionAssignor {
        @Override
        public String name() { return "RoundRobin"; }

        @Override
        public java.util.Map<String, java.util.List<Integer>> assign(
                java.util.List<String> consumers, int numPartitions) {
            java.util.Map<String, java.util.List<Integer>> result = new java.util.LinkedHashMap<>();
            consumers.forEach(c -> result.put(c, new java.util.ArrayList<>()));

            for (int i = 0; i < numPartitions; i++) {
                String consumer = consumers.get(i % consumers.size());
                result.get(consumer).add(i);
            }
            return result;
        }
    }

    /** Sticky 分配：尽量保持原有分配不变（减少 Rebalance 开销） */
    static class StickyAssignor implements PartitionAssignor {
        private java.util.Map<String, java.util.List<Integer>> previousAssignment;

        @Override
        public String name() { return "Sticky"; }

        @Override
        public java.util.Map<String, java.util.List<Integer>> assign(
                java.util.List<String> consumers, int numPartitions) {
            java.util.Map<String, java.util.List<Integer>> result = new java.util.LinkedHashMap<>();
            consumers.forEach(c -> result.put(c, new java.util.ArrayList<>()));

            java.util.Set<Integer> assigned = new java.util.HashSet<>();

            // 尽量保持原有分配
            if (previousAssignment != null) {
                for (String consumer : consumers) {
                    java.util.List<Integer> prev = previousAssignment.get(consumer);
                    if (prev != null) {
                        for (int p : prev) {
                            if (p < numPartitions && !assigned.contains(p)) {
                                result.get(consumer).add(p);
                                assigned.add(p);
                            }
                        }
                    }
                }
            }

            // 分配剩余的分区（轮询）
            int consumerIdx = 0;
            for (int p = 0; p < numPartitions; p++) {
                if (!assigned.contains(p)) {
                    result.get(consumers.get(consumerIdx % consumers.size())).add(p);
                    consumerIdx++;
                }
            }

            previousAssignment = result;
            return result;
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：三种分区分配策略对比 */
    static void demoPartitionAssignment() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：三种分区分配策略对比");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<String> consumers = java.util.Arrays.asList("C1", "C2", "C3");
        int numPartitions = 7;

        System.out.printf("\n  %d 个 Consumer 消费 %d 个 Partition：%n", consumers.size(), numPartitions);

        PartitionAssignor[] assignors = {
                new RangeAssignor(), new RoundRobinAssignor(), new StickyAssignor()
        };

        for (PartitionAssignor assignor : assignors) {
            System.out.printf("\n  【%s 策略】%n", assignor.name());
            java.util.Map<String, java.util.List<Integer>> assignment =
                    assignor.assign(consumers, numPartitions);
            for (var entry : assignment.entrySet()) {
                System.out.printf("    %s → %s%n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("\n  策略对比：");
        System.out.printf("    %-12s %-30s %-20s%n", "策略", "特点", "适用场景");
        System.out.println("    " + "─".repeat(65));
        System.out.printf("    %-12s %-30s %-20s%n", "Range", "按范围分配，可能不均匀", "单 Topic 场景");
        System.out.printf("    %-12s %-30s %-20s%n", "RoundRobin", "轮询分配，最均匀", "多 Topic 场景");
        System.out.printf("    %-12s %-30s %-20s%n", "Sticky", "保持原有分配，减少迁移", "频繁 Rebalance");
        System.out.println();
    }

    /** 演示2：Rebalance 过程模拟 */
    static void demoRebalance() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Consumer Group Rebalance 过程");
        System.out.println("═══════════════════════════════════════════════════");

        StickyAssignor assignor = new StickyAssignor();
        int numPartitions = 6;

        // 初始：3 个 Consumer
        System.out.println("\n  【初始状态】3 个 Consumer：");
        java.util.List<String> consumers3 = java.util.Arrays.asList("C1", "C2", "C3");
        var assignment1 = assignor.assign(consumers3, numPartitions);
        printAssignment(assignment1);

        // Consumer C3 下线 → 触发 Rebalance
        System.out.println("\n  【C3 下线】触发 Rebalance（Sticky 策略尽量保持 C1/C2 不变）：");
        java.util.List<String> consumers2 = java.util.Arrays.asList("C1", "C2");
        var assignment2 = assignor.assign(consumers2, numPartitions);
        printAssignment(assignment2);

        // 新 Consumer C4 加入 → 触发 Rebalance
        System.out.println("\n  【C4 加入】触发 Rebalance：");
        java.util.List<String> consumers3New = java.util.Arrays.asList("C1", "C2", "C4");
        var assignment3 = assignor.assign(consumers3New, numPartitions);
        printAssignment(assignment3);

        System.out.println("\n  Rebalance 影响：");
        System.out.println("    1. Rebalance 期间所有 Consumer 暂停消费（STW）");
        System.out.println("    2. 分区重新分配后，Consumer 需要从上次提交的 offset 继续消费");
        System.out.println("    3. 频繁 Rebalance 会严重影响消费吞吐量");
        System.out.println("\n  减少 Rebalance 的方法：");
        System.out.println("    1. 增大 session.timeout.ms（默认 10s → 25s）");
        System.out.println("    2. 增大 max.poll.interval.ms（默认 5min）");
        System.out.println("    3. 使用 Sticky 分配策略");
        System.out.println("    4. 使用静态成员（group.instance.id）");
        System.out.println();
    }

    /** 演示3：消息积压与扩容 */
    static void demoBacklogAndScaling() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：消息积压与消费者扩容");
        System.out.println("═══════════════════════════════════════════════════");

        int numPartitions = 4;
        int produceRate = 1000;  // 每秒生产 1000 条
        int consumeRate = 300;   // 每个 Consumer 每秒消费 300 条

        System.out.printf("\n  Topic: 4 个 Partition, 生产速率: %d msg/s%n", produceRate);

        // 模拟不同 Consumer 数量下的消费能力
        for (int consumers = 1; consumers <= 6; consumers++) {
            int effectiveConsumers = Math.min(consumers, numPartitions);
            int totalConsumeRate = effectiveConsumers * consumeRate;
            boolean canKeepUp = totalConsumeRate >= produceRate;
            String status = canKeepUp ? "✓ 跟得上" : "✗ 积压";

            System.out.printf("    %d Consumer (有效 %d): 消费 %d msg/s %s%n",
                    consumers, effectiveConsumers, totalConsumeRate, status);
            if (consumers > numPartitions) {
                System.out.printf("      ⚠ %d 个 Consumer 空闲（超过 Partition 数量）%n",
                        consumers - numPartitions);
            }
        }

        System.out.println("\n  扩容策略：");
        System.out.println("    1. 先增加 Consumer 数量（不超过 Partition 数）");
        System.out.println("    2. 如果 Consumer 数 = Partition 数仍积压 → 增加 Partition");
        System.out.println("    3. 优化单个 Consumer 的处理速度（批量处理、异步处理）");
        System.out.println();
    }

    /** 演示4：Exactly-Once 语义 */
    static void demoExactlyOnce() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Exactly-Once 语义（事务）");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  三种消息语义：");
        System.out.printf("    %-15s %-30s %-20s%n", "语义", "实现方式", "适用场景");
        System.out.println("    " + "─".repeat(65));
        System.out.printf("    %-15s %-30s %-20s%n", "At-Most-Once", "acks=0 或自动提交 offset", "日志采集");
        System.out.printf("    %-15s %-30s %-20s%n", "At-Least-Once", "acks=all + 手动提交 offset", "大多数业务");
        System.out.printf("    %-15s %-30s %-20s%n", "Exactly-Once", "事务 + 幂等 Producer", "金融/计费");

        System.out.println("\n  Kafka 事务 API（Exactly-Once）：");
        System.out.println("    producer.initTransactions();");
        System.out.println("    try {");
        System.out.println("        producer.beginTransaction();");
        System.out.println("        producer.send(record1);");
        System.out.println("        producer.send(record2);");
        System.out.println("        // 提交 Consumer offset 到事务中");
        System.out.println("        producer.sendOffsetsToTransaction(offsets, groupMetadata);");
        System.out.println("        producer.commitTransaction();");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        producer.abortTransaction();");
        System.out.println("    }");

        System.out.println("\n  Consumer 端配置：");
        System.out.println("    isolation.level=read_committed  // 只读已提交的消息");
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    static void printAssignment(java.util.Map<String, java.util.List<Integer>> assignment) {
        for (var entry : assignment.entrySet()) {
            System.out.printf("    %s → %s%n", entry.getKey(), entry.getValue());
        }
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Kafka 高级特性演示（混合模式）                         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟分区分配与 Rebalance ══════════");
        System.out.println();
        demoPartitionAssignment();
        demoRebalance();
        demoBacklogAndScaling();
        demoExactlyOnce();

        // ===== Part B：连接真实 Kafka =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：真实 Kafka 消费者组 ══════════");
            System.out.println();
            RealKafkaAdvanced.run();
        } else {
            System.out.println("提示：运行 Part B（真实 Kafka）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 Kafka ====================

    static class RealKafkaAdvanced {

        static final String BOOTSTRAP_SERVERS = "localhost:9092";
        static final String TOPIC = "advanced-demo";

        static void run() throws Exception {
            // 1. 创建 Topic 并写入数据
            System.out.println("  【Producer 写入测试数据】");
            java.util.Properties producerProps = new java.util.Properties();
            producerProps.put("bootstrap.servers", BOOTSTRAP_SERVERS);
            producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            org.apache.kafka.clients.producer.KafkaProducer<String, String> producer =
                    new org.apache.kafka.clients.producer.KafkaProducer<>(producerProps);

            for (int i = 0; i < 20; i++) {
                String key = "user-" + (i % 5);
                producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>(
                        TOPIC, key, "event-" + i)).get();
            }
            producer.close();
            System.out.println("    写入 20 条消息到 " + TOPIC);

            // 2. 启动两个 Consumer 观察分区分配
            System.out.println("\n  【两个 Consumer 同时消费，观察分区分配】");

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);
            java.util.concurrent.atomic.AtomicInteger totalConsumed = new java.util.concurrent.atomic.AtomicInteger(0);

            for (int c = 1; c <= 2; c++) {
                final int consumerId = c;
                new Thread(() -> {
                    java.util.Properties props = new java.util.Properties();
                    props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
                    props.put("group.id", "advanced-demo-group");
                    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                    props.put("auto.offset.reset", "earliest");
                    props.put("enable.auto.commit", "true");
                    // Sticky 分配策略
                    props.put("partition.assignment.strategy",
                            "org.apache.kafka.clients.consumer.StickyAssignor");

                    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer =
                            new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
                    consumer.subscribe(java.util.Collections.singletonList(TOPIC));

                    long deadline = System.currentTimeMillis() + 15000;
                    int count = 0;
                    while (System.currentTimeMillis() < deadline) {
                        var records = consumer.poll(java.time.Duration.ofMillis(1000));
                        if (!records.isEmpty()) {
                            // 打印分区分配情况
                            java.util.Set<Integer> partitions = new java.util.HashSet<>();
                            for (var r : records) {
                                partitions.add(r.partition());
                                count++;
                            }
                            System.out.printf("    Consumer-%d: 消费 %d 条, 分区=%s%n",
                                    consumerId, records.count(), partitions);
                            totalConsumed.addAndGet(records.count());
                        }
                        if (count >= 10) break;
                    }
                    consumer.close();
                    latch.countDown();
                }).start();

                // 错开启动，让第二个 Consumer 触发 Rebalance
                if (c == 1) Thread.sleep(3000);
            }

            latch.await();
            System.out.printf("\n  两个 Consumer 共消费 %d 条%n", totalConsumed.get());
            System.out.println("  提示：如需删除 Topic，执行 kafka-topics.sh --delete --topic " + TOPIC);
        }
    }
}

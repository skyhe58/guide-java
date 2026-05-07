package com.example.mq.kafka.reliability;

/**
 * Kafka 消息可靠性演示（混合模式）
 *
 * <p>Part A：用 AtomicInteger 模拟 acks 确认机制（直接运行）
 * <ul>
 *   <li>acks=0 / acks=1 / acks=all 三种模式对比</li>
 *   <li>ISR（In-Sync Replicas）机制</li>
 *   <li>min.insync.replicas 配置</li>
 *   <li>Producer 重试与幂等性（enable.idempotence）</li>
 *   <li>Consumer offset 提交策略</li>
 * </ul>
 *
 * <p>Part B：用 kafka-clients 演示不同 acks 配置的行为
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d kafka}
 *
 * <h3>Kafka 消息可靠性全链路：</h3>
 * <pre>
 *  Producer                    Broker                     Consumer
 *  ┌──────┐   acks=all    ┌──────────┐              ┌──────────┐
 *  │ send │ ──────────→   │ Leader   │              │  poll    │
 *  │      │   ←────────   │ Follower1│ ──offset──→  │  commit  │
 *  │ 重试  │   confirm    │ Follower2│              │  手动ACK  │
 *  └──────┘               └──────────┘              └──────────┘
 *     ↑                       ↑                         ↑
 *  enable.idempotence    min.insync.replicas      enable.auto.commit
 * </pre>
 */
public class KafkaReliabilityDemo {

    // ==================== Part A：模拟 acks 机制 ====================

    /** 模拟 Kafka 副本 */
    static class Replica {
        final int id;
        final boolean isLeader;
        private volatile boolean inSync;    // 是否在 ISR 中
        private final java.util.List<String> log = new java.util.ArrayList<>();
        private volatile boolean alive = true;

        Replica(int id, boolean isLeader) {
            this.id = id;
            this.isLeader = isLeader;
            this.inSync = true;
        }

        boolean append(String message) {
            if (!alive) return false;
            log.add(message);
            return true;
        }

        int logSize() { return log.size(); }
    }

    /** 模拟 Kafka Partition（含副本） */
    static class ReplicatedPartition {
        private final Replica leader;
        private final java.util.List<Replica> followers;
        private final java.util.List<Replica> allReplicas;
        private int minInsyncReplicas = 2;

        ReplicatedPartition(int replicationFactor) {
            leader = new Replica(0, true);
            followers = new java.util.ArrayList<>();
            for (int i = 1; i < replicationFactor; i++) {
                followers.add(new Replica(i, false));
            }
            allReplicas = new java.util.ArrayList<>();
            allReplicas.add(leader);
            allReplicas.addAll(followers);
        }

        /** 获取 ISR 列表 */
        java.util.List<Replica> getISR() {
            java.util.List<Replica> isr = new java.util.ArrayList<>();
            for (Replica r : allReplicas) {
                if (r.alive && r.inSync) isr.add(r);
            }
            return isr;
        }

        /**
         * 发送消息，根据 acks 配置决定确认策略。
         * @param acks 0=不等待, 1=Leader确认, -1(all)=ISR全部确认
         * @return 是否发送成功
         */
        boolean send(String message, int acks) {
            // 写入 Leader
            if (!leader.alive) return false;
            leader.append(message);

            if (acks == 0) {
                // acks=0：不等待任何确认，直接返回（最快，可能丢消息）
                return true;
            }

            if (acks == 1) {
                // acks=1：Leader 写入成功即返回（Leader 宕机可能丢消息）
                return true;
            }

            // acks=all(-1)：等待 ISR 中所有副本确认
            java.util.List<Replica> isr = getISR();
            if (isr.size() < minInsyncReplicas) {
                // ISR 数量不足 → 拒绝写入（NotEnoughReplicasException）
                return false;
            }

            // 同步到所有 ISR 副本
            int synced = 0;
            for (Replica r : isr) {
                if (r.append(message)) synced++;
            }
            return synced >= minInsyncReplicas;
        }

        void setMinInsyncReplicas(int min) { this.minInsyncReplicas = min; }
    }

    /** 模拟幂等 Producer（enable.idempotence=true） */
    static class IdempotentProducer {
        private final java.util.Map<Integer, Long> partitionSequences = new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Set<String> deduplicationLog = new java.util.LinkedHashSet<>();

        /** 发送消息（带序列号去重） */
        boolean send(int partition, String message) {
            long seq = partitionSequences.merge(partition, 1L, Long::sum);
            String deduplicationKey = partition + "-" + seq;

            if (deduplicationLog.contains(deduplicationKey)) {
                return false; // 重复消息，Broker 端去重
            }
            deduplicationLog.add(deduplicationKey);
            return true;
        }

        int totalSent() { return deduplicationLog.size(); }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：acks=0 / 1 / all 对比 */
    static void demoAcksComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：acks=0 / 1 / all 三种模式对比");
        System.out.println("═══════════════════════════════════════════════════");

        // 3 副本的 Partition
        System.out.println("\n  Partition: 1 Leader + 2 Follower（replication-factor=3）");

        // acks=0
        ReplicatedPartition p0 = new ReplicatedPartition(3);
        boolean r0 = p0.send("消息A", 0);
        System.out.printf("\n  【acks=0】发送结果: %s%n", r0 ? "成功" : "失败");
        System.out.println("    特点：不等待确认，直接返回");
        System.out.println("    风险：消息可能丢失（网络丢包、Broker 宕机）");
        System.out.println("    场景：日志采集等允许少量丢失的场景");

        // acks=1
        ReplicatedPartition p1 = new ReplicatedPartition(3);
        boolean r1 = p1.send("消息B", 1);
        System.out.printf("\n  【acks=1】发送结果: %s%n", r1 ? "成功" : "失败");
        System.out.println("    特点：Leader 写入成功即返回");
        System.out.println("    风险：Leader 宕机且 Follower 未同步时丢消息");
        System.out.println("    场景：大多数业务场景的默认选择");

        // acks=all
        ReplicatedPartition pAll = new ReplicatedPartition(3);
        boolean rAll = pAll.send("消息C", -1);
        System.out.printf("\n  【acks=all】发送结果: %s%n", rAll ? "成功" : "失败");
        System.out.println("    特点：ISR 中所有副本确认后才返回");
        System.out.println("    风险：性能较低，但消息不丢失");
        System.out.println("    场景：金融交易、订单等不允许丢消息的场景");

        // 对比表
        System.out.println("\n  对比总结：");
        System.out.printf("    %-10s %-10s %-10s %-15s%n", "acks", "可靠性", "性能", "适用场景");
        System.out.println("    " + "─".repeat(50));
        System.out.printf("    %-10s %-10s %-10s %-15s%n", "0", "低", "最高", "日志采集");
        System.out.printf("    %-10s %-10s %-10s %-15s%n", "1", "中", "高", "一般业务");
        System.out.printf("    %-10s %-10s %-10s %-15s%n", "all", "高", "低", "金融/订单");
        System.out.println();
    }

    /** 演示2：ISR 机制 */
    static void demoISR() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：ISR（In-Sync Replicas）机制");
        System.out.println("═══════════════════════════════════════════════════");

        ReplicatedPartition partition = new ReplicatedPartition(3);
        partition.setMinInsyncReplicas(2);

        System.out.println("\n  初始 ISR: " + partition.getISR().size() + " 个副本");

        // 正常发送
        boolean r1 = partition.send("正常消息", -1);
        System.out.printf("  acks=all 发送: %s（ISR=%d >= min.insync.replicas=%d）%n",
                r1 ? "成功" : "失败", partition.getISR().size(), 2);

        // 模拟一个 Follower 掉线
        partition.followers.get(0).alive = false;
        partition.followers.get(0).inSync = false;
        System.out.printf("\n  Follower-1 掉线，ISR 缩减为 %d 个%n", partition.getISR().size());

        boolean r2 = partition.send("Follower掉线后的消息", -1);
        System.out.printf("  acks=all 发送: %s（ISR=%d >= min.insync.replicas=%d）%n",
                r2 ? "成功" : "失败", partition.getISR().size(), 2);

        // 再掉一个 Follower → ISR 不足
        partition.followers.get(1).alive = false;
        partition.followers.get(1).inSync = false;
        System.out.printf("\n  Follower-2 也掉线，ISR 缩减为 %d 个%n", partition.getISR().size());

        boolean r3 = partition.send("ISR不足的消息", -1);
        System.out.printf("  acks=all 发送: %s（ISR=%d < min.insync.replicas=%d → 拒绝写入）%n",
                r3 ? "成功" : "失败", partition.getISR().size(), 2);
        System.out.println();
    }

    /** 演示3：Producer 幂等性 */
    static void demoIdempotence() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Producer 幂等性（enable.idempotence=true）");
        System.out.println("═══════════════════════════════════════════════════");

        IdempotentProducer producer = new IdempotentProducer();

        System.out.println("\n  模拟网络重试导致的重复发送：");
        for (int i = 0; i < 3; i++) {
            boolean sent = producer.send(0, "订单创建");
            System.out.printf("    第 %d 次发送: %s%n", i + 1, sent ? "✓ 写入" : "✗ 去重（重复消息）");
        }

        System.out.printf("\n  实际写入消息数: %d（幂等保证不重复）%n", producer.totalSent());
        System.out.println("\n  幂等性原理：");
        System.out.println("    Producer 每次发送携带 <PID, Partition, SeqNumber>");
        System.out.println("    Broker 端维护每个 <PID, Partition> 的最新 SeqNumber");
        System.out.println("    如果收到的 SeqNumber <= 已有的，则判定为重复消息，丢弃");
        System.out.println("    注意：幂等性只保证单分区内不重复，跨分区需要事务");
        System.out.println();
    }

    /** 演示4：Consumer offset 提交策略 */
    static void demoOffsetCommit() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Consumer offset 提交策略");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【自动提交】enable.auto.commit=true");
        System.out.println("    Consumer 定期（auto.commit.interval.ms）自动提交 offset");
        System.out.println("    风险：消息处理失败但 offset 已提交 → 消息丢失");

        System.out.println("\n  【手动同步提交】consumer.commitSync()");
        System.out.println("    处理完消息后手动提交，阻塞直到提交成功");
        System.out.println("    优点：不会丢消息");
        System.out.println("    缺点：阻塞影响吞吐量");

        System.out.println("\n  【手动异步提交】consumer.commitAsync()");
        System.out.println("    异步提交，不阻塞");
        System.out.println("    风险：提交失败时不会重试（可能重复消费）");

        System.out.println("\n  【最佳实践】异步提交 + 同步兜底");
        System.out.println("    try {");
        System.out.println("        while (running) {");
        System.out.println("            records = consumer.poll(Duration.ofMillis(1000));");
        System.out.println("            processRecords(records);");
        System.out.println("            consumer.commitAsync();  // 正常用异步");
        System.out.println("        }");
        System.out.println("    } finally {");
        System.out.println("        consumer.commitSync();       // 关闭前用同步兜底");
        System.out.println("        consumer.close();");
        System.out.println("    }");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Kafka 消息可靠性演示（混合模式）                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟 acks + ISR 机制 ══════════");
        System.out.println();
        demoAcksComparison();
        demoISR();
        demoIdempotence();
        demoOffsetCommit();

        // ===== Part B：连接真实 Kafka =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：真实 Kafka acks 对比 ══════════");
            System.out.println();
            RealKafkaReliability.run();
        } else {
            System.out.println("提示：运行 Part B（真实 Kafka）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 Kafka ====================

    static class RealKafkaReliability {

        static final String BOOTSTRAP_SERVERS = "localhost:9092";

        static void run() throws Exception {
            String topic = "reliability-demo";

            // 对比不同 acks 的发送耗时
            System.out.println("  【不同 acks 配置的发送耗时对比】");
            for (String acks : new String[]{"0", "1", "all"}) {
                java.util.Properties props = new java.util.Properties();
                props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
                props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                props.put("acks", acks);

                org.apache.kafka.clients.producer.KafkaProducer<String, String> producer =
                        new org.apache.kafka.clients.producer.KafkaProducer<>(props);

                long start = System.currentTimeMillis();
                int count = 100;
                for (int i = 0; i < count; i++) {
                    producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>(
                            topic, "key-" + i, "value-" + i)).get(); // 同步等待确认
                }
                long elapsed = System.currentTimeMillis() - start;

                System.out.printf("    acks=%s: 发送 %d 条耗时 %d ms (%.0f msg/s)%n",
                        acks.equals("-1") ? "all" : acks, count, elapsed,
                        count * 1000.0 / elapsed);
                producer.close();
            }

            System.out.println("\n  提示：如需删除 Topic，执行 kafka-topics.sh --delete --topic " + topic);
        }
    }
}

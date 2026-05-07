package com.example.mq.kafka.spring;

/**
 * Kafka Spring 集成演示（混合模式）
 *
 * <p>Part A：用纯 Java 模拟 KafkaTemplate / @KafkaListener 的核心行为（直接运行）
 * <ul>
 *   <li>KafkaTemplate 模拟：send / sendDefault / 回调</li>
 *   <li>@KafkaListener 模拟：消费者容器 + 并发消费</li>
 *   <li>消息序列化：JSON / Avro 对比</li>
 *   <li>Spring Kafka 配置最佳实践</li>
 * </ul>
 *
 * <p>Part B：用 kafka-clients 模拟 Spring Kafka 的核心行为
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d kafka}
 *
 * <h3>Spring Kafka 核心组件：</h3>
 * <pre>
 *  KafkaTemplate        — 消息发送模板（封装 KafkaProducer）
 *  @KafkaListener       — 声明式消费者注解
 *  ConcurrentKafkaListenerContainerFactory — 消费者容器工厂
 *  ProducerFactory / ConsumerFactory       — 工厂配置
 * </pre>
 */
public class KafkaSpringDemo {

    // ==================== 模拟 KafkaTemplate ====================

    /** 模拟 KafkaTemplate 的核心功能 */
    static class SimulatedKafkaTemplate<K, V> {
        private final java.util.Map<String, java.util.List<Record<K, V>>> topics =
                new java.util.concurrent.ConcurrentHashMap<>();
        private String defaultTopic;

        void setDefaultTopic(String topic) { this.defaultTopic = topic; }

        private void ensureTopic(String topic) {
            topics.putIfAbsent(topic, java.util.Collections.synchronizedList(new java.util.ArrayList<>()));
        }

        /** send：发送到指定 Topic */
        SendResult<K, V> send(String topic, K key, V value) {
            ensureTopic(topic);
            Record<K, V> record = new Record<>(topic, key, value);
            topics.get(topic).add(record);
            return new SendResult<>(record, true);
        }

        /** sendDefault：发送到默认 Topic */
        SendResult<K, V> sendDefault(K key, V value) {
            if (defaultTopic == null) throw new IllegalStateException("未设置默认 Topic");
            return send(defaultTopic, key, value);
        }

        /** send with callback：异步发送 + 回调 */
        void sendWithCallback(String topic, K key, V value,
                              java.util.function.BiConsumer<SendResult<K, V>, Exception> callback) {
            try {
                SendResult<K, V> result = send(topic, key, value);
                callback.accept(result, null);
            } catch (Exception e) {
                callback.accept(null, e);
            }
        }

        java.util.List<Record<K, V>> getRecords(String topic) {
            return topics.getOrDefault(topic, java.util.Collections.emptyList());
        }
    }

    static class Record<K, V> {
        final String topic;
        final K key;
        final V value;
        final long timestamp = System.currentTimeMillis();

        Record(String topic, K key, V value) {
            this.topic = topic; this.key = key; this.value = value;
        }
    }

    static class SendResult<K, V> {
        final Record<K, V> record;
        final boolean success;

        SendResult(Record<K, V> record, boolean success) {
            this.record = record; this.success = success;
        }
    }

    // ==================== 模拟 @KafkaListener ====================

    /** 模拟消费者容器 */
    static class SimulatedListenerContainer<K, V> {
        private final SimulatedKafkaTemplate<K, V> template;
        private final String topic;
        private final int concurrency;
        private volatile boolean running = false;
        private final java.util.concurrent.atomic.AtomicInteger consumed = new java.util.concurrent.atomic.AtomicInteger(0);
        private int lastIndex = 0;

        SimulatedListenerContainer(SimulatedKafkaTemplate<K, V> template, String topic, int concurrency) {
            this.template = template;
            this.topic = topic;
            this.concurrency = concurrency;
        }

        void start(java.util.function.Consumer<V> handler) {
            running = true;
            for (int i = 0; i < concurrency; i++) {
                final int consumerId = i + 1;
                Thread t = new Thread(() -> {
                    while (running) {
                        java.util.List<Record<K, V>> records = template.getRecords(topic);
                        synchronized (records) {
                            if (lastIndex < records.size()) {
                                Record<K, V> record = records.get(lastIndex++);
                                System.out.printf("    [@KafkaListener-%d] 消费: key=%s, value=%s%n",
                                        consumerId, record.key, record.value);
                                handler.accept(record.value);
                                consumed.incrementAndGet();
                            }
                        }
                        try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        }

        void stop() { running = false; }
        int consumedCount() { return consumed.get(); }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：KafkaTemplate 基本使用 */
    static void demoKafkaTemplate() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：KafkaTemplate — send / sendDefault / callback");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedKafkaTemplate<String, String> template = new SimulatedKafkaTemplate<>();
        template.setDefaultTopic("default-topic");

        // send 到指定 Topic
        System.out.println("\n  【send 到指定 Topic】");
        SendResult<String, String> r1 = template.send("orders", "order-1001", "创建订单");
        System.out.printf("    template.send('orders', 'order-1001', '创建订单') → success=%s%n", r1.success);

        // sendDefault
        System.out.println("\n  【sendDefault 到默认 Topic】");
        SendResult<String, String> r2 = template.sendDefault("key-1", "默认Topic消息");
        System.out.printf("    template.sendDefault('key-1', '默认Topic消息') → topic=%s%n", r2.record.topic);

        // send with callback
        System.out.println("\n  【send with callback 异步回调】");
        template.sendWithCallback("orders", "order-1002", "支付订单", (result, ex) -> {
            if (ex == null) {
                System.out.printf("    回调: 发送成功, key=%s%n", result.record.key);
            } else {
                System.out.printf("    回调: 发送失败, error=%s%n", ex.getMessage());
            }
        });

        System.out.println("\n  Spring 配置示例：");
        System.out.println("    @Autowired");
        System.out.println("    private KafkaTemplate<String, String> kafkaTemplate;");
        System.out.println("    kafkaTemplate.send(\"orders\", orderId, orderJson);");
        System.out.println();
    }

    /** 演示2：@KafkaListener 消费者 */
    static void demoKafkaListener() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：@KafkaListener — 声明式消费者");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedKafkaTemplate<String, String> template = new SimulatedKafkaTemplate<>();

        // 先发送消息
        for (int i = 1; i <= 6; i++) {
            template.send("task-topic", "task-" + i, "处理任务 #" + i);
        }
        System.out.println("\n  发送 6 条消息，启动 2 个并发消费者：");

        // 启动消费者容器
        SimulatedListenerContainer<String, String> container =
                new SimulatedListenerContainer<>(template, "task-topic", 2);
        container.start(value -> {
            // 模拟业务处理
        });

        Thread.sleep(500);
        container.stop();

        System.out.printf("\n  共消费 %d 条消息%n", container.consumedCount());
        System.out.println("\n  Spring 配置示例：");
        System.out.println("    @KafkaListener(topics = \"task-topic\", groupId = \"task-group\",");
        System.out.println("                   concurrency = \"3\")");
        System.out.println("    public void handleTask(String message) {");
        System.out.println("        // 处理消息");
        System.out.println("    }");
        System.out.println();
    }

    /** 演示3：Spring Kafka 配置最佳实践 */
    static void demoConfiguration() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Spring Kafka 配置最佳实践");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  application.yml 推荐配置：");
        System.out.println("  spring:");
        System.out.println("    kafka:");
        System.out.println("      bootstrap-servers: localhost:9092");
        System.out.println("      producer:");
        System.out.println("        acks: all                    # 最高可靠性");
        System.out.println("        retries: 3                   # 重试次数");
        System.out.println("        batch-size: 16384             # 批量发送大小");
        System.out.println("        buffer-memory: 33554432       # 缓冲区大小");
        System.out.println("        key-serializer: org.apache.kafka.common.serialization.StringSerializer");
        System.out.println("        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer");
        System.out.println("      consumer:");
        System.out.println("        group-id: my-group");
        System.out.println("        auto-offset-reset: earliest  # 从最早开始消费");
        System.out.println("        enable-auto-commit: false    # 手动提交 offset");
        System.out.println("        max-poll-records: 500        # 每次 poll 最大记录数");
        System.out.println("        key-deserializer: org.apache.kafka.common.serialization.StringDeserializer");
        System.out.println("        value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer");
        System.out.println("      listener:");
        System.out.println("        ack-mode: manual_immediate   # 手动 ACK");
        System.out.println("        concurrency: 3               # 并发消费者数");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Kafka Spring 集成演示（混合模式）                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟 Spring Kafka 组件 ══════════");
        System.out.println();
        demoKafkaTemplate();
        demoKafkaListener();
        demoConfiguration();

        // ===== Part B =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：真实 Kafka 收发 ══════════");
            System.out.println();
            RealKafkaSpring.run();
        } else {
            System.out.println("提示：运行 Part B 请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 Kafka ====================

    static class RealKafkaSpring {

        static final String BOOTSTRAP_SERVERS = "localhost:9092";
        static final String TOPIC = "spring-demo";

        static void run() throws Exception {
            // 1. 模拟 KafkaTemplate.send（带回调）
            System.out.println("  【模拟 KafkaTemplate.send + 回调】");
            java.util.Properties producerProps = new java.util.Properties();
            producerProps.put("bootstrap.servers", BOOTSTRAP_SERVERS);
            producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producerProps.put("acks", "all");

            org.apache.kafka.clients.producer.KafkaProducer<String, String> producer =
                    new org.apache.kafka.clients.producer.KafkaProducer<>(producerProps);

            // 发送 JSON 格式消息（模拟 JsonSerializer）
            String[] orders = {
                    "{\"orderId\":\"ORD-1001\",\"amount\":299.99}",
                    "{\"orderId\":\"ORD-1002\",\"amount\":599.00}",
                    "{\"orderId\":\"ORD-1003\",\"amount\":99.50}",
            };

            for (String orderJson : orders) {
                producer.send(
                        new org.apache.kafka.clients.producer.ProducerRecord<>(TOPIC, orderJson),
                        (metadata, ex) -> {
                            if (ex == null) {
                                System.out.printf("    发送成功: partition=%d, offset=%d%n",
                                        metadata.partition(), metadata.offset());
                            } else {
                                System.out.printf("    发送失败: %s%n", ex.getMessage());
                            }
                        });
            }
            producer.flush();
            producer.close();

            // 2. 模拟 @KafkaListener 消费（手动提交 offset）
            System.out.println("\n  【模拟 @KafkaListener 手动 ACK 消费】");
            java.util.Properties consumerProps = new java.util.Properties();
            consumerProps.put("bootstrap.servers", BOOTSTRAP_SERVERS);
            consumerProps.put("group.id", "spring-demo-group");
            consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            consumerProps.put("auto.offset.reset", "earliest");
            consumerProps.put("enable.auto.commit", "false"); // 手动提交（模拟 ack-mode: manual）

            org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer =
                    new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
            consumer.subscribe(java.util.Collections.singletonList(TOPIC));

            long deadline = System.currentTimeMillis() + 10000;
            int totalConsumed = 0;
            while (System.currentTimeMillis() < deadline && totalConsumed < 3) {
                var records = consumer.poll(java.time.Duration.ofMillis(1000));
                for (var r : records) {
                    System.out.printf("    [@KafkaListener] partition=%d, offset=%d, value=%s%n",
                            r.partition(), r.offset(), r.value());
                    totalConsumed++;
                }
                if (!records.isEmpty()) {
                    // 手动同步提交 offset（模拟 Acknowledgment.acknowledge()）
                    consumer.commitSync();
                    System.out.println("    → commitSync() 手动提交 offset");
                }
            }
            consumer.close();

            System.out.printf("\n  共消费 %d 条消息%n", totalConsumed);
            System.out.println("  提示：如需删除 Topic，执行 kafka-topics.sh --delete --topic " + TOPIC);
        }
    }
}

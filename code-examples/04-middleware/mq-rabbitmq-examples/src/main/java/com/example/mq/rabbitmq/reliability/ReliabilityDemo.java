package com.example.mq.rabbitmq.reliability;

/**
 * RabbitMQ 消息可靠性演示（混合模式）
 *
 * <p>Part A：用回调+重试模拟消息确认机制（直接运行）
 * <ul>
 *   <li>Publisher Confirm（发送方确认）</li>
 *   <li>Publisher Return（消息无法路由时回调）</li>
 *   <li>消息持久化（Exchange + Queue + Message 三级持久化）</li>
 *   <li>消费者手动 ACK / NACK / Reject</li>
 *   <li>幂等消费（基于消息 ID 去重）</li>
 *   <li>重试机制（指数退避）</li>
 * </ul>
 *
 * <p>Part B：用 amqp-client 连接真实 RabbitMQ 演示 Confirm 和手动 ACK
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d rabbitmq}
 * <p>管理界面：http://localhost:15672（guest/guest）
 *
 * <h3>消息可靠性保障全链路：</h3>
 * <pre>
 *  Producer → [Confirm] → Exchange → [Return] → Queue → [持久化] → Consumer → [ACK]
 *
 *  1. Producer → Broker：Publisher Confirm 确认消息到达 Broker
 *  2. Exchange → Queue：Publisher Return 回调无法路由的消息
 *  3. Queue 持久化：durable=true + 消息 deliveryMode=2
 *  4. Consumer ACK：手动确认，处理失败可 NACK 重回队列
 *  5. 幂等消费：基于 messageId 去重，防止重复消费
 * </pre>
 */
public class ReliabilityDemo {

    // ==================== Part A：模拟消息确认机制 ====================

    /** 消息状态 */
    enum MessageStatus {
        PENDING,        // 待确认
        CONFIRMED,      // 已确认（Broker 收到）
        RETURNED,       // 被退回（无法路由）
        ACKED,          // 消费者已确认
        NACKED,         // 消费者拒绝
        REJECTED        // 消费者拒绝且不重回队列
    }

    /** 带确认机制的消息 */
    static class ReliableMessage {
        final String messageId;
        final String body;
        final String routingKey;
        final boolean persistent;   // 是否持久化
        volatile MessageStatus status;
        int retryCount;
        long createTime;

        ReliableMessage(String messageId, String body, String routingKey, boolean persistent) {
            this.messageId = messageId;
            this.body = body;
            this.routingKey = routingKey;
            this.persistent = persistent;
            this.status = MessageStatus.PENDING;
            this.retryCount = 0;
            this.createTime = System.currentTimeMillis();
        }
    }

    /** 模拟 Publisher Confirm 回调 */
    interface ConfirmCallback {
        void onConfirm(String messageId, boolean ack);
    }

    /** 模拟 Publisher Return 回调 */
    interface ReturnCallback {
        void onReturn(String messageId, String reason);
    }

    /** 带可靠性保障的 Broker */
    static class ReliableBroker {
        private final java.util.Map<String, java.util.concurrent.BlockingQueue<ReliableMessage>> queues =
                new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Map<String, String> bindings = new java.util.concurrent.ConcurrentHashMap<>();
        private final boolean durable;

        // 持久化存储（模拟磁盘）
        private final java.util.List<ReliableMessage> persistentStore = new java.util.ArrayList<>();

        private ConfirmCallback confirmCallback;
        private ReturnCallback returnCallback;

        ReliableBroker(boolean durable) {
            this.durable = durable;
        }

        void setConfirmCallback(ConfirmCallback callback) { this.confirmCallback = callback; }
        void setReturnCallback(ReturnCallback callback) { this.returnCallback = callback; }

        void declareQueue(String name) {
            queues.putIfAbsent(name, new java.util.concurrent.LinkedBlockingQueue<>());
        }

        void bind(String queue, String routingKey) {
            bindings.put(routingKey, queue);
        }

        /** 发布消息（带 Confirm 和 Return 回调） */
        void publish(ReliableMessage message) {
            String targetQueue = bindings.get(message.routingKey);

            if (targetQueue == null) {
                // 无法路由 → 触发 Return 回调
                message.status = MessageStatus.RETURNED;
                if (returnCallback != null) {
                    returnCallback.onReturn(message.messageId,
                            "NO_ROUTE: routingKey=" + message.routingKey + " 无匹配队列");
                }
                // Confirm 仍然是 ack（消息到达了 Exchange，只是无法路由）
                if (confirmCallback != null) {
                    confirmCallback.onConfirm(message.messageId, true);
                }
                return;
            }

            // 消息入队
            java.util.concurrent.BlockingQueue<ReliableMessage> queue = queues.get(targetQueue);
            if (queue != null) {
                queue.offer(message);
                // 持久化消息写入磁盘
                if (message.persistent && durable) {
                    persistentStore.add(message);
                }
                message.status = MessageStatus.CONFIRMED;
                if (confirmCallback != null) {
                    confirmCallback.onConfirm(message.messageId, true);
                }
            } else {
                // Broker 内部错误 → Confirm nack
                if (confirmCallback != null) {
                    confirmCallback.onConfirm(message.messageId, false);
                }
            }
        }

        /** 消费消息（手动 ACK 模式） */
        ReliableMessage consume(String queueName) {
            java.util.concurrent.BlockingQueue<ReliableMessage> queue = queues.get(queueName);
            return queue != null ? queue.poll() : null;
        }

        /** 消费者确认 */
        void ack(ReliableMessage message) {
            message.status = MessageStatus.ACKED;
        }

        /** 消费者拒绝（重回队列） */
        void nack(ReliableMessage message, boolean requeue) {
            if (requeue) {
                message.status = MessageStatus.NACKED;
                message.retryCount++;
                // 重回队列
                String targetQueue = bindings.get(message.routingKey);
                if (targetQueue != null) {
                    java.util.concurrent.BlockingQueue<ReliableMessage> queue = queues.get(targetQueue);
                    if (queue != null) queue.offer(message);
                }
            } else {
                message.status = MessageStatus.REJECTED;
            }
        }

        int persistentCount() { return persistentStore.size(); }
    }

    /** 幂等消费器：基于 messageId 去重 */
    static class IdempotentConsumer {
        private final java.util.Set<String> processedIds =
                java.util.Collections.synchronizedSet(new java.util.HashSet<>());

        /** 消费消息（幂等：同一 messageId 只处理一次） */
        boolean consume(ReliableMessage message) {
            if (processedIds.contains(message.messageId)) {
                return false; // 重复消息，跳过
            }
            processedIds.add(message.messageId);
            return true; // 首次消费
        }

        int processedCount() { return processedIds.size(); }
    }

    /** 指数退避重试器 */
    static class ExponentialBackoffRetry {
        private final int maxRetries;
        private final long baseDelayMs;

        ExponentialBackoffRetry(int maxRetries, long baseDelayMs) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
        }

        /** 计算第 n 次重试的延迟时间 */
        long getDelay(int retryCount) {
            return baseDelayMs * (1L << Math.min(retryCount, 10)); // 2^n * baseDelay
        }

        boolean shouldRetry(int retryCount) {
            return retryCount < maxRetries;
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：Publisher Confirm + Return */
    static void demoPublisherConfirm() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Publisher Confirm + Return 回调");
        System.out.println("═══════════════════════════════════════════════════");

        ReliableBroker broker = new ReliableBroker(true);
        broker.declareQueue("order.queue");
        broker.bind("order.queue", "order.create");

        // 设置 Confirm 回调
        broker.setConfirmCallback((msgId, ack) ->
                System.out.printf("    [Confirm] messageId=%s, ack=%s%n", msgId, ack));

        // 设置 Return 回调
        broker.setReturnCallback((msgId, reason) ->
                System.out.printf("    [Return]  messageId=%s, reason=%s%n", msgId, reason));

        // 发送可路由的消息 → Confirm ack
        System.out.println("\n  发送可路由消息（routingKey=order.create）：");
        broker.publish(new ReliableMessage("msg-001", "创建订单", "order.create", true));

        // 发送不可路由的消息 → Return + Confirm ack
        System.out.println("\n  发送不可路由消息（routingKey=order.cancel，无绑定队列）：");
        broker.publish(new ReliableMessage("msg-002", "取消订单", "order.cancel", true));

        System.out.println("\n  说明：");
        System.out.println("    Confirm 回调：消息是否到达 Exchange（ack=true 表示到达）");
        System.out.println("    Return 回调：消息到达 Exchange 但无法路由到 Queue");
        System.out.println("    两者不矛盾：消息可以同时触发 Return 和 Confirm(ack=true)");
        System.out.println();
    }

    /** 演示2：消息持久化 */
    static void demoPersistence() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：消息持久化 — 三级持久化保障");
        System.out.println("═══════════════════════════════════════════════════");

        // 非持久化 Broker
        ReliableBroker transientBroker = new ReliableBroker(false);
        transientBroker.declareQueue("temp.queue");
        transientBroker.bind("temp.queue", "temp.key");
        transientBroker.publish(new ReliableMessage("t1", "临时消息", "temp.key", false));

        // 持久化 Broker
        ReliableBroker durableBroker = new ReliableBroker(true);
        durableBroker.declareQueue("durable.queue");
        durableBroker.bind("durable.queue", "durable.key");
        durableBroker.publish(new ReliableMessage("d1", "持久消息", "durable.key", true));
        durableBroker.publish(new ReliableMessage("d2", "持久消息2", "durable.key", true));

        System.out.println("\n  三级持久化：");
        System.out.printf("    1. Exchange 持久化: durable=true%n");
        System.out.printf("    2. Queue 持久化:    durable=true%n");
        System.out.printf("    3. Message 持久化:  deliveryMode=2%n");
        System.out.printf("\n  非持久化 Broker 磁盘消息数: %d（重启后丢失）%n", transientBroker.persistentCount());
        System.out.printf("  持久化 Broker 磁盘消息数:   %d（重启后恢复）%n", durableBroker.persistentCount());
        System.out.println();
    }

    /** 演示3：消费者手动 ACK / NACK */
    static void demoManualAck() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：消费者手动 ACK / NACK / Reject");
        System.out.println("═══════════════════════════════════════════════════");

        ReliableBroker broker = new ReliableBroker(true);
        broker.declareQueue("task.queue");
        broker.bind("task.queue", "task");

        broker.publish(new ReliableMessage("m1", "任务1-成功", "task", true));
        broker.publish(new ReliableMessage("m2", "任务2-失败需重试", "task", true));
        broker.publish(new ReliableMessage("m3", "任务3-永久失败", "task", true));

        System.out.println("\n  消费消息：");

        // 消息1：处理成功 → ACK
        ReliableMessage msg1 = broker.consume("task.queue");
        System.out.printf("    收到: %s → 处理成功 → ACK%n", msg1.body);
        broker.ack(msg1);

        // 消息2：处理失败 → NACK（重回队列）
        ReliableMessage msg2 = broker.consume("task.queue");
        System.out.printf("    收到: %s → 处理失败 → NACK(requeue=true)%n", msg2.body);
        broker.nack(msg2, true);
        System.out.printf("    消息重回队列，retryCount=%d%n", msg2.retryCount);

        // 消息3：永久失败 → Reject（不重回队列，进入死信队列）
        ReliableMessage msg3 = broker.consume("task.queue");
        System.out.printf("    收到: %s → 永久失败 → Reject(requeue=false)%n", msg3.body);
        broker.nack(msg3, false);

        System.out.println("\n  ACK 模式对比：");
        System.out.println("    autoAck=true:  消息投递后立即确认（可能丢消息）");
        System.out.println("    autoAck=false: 手动确认，处理完再 ACK（推荐）");
        System.out.println();
    }

    /** 演示4：幂等消费 */
    static void demoIdempotent() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：幂等消费 — 基于 messageId 去重");
        System.out.println("═══════════════════════════════════════════════════");

        IdempotentConsumer consumer = new IdempotentConsumer();

        ReliableMessage msg = new ReliableMessage("order-1001", "创建订单", "order", true);

        System.out.println("\n  模拟消息重复投递（网络抖动/重试导致）：");
        for (int i = 1; i <= 3; i++) {
            boolean processed = consumer.consume(msg);
            System.out.printf("    第 %d 次投递 messageId=%s → %s%n",
                    i, msg.messageId, processed ? "✓ 处理" : "✗ 跳过（重复）");
        }

        System.out.printf("\n  实际处理次数: %d（幂等保证只处理一次）%n", consumer.processedCount());
        System.out.println("\n  幂等实现方案：");
        System.out.println("    1. 数据库唯一索引（messageId）");
        System.out.println("    2. Redis SETNX（messageId, 1, TTL）");
        System.out.println("    3. 业务状态机（订单状态不允许重复流转）");
        System.out.println();
    }

    /** 演示5：指数退避重试 */
    static void demoRetry() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：指数退避重试策略");
        System.out.println("═══════════════════════════════════════════════════");

        ExponentialBackoffRetry retry = new ExponentialBackoffRetry(5, 1000);

        System.out.println("\n  重试延迟计算（baseDelay=1000ms, maxRetries=5）：");
        for (int i = 0; i < 7; i++) {
            boolean shouldRetry = retry.shouldRetry(i);
            long delay = retry.getDelay(i);
            System.out.printf("    第 %d 次重试: delay=%5dms, shouldRetry=%s%n",
                    i + 1, delay, shouldRetry ? "✓" : "✗ 超过最大重试次数");
        }

        System.out.println("\n  重试策略最佳实践：");
        System.out.println("    1. 设置最大重试次数，避免无限重试");
        System.out.println("    2. 使用指数退避，避免重试风暴");
        System.out.println("    3. 超过最大重试次数后发送到死信队列");
        System.out.println("    4. 记录重试日志，方便排查问题");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  RabbitMQ 消息可靠性演示（混合模式）                    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟消息确认机制 ══════════");
        System.out.println();
        demoPublisherConfirm();
        demoPersistence();
        demoManualAck();
        demoIdempotent();
        demoRetry();

        // ===== Part B：连接真实 RabbitMQ =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：真实 RabbitMQ Confirm + ACK ══════════");
            System.out.println();
            RealReliability.run();
        } else {
            System.out.println("提示：运行 Part B（真实 RabbitMQ）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 RabbitMQ ====================

    static class RealReliability {

        static void run() throws Exception {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("guest");
            factory.setPassword("guest");

            try (com.rabbitmq.client.Connection connection = factory.newConnection();
                 com.rabbitmq.client.Channel channel = connection.createChannel()) {

                demoRealConfirm(channel);
                demoRealManualAck(channel);
            }
        }

        /** Publisher Confirm 真实演示 */
        static void demoRealConfirm(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — Publisher Confirm】");

            // 开启 Confirm 模式
            channel.confirmSelect();

            String queue = "demo.confirm.queue";
            channel.queueDeclare(queue, true, false, true, null);

            // 发送消息并等待确认
            for (int i = 1; i <= 3; i++) {
                String body = "确认消息 #" + i;
                channel.basicPublish("", queue, null, body.getBytes("UTF-8"));
                // 等待 Broker 确认（同步方式）
                boolean confirmed = channel.waitForConfirms(5000);
                System.out.printf("    发送: %s → Confirm: %s%n", body, confirmed ? "✓ ack" : "✗ nack");
            }

            // 清理
            channel.queueDelete(queue);
            System.out.println();
        }

        /** 手动 ACK 真实演示 */
        static void demoRealManualAck(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — 手动 ACK】");

            String queue = "demo.ack.queue";
            channel.queueDeclare(queue, false, false, true, null);

            // 发送 3 条消息
            for (int i = 1; i <= 3; i++) {
                channel.basicPublish("", queue, null, ("任务 #" + i).getBytes("UTF-8"));
            }
            System.out.println("    发送 3 条消息");

            Thread.sleep(100);

            // 手动 ACK 消费
            // autoAck=false 表示手动确认
            com.rabbitmq.client.GetResponse resp;
            int count = 0;
            while ((resp = channel.basicGet(queue, false)) != null) {
                String body = new String(resp.getBody(), "UTF-8");
                long deliveryTag = resp.getEnvelope().getDeliveryTag();

                if (count == 1) {
                    // 第 2 条消息模拟处理失败 → NACK 重回队列
                    channel.basicNack(deliveryTag, false, true);
                    System.out.printf("    收到: %s (tag=%d) → NACK(requeue=true)%n", body, deliveryTag);
                } else {
                    // 处理成功 → ACK
                    channel.basicAck(deliveryTag, false);
                    System.out.printf("    收到: %s (tag=%d) → ACK%n", body, deliveryTag);
                }
                count++;
                if (count >= 4) break; // 防止无限循环（NACK 重回的消息）
            }

            // 清理（如需保留队列在管理界面查看，注释掉即可）
            channel.queueDelete(queue);
            System.out.println("    提示：如需保留队列，注释掉 queueDelete 即可");
            System.out.println();
        }
    }
}

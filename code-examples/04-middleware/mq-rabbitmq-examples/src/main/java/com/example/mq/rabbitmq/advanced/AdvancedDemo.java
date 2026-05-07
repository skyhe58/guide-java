package com.example.mq.rabbitmq.advanced;

/**
 * RabbitMQ 高级特性演示（混合模式）
 *
 * <p>Part A：用 Java 并发工具模拟高级特性（直接运行）
 * <ul>
 *   <li>延迟消息（DelayQueue 模拟 TTL + 死信队列）</li>
 *   <li>死信队列（DLX, Dead Letter Exchange）</li>
 *   <li>优先级队列</li>
 *   <li>消息 TTL（过期时间）</li>
 * </ul>
 *
 * <p>Part B：用 amqp-client 连接真实 RabbitMQ 演示 TTL + DLX
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d rabbitmq}
 *
 * <h3>延迟消息实现方案：</h3>
 * <pre>
 *  方案1：TTL + 死信队列（原生支持）
 *  Producer → normal.exchange → normal.queue(TTL=30s, DLX=dlx.exchange)
 *                                    ↓ 消息过期
 *                              dlx.exchange → dlx.queue → Consumer
 *
 *  方案2：rabbitmq-delayed-message-exchange 插件（推荐）
 *  Producer → delayed.exchange(x-delayed-type) → queue → Consumer
 * </pre>
 */
public class AdvancedDemo {

    // ==================== 延迟消息模型 ====================

    /** 带过期时间的消息 */
    static class DelayedMessage implements Comparable<DelayedMessage> {
        final String messageId;
        final String body;
        final long expireAt;        // 过期时间戳
        final int priority;         // 优先级（0-9，越大越优先）
        final long delayMs;         // 延迟毫秒数

        DelayedMessage(String messageId, String body, long delayMs, int priority) {
            this.messageId = messageId;
            this.body = body;
            this.delayMs = delayMs;
            this.expireAt = System.currentTimeMillis() + delayMs;
            this.priority = priority;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expireAt;
        }

        @Override
        public int compareTo(DelayedMessage o) {
            return Long.compare(this.expireAt, o.expireAt);
        }
    }

    /** 模拟死信队列机制 */
    static class DeadLetterBroker {
        // 正常队列（带 TTL）
        private final java.util.concurrent.PriorityBlockingQueue<DelayedMessage> normalQueue =
                new java.util.concurrent.PriorityBlockingQueue<>();
        // 死信队列（接收过期/被拒绝的消息）
        private final java.util.concurrent.BlockingQueue<DelayedMessage> dlxQueue =
                new java.util.concurrent.LinkedBlockingQueue<>();
        // 优先级队列
        private final java.util.PriorityQueue<DelayedMessage> priorityQueue =
                new java.util.PriorityQueue<>((a, b) -> Integer.compare(b.priority, a.priority));

        /** 发送延迟消息到正常队列 */
        void publishDelayed(DelayedMessage message) {
            normalQueue.offer(message);
        }

        /** 发送优先级消息 */
        void publishWithPriority(DelayedMessage message) {
            priorityQueue.offer(message);
        }

        /** 检查正常队列中过期的消息，转移到死信队列 */
        int transferExpiredToDlx() {
            int transferred = 0;
            java.util.List<DelayedMessage> expired = new java.util.ArrayList<>();

            // 取出所有过期消息
            for (DelayedMessage msg : normalQueue) {
                if (msg.isExpired()) {
                    expired.add(msg);
                }
            }

            for (DelayedMessage msg : expired) {
                normalQueue.remove(msg);
                dlxQueue.offer(msg);
                transferred++;
            }
            return transferred;
        }

        /** 从死信队列消费（延迟消息的消费者） */
        DelayedMessage consumeFromDlx() {
            return dlxQueue.poll();
        }

        /** 从优先级队列消费 */
        DelayedMessage consumeFromPriority() {
            return priorityQueue.poll();
        }

        int normalQueueSize() { return normalQueue.size(); }
        int dlxQueueSize() { return dlxQueue.size(); }
        int priorityQueueSize() { return priorityQueue.size(); }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：延迟消息 — TTL + 死信队列 */
    static void demoDelayedMessage() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：延迟消息 — TTL + 死信队列");
        System.out.println("═══════════════════════════════════════════════════");

        DeadLetterBroker broker = new DeadLetterBroker();

        // 发送不同延迟时间的消息
        System.out.println("\n  发送延迟消息：");
        broker.publishDelayed(new DelayedMessage("order-1", "订单超时取消 #1001", 500, 0));
        broker.publishDelayed(new DelayedMessage("order-2", "订单超时取消 #1002", 200, 0));
        broker.publishDelayed(new DelayedMessage("order-3", "订单超时取消 #1003", 800, 0));
        System.out.println("    order-1: delay=500ms");
        System.out.println("    order-2: delay=200ms");
        System.out.println("    order-3: delay=800ms");

        System.out.printf("    正常队列: %d 条, 死信队列: %d 条%n",
                broker.normalQueueSize(), broker.dlxQueueSize());

        // 等待消息过期
        System.out.println("\n  等待消息过期...");
        Thread.sleep(300);
        int t1 = broker.transferExpiredToDlx();
        System.out.printf("    300ms 后: 转移 %d 条到死信队列（order-2 已过期）%n", t1);

        Thread.sleep(300);
        int t2 = broker.transferExpiredToDlx();
        System.out.printf("    600ms 后: 转移 %d 条到死信队列（order-1 已过期）%n", t2);

        Thread.sleep(300);
        int t3 = broker.transferExpiredToDlx();
        System.out.printf("    900ms 后: 转移 %d 条到死信队列（order-3 已过期）%n", t3);

        // 从死信队列消费
        System.out.println("\n  从死信队列消费（按过期顺序）：");
        DelayedMessage msg;
        while ((msg = broker.consumeFromDlx()) != null) {
            System.out.printf("    消费: %s (delay=%dms)%n", msg.body, msg.delayMs);
        }

        System.out.println("\n  典型应用场景：");
        System.out.println("    1. 订单超时未支付自动取消（30分钟）");
        System.out.println("    2. 预约提醒（提前15分钟通知）");
        System.out.println("    3. 延迟重试（失败后延迟重新处理）");
        System.out.println();
    }

    /** 演示2：死信队列的三种触发条件 */
    static void demoDlxTriggers() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：死信队列的三种触发条件");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  消息进入死信队列的三种情况：");
        System.out.println("    1. 消息 TTL 过期（x-message-ttl 或 per-message TTL）");
        System.out.println("    2. 消费者拒绝消息（basic.reject / basic.nack，requeue=false）");
        System.out.println("    3. 队列达到最大长度（x-max-length）");

        System.out.println("\n  死信队列配置：");
        System.out.println("    // 声明正常队列时指定死信交换机");
        System.out.println("    Map<String, Object> args = new HashMap<>();");
        System.out.println("    args.put(\"x-dead-letter-exchange\", \"dlx.exchange\");");
        System.out.println("    args.put(\"x-dead-letter-routing-key\", \"dlx.routing.key\");");
        System.out.println("    args.put(\"x-message-ttl\", 30000);  // 30秒 TTL");
        System.out.println("    args.put(\"x-max-length\", 1000);    // 最大长度");
        System.out.println("    channel.queueDeclare(\"normal.queue\", true, false, false, args);");
        System.out.println();
    }

    /** 演示3：优先级队列 */
    static void demoPriorityQueue() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：优先级队列");
        System.out.println("═══════════════════════════════════════════════════");

        DeadLetterBroker broker = new DeadLetterBroker();

        // 发送不同优先级的消息
        System.out.println("\n  发送消息（不同优先级）：");
        broker.publishWithPriority(new DelayedMessage("m1", "普通任务", 0, 1));
        broker.publishWithPriority(new DelayedMessage("m2", "紧急任务", 0, 9));
        broker.publishWithPriority(new DelayedMessage("m3", "重要任务", 0, 5));
        broker.publishWithPriority(new DelayedMessage("m4", "低优先级", 0, 0));
        System.out.println("    m1: priority=1（普通）");
        System.out.println("    m2: priority=9（紧急）");
        System.out.println("    m3: priority=5（重要）");
        System.out.println("    m4: priority=0（低）");

        // 消费（按优先级从高到低）
        System.out.println("\n  消费顺序（优先级从高到低）：");
        DelayedMessage msg;
        while ((msg = broker.consumeFromPriority()) != null) {
            System.out.printf("    priority=%d → %s%n", msg.priority, msg.body);
        }

        System.out.println("\n  优先级队列配置：");
        System.out.println("    // 声明队列时设置最大优先级");
        System.out.println("    args.put(\"x-max-priority\", 10);");
        System.out.println("    // 发送消息时设置优先级");
        System.out.println("    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()");
        System.out.println("        .priority(5).build();");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  RabbitMQ 高级特性演示（混合模式）                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟延迟消息+死信队列 ══════════");
        System.out.println();
        demoDelayedMessage();
        demoDlxTriggers();
        demoPriorityQueue();

        // ===== Part B：连接真实 RabbitMQ =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：真实 RabbitMQ TTL + DLX ══════════");
            System.out.println();
            RealAdvanced.run();
        } else {
            System.out.println("提示：运行 Part B（真实 RabbitMQ）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 RabbitMQ ====================

    static class RealAdvanced {

        static void run() throws Exception {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost("localhost");

            try (com.rabbitmq.client.Connection connection = factory.newConnection();
                 com.rabbitmq.client.Channel channel = connection.createChannel()) {

                demoRealTtlDlx(channel);
                demoRealPriority(channel);
            }
        }

        /** TTL + 死信队列真实演示 */
        static void demoRealTtlDlx(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — TTL + 死信队列】");

            // 声明死信交换机和队列
            String dlxExchange = "demo.dlx.exchange";
            String dlxQueue = "demo.dlx.queue";
            channel.exchangeDeclare(dlxExchange, "direct", false, true, null);
            channel.queueDeclare(dlxQueue, false, false, true, null);
            channel.queueBind(dlxQueue, dlxExchange, "dlx.key");

            // 声明正常队列（TTL=2秒，死信转发到 dlx.exchange）
            String normalQueue = "demo.ttl.queue";
            java.util.Map<String, Object> queueArgs = new java.util.HashMap<>();
            queueArgs.put("x-message-ttl", 2000);                      // 消息 TTL 2秒
            queueArgs.put("x-dead-letter-exchange", dlxExchange);       // 死信交换机
            queueArgs.put("x-dead-letter-routing-key", "dlx.key");      // 死信路由键
            channel.queueDeclare(normalQueue, false, false, true, queueArgs);

            // 发送消息到正常队列
            channel.basicPublish("", normalQueue, null, "延迟2秒的消息".getBytes("UTF-8"));
            System.out.println("    发送消息到 TTL 队列（2秒后过期转入死信队列）");

            // 等待消息过期
            System.out.println("    等待 3 秒...");
            Thread.sleep(3000);

            // 从死信队列消费
            com.rabbitmq.client.GetResponse resp = channel.basicGet(dlxQueue, true);
            if (resp != null) {
                System.out.printf("    从死信队列收到: %s ✓%n", new String(resp.getBody(), "UTF-8"));
            } else {
                System.out.println("    死信队列为空 ✗");
            }

            // 清理（如需保留在管理界面查看，注释掉即可）
            channel.exchangeDelete(dlxExchange);
            channel.queueDelete(normalQueue);
            System.out.println("    提示：如需保留队列，注释掉清理代码即可");
            System.out.println();
        }

        /** 优先级队列真实演示 */
        static void demoRealPriority(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — 优先级队列】");

            String queue = "demo.priority.queue";
            java.util.Map<String, Object> args = new java.util.HashMap<>();
            args.put("x-max-priority", 10);
            channel.queueDeclare(queue, false, false, true, args);

            // 发送不同优先级的消息
            int[] priorities = {1, 9, 5, 0, 7};
            String[] bodies = {"普通", "紧急", "重要", "低", "较高"};
            for (int i = 0; i < priorities.length; i++) {
                com.rabbitmq.client.AMQP.BasicProperties props =
                        new com.rabbitmq.client.AMQP.BasicProperties.Builder()
                                .priority(priorities[i]).build();
                channel.basicPublish("", queue, props, bodies[i].getBytes("UTF-8"));
            }
            System.out.println("    发送 5 条不同优先级的消息");

            Thread.sleep(200);

            // 消费（应按优先级从高到低）
            System.out.println("    消费顺序：");
            com.rabbitmq.client.GetResponse resp;
            while ((resp = channel.basicGet(queue, true)) != null) {
                System.out.printf("      priority=%d → %s%n",
                        resp.getProps().getPriority(),
                        new String(resp.getBody(), "UTF-8"));
            }

            channel.queueDelete(queue);
            System.out.println();
        }
    }
}

package com.example.mq.rabbitmq.spring;

/**
 * RabbitMQ Spring 集成演示（混合模式）
 *
 * <p>Part A：用纯 Java 模拟 RabbitTemplate / MessageConverter / ListenerContainer（直接运行）
 * <ul>
 *   <li>RabbitTemplate 模拟：发送消息 + 接收消息 + convertAndSend</li>
 *   <li>MessageConverter 模拟：JSON 序列化/反序列化</li>
 *   <li>MessageListenerContainer 模拟：消费者监听 + 并发消费</li>
 *   <li>@RabbitListener 注解原理模拟</li>
 * </ul>
 *
 * <p>Part B：用 amqp-client 模拟 Spring AMQP 的核心行为
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d rabbitmq}
 *
 * <h3>Spring AMQP 核心组件：</h3>
 * <pre>
 *  RabbitTemplate          — 消息发送模板（类似 JdbcTemplate）
 *  MessageConverter        — 消息序列化（默认 SimpleMessageConverter，推荐 Jackson2JsonMessageConverter）
 *  MessageListenerContainer — 消费者容器（管理消费者线程、ACK、并发数）
 *  @RabbitListener          — 声明式消费者注解
 *  RabbitAdmin              — 自动声明 Exchange/Queue/Binding
 * </pre>
 */
public class SpringIntegrationDemo {

    // ==================== 消息模型 ====================

    /** 模拟 Spring AMQP 的 Message 对象 */
    static class Message {
        final byte[] body;
        final MessageProperties properties;

        Message(byte[] body, MessageProperties properties) {
            this.body = body;
            this.properties = properties;
        }
    }

    /** 模拟 MessageProperties */
    static class MessageProperties {
        String contentType = "text/plain";
        String messageId;
        String correlationId;
        String replyTo;
        java.util.Map<String, Object> headers = new java.util.LinkedHashMap<>();

        MessageProperties() {
            this.messageId = java.util.UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // ==================== MessageConverter ====================

    /** 模拟 MessageConverter 接口 */
    interface MessageConverter {
        Message toMessage(Object object);
        Object fromMessage(Message message);
    }

    /** 模拟 SimpleMessageConverter（默认：String/byte[] 直接转换） */
    static class SimpleMessageConverter implements MessageConverter {
        @Override
        public Message toMessage(Object object) {
            MessageProperties props = new MessageProperties();
            props.contentType = "text/plain";
            return new Message(object.toString().getBytes(), props);
        }

        @Override
        public Object fromMessage(Message message) {
            return new String(message.body);
        }
    }

    /** 模拟 Jackson2JsonMessageConverter（推荐：对象 ↔ JSON） */
    static class JsonMessageConverter implements MessageConverter {
        @Override
        public Message toMessage(Object object) {
            MessageProperties props = new MessageProperties();
            props.contentType = "application/json";
            // 简化的 JSON 序列化
            String json;
            if (object instanceof OrderMessage) {
                OrderMessage order = (OrderMessage) object;
                json = String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f,\"status\":\"%s\"}",
                        order.orderId, order.userId, order.amount, order.status);
            } else {
                json = object.toString();
            }
            return new Message(json.getBytes(), props);
        }

        @Override
        public Object fromMessage(Message message) {
            String json = new String(message.body);
            // 简化的 JSON 反序列化
            if (json.contains("orderId")) {
                String orderId = extractJsonField(json, "orderId");
                String userId = extractJsonField(json, "userId");
                double amount = Double.parseDouble(extractJsonField(json, "amount"));
                String status = extractJsonField(json, "status");
                return new OrderMessage(orderId, userId, amount, status);
            }
            return json;
        }

        private String extractJsonField(String json, String field) {
            int start = json.indexOf("\"" + field + "\":") + field.length() + 3;
            if (json.charAt(start) == '"') {
                start++;
                int end = json.indexOf('"', start);
                return json.substring(start, end);
            } else {
                int end = json.indexOf(',', start);
                if (end == -1) end = json.indexOf('}', start);
                return json.substring(start, end);
            }
        }
    }

    /** 订单消息 DTO */
    static class OrderMessage {
        final String orderId;
        final String userId;
        final double amount;
        final String status;

        OrderMessage(String orderId, String userId, double amount, String status) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
        }

        @Override
        public String toString() {
            return String.format("OrderMessage{orderId=%s, userId=%s, amount=%.2f, status=%s}",
                    orderId, userId, amount, status);
        }
    }

    // ==================== RabbitTemplate 模拟 ====================

    /** 模拟 RabbitTemplate 的核心功能 */
    static class SimulatedRabbitTemplate {
        private final java.util.Map<String, java.util.concurrent.BlockingQueue<Message>> queues =
                new java.util.concurrent.ConcurrentHashMap<>();
        private MessageConverter messageConverter = new SimpleMessageConverter();

        void setMessageConverter(MessageConverter converter) {
            this.messageConverter = converter;
        }

        void declareQueue(String name) {
            queues.putIfAbsent(name, new java.util.concurrent.LinkedBlockingQueue<>());
        }

        /** convertAndSend：将对象转换为 Message 并发送 */
        void convertAndSend(String queueName, Object object) {
            Message message = messageConverter.toMessage(object);
            java.util.concurrent.BlockingQueue<Message> queue = queues.get(queueName);
            if (queue != null) {
                queue.offer(message);
            }
        }

        /** receiveAndConvert：接收 Message 并转换为对象 */
        Object receiveAndConvert(String queueName) {
            java.util.concurrent.BlockingQueue<Message> queue = queues.get(queueName);
            if (queue == null) return null;
            Message message = queue.poll();
            return message != null ? messageConverter.fromMessage(message) : null;
        }

        /** send：直接发送 Message 对象 */
        void send(String queueName, Message message) {
            java.util.concurrent.BlockingQueue<Message> queue = queues.get(queueName);
            if (queue != null) {
                queue.offer(message);
            }
        }

        int queueSize(String name) {
            java.util.concurrent.BlockingQueue<Message> q = queues.get(name);
            return q != null ? q.size() : 0;
        }
    }

    // ==================== MessageListenerContainer 模拟 ====================

    /** 模拟消息监听器接口（对应 @RabbitListener） */
    interface MessageHandler {
        void handle(Object message);
    }

    /** 模拟 SimpleMessageListenerContainer */
    static class SimulatedListenerContainer {
        private final SimulatedRabbitTemplate template;
        private final String queueName;
        private final int concurrency;
        private volatile boolean running = false;
        private final java.util.List<Thread> consumerThreads = new java.util.ArrayList<>();

        SimulatedListenerContainer(SimulatedRabbitTemplate template, String queueName, int concurrency) {
            this.template = template;
            this.queueName = queueName;
            this.concurrency = concurrency;
        }

        /** 启动监听容器（模拟 @RabbitListener 的启动过程） */
        void start(MessageHandler handler) {
            running = true;
            for (int i = 0; i < concurrency; i++) {
                final int consumerId = i + 1;
                Thread t = new Thread(() -> {
                    while (running) {
                        Object msg = template.receiveAndConvert(queueName);
                        if (msg != null) {
                            System.out.printf("    [Consumer-%d] 收到: %s%n", consumerId, msg);
                            handler.handle(msg);
                        } else {
                            try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                        }
                    }
                }, "consumer-" + consumerId);
                t.setDaemon(true);
                t.start();
                consumerThreads.add(t);
            }
        }

        void stop() {
            running = false;
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：RabbitTemplate 基本使用 */
    static void demoRabbitTemplate() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：RabbitTemplate — convertAndSend / receiveAndConvert");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedRabbitTemplate template = new SimulatedRabbitTemplate();
        template.declareQueue("order.queue");

        // 使用默认 SimpleMessageConverter
        System.out.println("\n  【SimpleMessageConverter】发送字符串：");
        template.convertAndSend("order.queue", "创建订单 #1001");
        template.convertAndSend("order.queue", "创建订单 #1002");

        Object msg1 = template.receiveAndConvert("order.queue");
        Object msg2 = template.receiveAndConvert("order.queue");
        System.out.printf("    收到: %s%n", msg1);
        System.out.printf("    收到: %s%n", msg2);

        // 切换为 JSON Converter
        System.out.println("\n  【Jackson2JsonMessageConverter】发送对象：");
        template.setMessageConverter(new JsonMessageConverter());
        template.convertAndSend("order.queue",
                new OrderMessage("ORD-1001", "USER-001", 299.99, "CREATED"));
        template.convertAndSend("order.queue",
                new OrderMessage("ORD-1002", "USER-002", 599.00, "PAID"));

        Object obj1 = template.receiveAndConvert("order.queue");
        Object obj2 = template.receiveAndConvert("order.queue");
        System.out.printf("    收到: %s%n", obj1);
        System.out.printf("    收到: %s%n", obj2);
        System.out.println();
    }

    /** 演示2：MessageListenerContainer — 并发消费 */
    static void demoListenerContainer() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：MessageListenerContainer — 并发消费");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedRabbitTemplate template = new SimulatedRabbitTemplate();
        template.declareQueue("task.queue");

        // 发送 6 条消息
        for (int i = 1; i <= 6; i++) {
            template.convertAndSend("task.queue", "任务 #" + i);
        }
        System.out.printf("\n  发送 6 条消息，启动 3 个并发消费者：%n");

        // 启动 3 个并发消费者
        SimulatedListenerContainer container =
                new SimulatedListenerContainer(template, "task.queue", 3);
        container.start(msg -> {
            // 模拟处理耗时
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread.sleep(1000);
        container.stop();

        System.out.println("\n  对应 Spring 配置：");
        System.out.println("    @RabbitListener(queues = \"task.queue\", concurrency = \"3-10\")");
        System.out.println("    public void handleMessage(String message) { ... }");
        System.out.println();
    }

    /** 演示3：@RabbitListener 注解原理 */
    static void demoRabbitListenerPrinciple() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：@RabbitListener 注解工作原理");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  @RabbitListener 的启动流程：");
        System.out.println("    1. Spring 扫描 @RabbitListener 注解的方法");
        System.out.println("    2. 为每个方法创建 MessageListenerContainer");
        System.out.println("    3. Container 启动消费者线程，连接 RabbitMQ");
        System.out.println("    4. 收到消息后，通过 MessageConverter 反序列化");
        System.out.println("    5. 调用 @RabbitListener 标注的方法处理消息");
        System.out.println("    6. 处理成功自动 ACK，异常则 NACK");

        System.out.println("\n  常用配置示例：");
        System.out.println("    // 简单监听");
        System.out.println("    @RabbitListener(queues = \"order.queue\")");
        System.out.println("    public void handleOrder(OrderMessage order) { ... }");
        System.out.println();
        System.out.println("    // 自动声明 Exchange + Queue + Binding");
        System.out.println("    @RabbitListener(bindings = @QueueBinding(");
        System.out.println("        value = @Queue(\"order.queue\"),");
        System.out.println("        exchange = @Exchange(\"order.exchange\"),");
        System.out.println("        key = \"order.create\"");
        System.out.println("    ))");
        System.out.println("    public void handleOrder(OrderMessage order) { ... }");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  RabbitMQ Spring 集成演示（混合模式）                   ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟 Spring AMQP 组件 ══════════");
        System.out.println();
        demoRabbitTemplate();
        demoListenerContainer();
        demoRabbitListenerPrinciple();

        // ===== Part B：连接真实 RabbitMQ =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：真实 RabbitMQ 收发 ══════════");
            System.out.println();
            RealSpringIntegration.run();
        } else {
            System.out.println("提示：运行 Part B（真实 RabbitMQ）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 RabbitMQ ====================

    static class RealSpringIntegration {

        static void run() throws Exception {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost("localhost");

            try (com.rabbitmq.client.Connection connection = factory.newConnection();
                 com.rabbitmq.client.Channel channel = connection.createChannel()) {

                String queue = "demo.spring.queue";
                channel.queueDeclare(queue, false, false, true, null);

                // 模拟 convertAndSend：发送 JSON 消息
                System.out.println("  【模拟 convertAndSend】发送 JSON 消息：");
                String json = "{\"orderId\":\"ORD-1001\",\"userId\":\"USER-001\",\"amount\":299.99}";
                com.rabbitmq.client.AMQP.BasicProperties props =
                        new com.rabbitmq.client.AMQP.BasicProperties.Builder()
                                .contentType("application/json")
                                .messageId(java.util.UUID.randomUUID().toString())
                                .build();
                channel.basicPublish("", queue, props, json.getBytes("UTF-8"));
                System.out.printf("    发送: %s%n", json);

                Thread.sleep(100);

                // 模拟 receiveAndConvert：接收并解析
                System.out.println("\n  【模拟 receiveAndConvert】接收 JSON 消息：");
                com.rabbitmq.client.GetResponse resp = channel.basicGet(queue, true);
                if (resp != null) {
                    String body = new String(resp.getBody(), "UTF-8");
                    System.out.printf("    收到: %s%n", body);
                    System.out.printf("    contentType: %s%n", resp.getProps().getContentType());
                    System.out.printf("    messageId: %s%n", resp.getProps().getMessageId());
                }

                // 清理（如需保留在管理界面查看，注释掉即可）
                channel.queueDelete(queue);
                System.out.println("\n    提示：如需保留队列，注释掉 queueDelete 即可");
            }
        }
    }
}

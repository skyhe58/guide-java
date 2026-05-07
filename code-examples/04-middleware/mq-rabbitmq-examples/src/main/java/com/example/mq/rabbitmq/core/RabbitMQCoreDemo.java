package com.example.mq.rabbitmq.core;

/**
 * RabbitMQ 核心概念演示（混合模式）
 *
 * <p>Part A：用纯 Java 模拟 Exchange 路由模型（直接运行）
 * <p>Part B：用 amqp-client 连接真实 RabbitMQ 演示消息收发
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d rabbitmq}
 *
 * <p>本示例用 Java 内置的 Map + BlockingQueue 完整模拟 RabbitMQ 的三种 Exchange 路由行为：
 * Direct（精确匹配）、Topic（通配符匹配）、Fanout（广播）。
 * 无需外部依赖，直接运行即可观察路由差异。</p>
 *
 * <p>如需连接真实 RabbitMQ，使用：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d rabbitmq}</p>
 *
 * <h3>RabbitMQ 消息流转模型：</h3>
 * <pre>
 * Producer → Exchange →（Binding Rule）→ Queue → Consumer
 *
 * - Producer：消息生产者，将消息发送到 Exchange
 * - Exchange：交换机，根据类型和 Routing Key 决定消息路由到哪些 Queue
 * - Binding：绑定关系，定义 Exchange 到 Queue 的路由规则（含 Binding Key）
 * - Queue：消息队列，用 BlockingQueue 实现，FIFO 存储消息
 * - Consumer：消息消费者，从 Queue 中取出并处理消息
 * </pre>
 */
public class RabbitMQCoreDemo {

    // ==================== 消息模型 ====================

    /**
     * 消息体，对应 RabbitMQ 中的 Message。
     * 包含 routingKey（路由键）、body（消息内容）、headers（消息头，用于 Headers Exchange 等场景）。
     */
    static class Message {
        final String routingKey;
        final String body;
        final java.util.Map<String, String> headers;

        Message(String routingKey, String body) {
            this(routingKey, body, java.util.Collections.emptyMap());
        }

        Message(String routingKey, String body, java.util.Map<String, String> headers) {
            this.routingKey = routingKey;
            this.body = body;
            this.headers = headers;
        }

        @Override
        public String toString() {
            return String.format("[routingKey=%s, body=%s]", routingKey, body);
        }
    }

    // ==================== 队列 ====================

    /**
     * 模拟 RabbitMQ Queue，底层用 LinkedBlockingQueue 实现 FIFO 语义。
     * RabbitMQ 的 Queue 是消息的最终存储位置，Consumer 从 Queue 中拉取消息。
     */
    static class Queue {
        final String name;
        private final java.util.concurrent.BlockingQueue<Message> messages =
                new java.util.concurrent.LinkedBlockingQueue<>();

        Queue(String name) {
            this.name = name;
        }

        /** 入队：Exchange 路由成功后将消息放入队列 */
        void enqueue(Message msg) {
            messages.offer(msg);
        }

        /** 出队：Consumer 消费一条消息，队列为空时返回 null */
        Message dequeue() {
            return messages.poll();
        }

        int size() {
            return messages.size();
        }

        /** 消费队列中所有消息并打印 */
        void consumeAll(String consumerTag) {
            Message msg;
            while ((msg = dequeue()) != null) {
                System.out.printf("    [%s] 消费 ← %s: %s%n", consumerTag, name, msg);
            }
        }
    }

    // ==================== Exchange 接口与三种实现 ====================

    /**
     * Exchange 接口，定义路由行为。
     * RabbitMQ 中 Exchange 负责接收 Producer 发来的消息，并根据路由规则分发到绑定的 Queue。
     */
    interface Exchange {
        String name();

        /**
         * 路由消息：根据 Exchange 类型和 Binding 规则，将消息投递到匹配的 Queue。
         * @return 成功路由到的队列数量
         */
        int route(Message message, java.util.List<Binding> bindings);
    }

    /**
     * Direct Exchange：Routing Key 精确匹配。
     * 消息的 routingKey 必须与 Binding 的 bindingKey 完全相同，消息才会路由到对应 Queue。
     * 适用场景：点对点通信，如订单创建、支付回调。
     */
    static class DirectExchange implements Exchange {
        private final String name;

        DirectExchange(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int route(Message message, java.util.List<Binding> bindings) {
            int count = 0;
            for (Binding binding : bindings) {
                // Direct 路由核心：routingKey == bindingKey
                if (binding.exchangeName.equals(this.name)
                        && message.routingKey.equals(binding.bindingKey)) {
                    binding.queue.enqueue(message);
                    System.out.printf("  ✓ [Direct] routingKey=\"%s\" 匹配 bindingKey=\"%s\" → %s%n",
                            message.routingKey, binding.bindingKey, binding.queue.name);
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Topic Exchange：通配符模式匹配。
     * Routing Key 和 Binding Key 都是以 "." 分隔的多级词组。
     * 通配符规则：
     *   * （星号）— 匹配恰好一个词，如 "order.*" 匹配 "order.create" 但不匹配 "order.item.create"
     *   # （井号）— 匹配零个或多个词，如 "order.#" 匹配 "order"、"order.create"、"order.item.create"
     * 适用场景：按主题分类的消息，如日志系统（log.info、log.error.detail）。
     */
    static class TopicExchange implements Exchange {
        private final String name;

        TopicExchange(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int route(Message message, java.util.List<Binding> bindings) {
            int count = 0;
            for (Binding binding : bindings) {
                if (binding.exchangeName.equals(this.name)
                        && topicMatch(binding.bindingKey, message.routingKey)) {
                    binding.queue.enqueue(message);
                    System.out.printf("  ✓ [Topic]  routingKey=\"%s\" 匹配 pattern=\"%s\" → %s%n",
                            message.routingKey, binding.bindingKey, binding.queue.name);
                    count++;
                }
            }
            return count;
        }

        /**
         * Topic 通配符匹配算法（动态规划实现）。
         * 将 pattern 和 routingKey 按 "." 拆分为词组数组，逐词匹配：
         *   - 普通词：必须完全相等
         *   - "*"：匹配恰好一个词
         *   - "#"：匹配零个或多个词
         */
        static boolean topicMatch(String pattern, String routingKey) {
            String[] patternWords = pattern.split("\\.");
            String[] routingWords = routingKey.split("\\.");

            // dp[i][j] 表示 pattern 前 i 个词能否匹配 routingKey 前 j 个词
            boolean[][] dp = new boolean[patternWords.length + 1][routingWords.length + 1];
            dp[0][0] = true;

            // 处理 pattern 开头连续 "#" 的情况：# 可以匹配零个词
            for (int i = 1; i <= patternWords.length; i++) {
                if (patternWords[i - 1].equals("#")) {
                    dp[i][0] = dp[i - 1][0];
                }
            }

            for (int i = 1; i <= patternWords.length; i++) {
                for (int j = 1; j <= routingWords.length; j++) {
                    String pw = patternWords[i - 1];
                    if (pw.equals("#")) {
                        // "#" 匹配零个词（dp[i-1][j]）或多个词（dp[i][j-1]）
                        dp[i][j] = dp[i - 1][j] || dp[i][j - 1];
                    } else if (pw.equals("*") || pw.equals(routingWords[j - 1])) {
                        // "*" 匹配恰好一个词，或者普通词精确匹配
                        dp[i][j] = dp[i - 1][j - 1];
                    }
                }
            }
            return dp[patternWords.length][routingWords.length];
        }
    }

    /**
     * Fanout Exchange：广播模式，忽略 Routing Key，将消息投递到所有绑定的 Queue。
     * 适用场景：广播通知（系统公告、配置更新、缓存刷新）。
     */
    static class FanoutExchange implements Exchange {
        private final String name;

        FanoutExchange(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int route(Message message, java.util.List<Binding> bindings) {
            int count = 0;
            for (Binding binding : bindings) {
                if (binding.exchangeName.equals(this.name)) {
                    // Fanout 核心：不看 routingKey，直接投递到所有绑定队列
                    binding.queue.enqueue(message);
                    System.out.printf("  ✓ [Fanout] 广播 → %s%n", binding.queue.name);
                    count++;
                }
            }
            return count;
        }
    }

    // ==================== 绑定关系 ====================

    /**
     * Binding：Exchange 与 Queue 之间的绑定关系。
     * 在 RabbitMQ 中，Queue 必须通过 Binding 绑定到 Exchange 才能接收消息。
     * bindingKey 是路由匹配的依据（Fanout Exchange 忽略此字段）。
     */
    static class Binding {
        final String exchangeName;
        final Queue queue;
        final String bindingKey;

        Binding(String exchangeName, Queue queue, String bindingKey) {
            this.exchangeName = exchangeName;
            this.queue = queue;
            this.bindingKey = bindingKey;
        }
    }

    // ==================== Broker（消息代理） ====================

    /**
     * Broker：模拟 RabbitMQ 服务端，管理所有 Exchange、Queue、Binding。
     * 提供 declareExchange / declareQueue / bind / publish / consume 等核心 API，
     * 对应 RabbitMQ 中 Channel 上的 AMQP 操作。
     */
    static class Broker {
        private final java.util.Map<String, Exchange> exchanges = new java.util.LinkedHashMap<>();
        private final java.util.Map<String, Queue> queues = new java.util.LinkedHashMap<>();
        private final java.util.List<Binding> bindings = new java.util.ArrayList<>();

        /** 声明 Exchange（幂等操作，重复声明不会报错） */
        void declareExchange(Exchange exchange) {
            exchanges.put(exchange.name(), exchange);
        }

        /** 声明 Queue（幂等操作） */
        Queue declareQueue(String name) {
            return queues.computeIfAbsent(name, Queue::new);
        }

        /** 绑定 Queue 到 Exchange，指定 bindingKey */
        void bind(String exchangeName, String queueName, String bindingKey) {
            Queue queue = queues.get(queueName);
            if (queue == null) {
                throw new IllegalArgumentException("Queue 不存在: " + queueName);
            }
            bindings.add(new Binding(exchangeName, queue, bindingKey));
        }

        /**
         * 发布消息到指定 Exchange。
         * Exchange 根据自身类型和 Binding 规则将消息路由到匹配的 Queue。
         * 如果没有匹配的 Queue，消息将被丢弃（生产环境可配置 mandatory 标志触发 Return 回调）。
         */
        int publish(String exchangeName, Message message) {
            Exchange exchange = exchanges.get(exchangeName);
            if (exchange == null) {
                throw new IllegalArgumentException("Exchange 不存在: " + exchangeName);
            }
            int routed = exchange.route(message, bindings);
            if (routed == 0) {
                System.out.printf("  ✗ routingKey=\"%s\" 无匹配队列，消息丢弃%n", message.routingKey);
            }
            return routed;
        }

        /** 消费指定 Queue 中的所有消息 */
        void consume(String queueName, String consumerTag) {
            Queue queue = queues.get(queueName);
            if (queue != null) {
                queue.consumeAll(consumerTag);
            }
        }

        /** 获取指定 Queue 中的消息数量 */
        int queueSize(String queueName) {
            Queue queue = queues.get(queueName);
            return queue != null ? queue.size() : 0;
        }
    }

    // ==================== 演示方法 ====================

    /**
     * 演示 Direct Exchange：Routing Key 精确匹配路由。
     * 创建一个 Direct Exchange 和两个 Queue，分别绑定不同的 bindingKey，
     * 发送三条消息观察精确匹配行为。
     */
    static void demoDirectExchange() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  Direct Exchange 演示 — Routing Key 精确匹配");
        System.out.println("═══════════════════════════════════════════════════");

        Broker broker = new Broker();

        // 1. 声明 Exchange 和 Queue
        broker.declareExchange(new DirectExchange("order.direct"));
        broker.declareQueue("order.create.queue");
        broker.declareQueue("order.pay.queue");

        // 2. 绑定：每个 Queue 绑定一个精确的 bindingKey
        broker.bind("order.direct", "order.create.queue", "order.create");
        broker.bind("order.direct", "order.pay.queue", "order.pay");

        System.out.println("\n绑定关系：");
        System.out.println("  order.direct → order.create.queue (bindingKey=order.create)");
        System.out.println("  order.direct → order.pay.queue    (bindingKey=order.pay)");

        // 3. 发布消息
        System.out.println("\n发布消息：");
        broker.publish("order.direct", new Message("order.create", "创建订单 #1001"));
        broker.publish("order.direct", new Message("order.pay", "支付订单 #1001"));
        broker.publish("order.direct", new Message("order.cancel", "取消订单 #1002"));

        // 4. 消费消息
        System.out.println("\n消费结果：");
        broker.consume("order.create.queue", "createConsumer");
        broker.consume("order.pay.queue", "payConsumer");

        System.out.println();
    }

    /**
     * 演示 Topic Exchange：通配符模式匹配路由。
     * 创建一个 Topic Exchange 和三个 Queue，使用 * 和 # 通配符绑定，
     * 发送多条不同层级的 routingKey 消息，观察通配符匹配行为。
     */
    static void demoTopicExchange() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  Topic Exchange 演示 — 通配符模式匹配");
        System.out.println("═══════════════════════════════════════════════════");

        Broker broker = new Broker();

        // 1. 声明 Exchange 和 Queue
        broker.declareExchange(new TopicExchange("log.topic"));
        broker.declareQueue("log.all.queue");
        broker.declareQueue("log.error.queue");
        broker.declareQueue("log.detail.queue");

        // 2. 绑定：使用通配符
        //    "#" 匹配零个或多个词 → log.all.queue 接收所有 log 开头的消息
        //    精确匹配 → log.error.queue 只接收 routingKey 为 "log.error" 的消息
        //    "*" 匹配恰好一个词 → log.detail.queue 接收 "log.任意一个词.detail" 的消息
        broker.bind("log.topic", "log.all.queue", "log.#");
        broker.bind("log.topic", "log.error.queue", "log.error");
        broker.bind("log.topic", "log.detail.queue", "log.*.detail");

        System.out.println("\n绑定关系：");
        System.out.println("  log.topic → log.all.queue    (pattern=log.#)");
        System.out.println("  log.topic → log.error.queue  (pattern=log.error)");
        System.out.println("  log.topic → log.detail.queue (pattern=log.*.detail)");

        // 3. 发布消息：不同层级的 routingKey
        System.out.println("\n发布消息：");
        broker.publish("log.topic", new Message("log.info", "普通日志信息"));
        broker.publish("log.topic", new Message("log.error", "数据库连接失败"));
        broker.publish("log.topic", new Message("log.error.detail", "连接超时，重试3次后失败"));
        broker.publish("log.topic", new Message("log.warn.detail", "内存使用率超过80%"));

        // 4. 消费消息，验证路由结果
        System.out.println("\n消费结果：");
        System.out.printf("  log.all.queue 消息数: %d（log.# 匹配所有）%n",
                broker.queueSize("log.all.queue"));
        broker.consume("log.all.queue", "allConsumer");

        System.out.printf("  log.error.queue 消息数: %d（仅精确匹配 log.error）%n",
                broker.queueSize("log.error.queue"));
        broker.consume("log.error.queue", "errorConsumer");

        System.out.printf("  log.detail.queue 消息数: %d（log.*.detail 匹配）%n",
                broker.queueSize("log.detail.queue"));
        broker.consume("log.detail.queue", "detailConsumer");

        System.out.println();
    }

    /**
     * 演示 Fanout Exchange：广播到所有绑定队列。
     * 创建一个 Fanout Exchange 和三个 Queue，发送一条消息，
     * 观察所有绑定队列都收到消息的广播行为。
     */
    static void demoFanoutExchange() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  Fanout Exchange 演示 — 广播到所有绑定队列");
        System.out.println("═══════════════════════════════════════════════════");

        Broker broker = new Broker();

        // 1. 声明 Exchange 和 Queue
        broker.declareExchange(new FanoutExchange("notification.fanout"));
        broker.declareQueue("email.queue");
        broker.declareQueue("sms.queue");
        broker.declareQueue("push.queue");

        // 2. 绑定：Fanout 忽略 bindingKey，这里传空字符串
        broker.bind("notification.fanout", "email.queue", "");
        broker.bind("notification.fanout", "sms.queue", "");
        broker.bind("notification.fanout", "push.queue", "");

        System.out.println("\n绑定关系：");
        System.out.println("  notification.fanout → email.queue (bindingKey 被忽略)");
        System.out.println("  notification.fanout → sms.queue   (bindingKey 被忽略)");
        System.out.println("  notification.fanout → push.queue  (bindingKey 被忽略)");

        // 3. 发布消息：routingKey 会被 Fanout Exchange 忽略
        System.out.println("\n发布消息（routingKey 被忽略）：");
        broker.publish("notification.fanout",
                new Message("any.key.ignored", "系统公告：今晚22:00进行系统维护"));

        // 4. 消费消息，验证广播效果
        System.out.println("\n消费结果（所有队列都收到同一条消息）：");
        broker.consume("email.queue", "emailConsumer");
        broker.consume("sms.queue", "smsConsumer");
        broker.consume("push.queue", "pushConsumer");

        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  RabbitMQ 核心概念演示（混合模式）                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟（直接运行） =====
        System.out.println("══════════ Part A：内存模拟 Exchange 路由 ══════════");
        System.out.println();

        demoDirectExchange();
        demoTopicExchange();
        demoFanoutExchange();

        // 补充演示：Topic 通配符匹配的边界情况
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  补充：Topic 通配符匹配验证");
        System.out.println("═══════════════════════════════════════════════════");
        String[][] testCases = {
                {"order.#",     "order",              "true"},
                {"order.#",     "order.create",       "true"},
                {"order.#",     "order.item.create",  "true"},
                {"order.*",     "order.create",       "true"},
                {"order.*",     "order.item.create",  "false"},
                {"*.create",    "order.create",       "true"},
                {"*.create",    "order.item.create",  "false"},
                {"#",           "any.routing.key",    "true"},
                {"log.*.detail","log.error.detail",   "true"},
                {"log.*.detail","log.error",          "false"},
        };
        System.out.println();
        System.out.printf("  %-20s %-22s %-8s %-8s%n", "Pattern", "RoutingKey", "期望", "实际");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 62; i++) sb.append('-');
        System.out.println("  " + sb);
        for (String[] tc : testCases) {
            boolean actual = TopicExchange.topicMatch(tc[0], tc[1]);
            boolean expected = Boolean.parseBoolean(tc[2]);
            String status = actual == expected ? "✓" : "✗ BUG";
            System.out.printf("  %-20s %-22s %-8s %-8s %s%n",
                    tc[0], tc[1], expected, actual, status);
        }
        System.out.println();

        // ===== Part B：连接真实 RabbitMQ =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：连接真实 RabbitMQ ══════════");
            System.out.println();
            RealRabbitMQ.run();
        } else {
            System.out.println("提示：运行 Part B（真实 RabbitMQ）请传入参数 'real'");
            System.out.println("  java -cp ... com.example.mq.rabbitmq.core.RabbitMQCoreDemo real");
        }
    }

    // ==================== Part B：真实 RabbitMQ 操作 ====================

    /**
     * Part B：使用 amqp-client 连接真实 RabbitMQ，演示 Exchange 声明、Queue 绑定、消息发布和消费。
     * 需要先启动 RabbitMQ：docker compose -f docker/docker-compose.mq.yml up -d rabbitmq
     * 管理界面：http://localhost:15672（guest/guest）
     */
    static class RealRabbitMQ {

        static void run() throws Exception {
            // 1. 创建连接
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("guest");
            factory.setPassword("guest");

            try (com.rabbitmq.client.Connection connection = factory.newConnection();
                 com.rabbitmq.client.Channel channel = connection.createChannel()) {

                demoRealDirect(channel);
                demoRealTopic(channel);
                demoRealFanout(channel);
            }
        }

        /** Direct Exchange：精确路由 */
        static void demoRealDirect(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — Direct Exchange】");

            String exchange = "demo.direct";
            String queue1 = "demo.order.create";
            String queue2 = "demo.order.pay";

            // 声明 Exchange 和 Queue
            channel.exchangeDeclare(exchange, "direct", false, true, null);
            channel.queueDeclare(queue1, false, false, true, null);
            channel.queueDeclare(queue2, false, false, true, null);

            // 绑定
            channel.queueBind(queue1, exchange, "order.create");
            channel.queueBind(queue2, exchange, "order.pay");

            // 发布消息
            channel.basicPublish(exchange, "order.create", null, "创建订单 #1001".getBytes("UTF-8"));
            channel.basicPublish(exchange, "order.pay", null, "支付订单 #1001".getBytes("UTF-8"));
            channel.basicPublish(exchange, "order.cancel", null, "取消订单（无匹配队列）".getBytes("UTF-8"));
            System.out.println("    发布 3 条消息（order.create / order.pay / order.cancel）");

            Thread.sleep(100); // 等待消息路由

            // 消费
            com.rabbitmq.client.GetResponse r1 = channel.basicGet(queue1, true);
            com.rabbitmq.client.GetResponse r2 = channel.basicGet(queue2, true);
            System.out.printf("    %s 收到: %s%n", queue1, r1 != null ? new String(r1.getBody(), "UTF-8") : "空");
            System.out.printf("    %s 收到: %s%n", queue2, r2 != null ? new String(r2.getBody(), "UTF-8") : "空");
            System.out.println("    order.cancel 无匹配队列，消息被丢弃");

            // 清理（可注释掉以保留 Exchange/Queue，方便在管理界面查看）
            // channel.exchangeDelete(exchange);
            cleanup(channel, exchange);
            System.out.println();
        }

        /** 清理 Exchange。如需保留在 RabbitMQ 管理界面查看，注释掉 cleanup() 调用即可 */
        static void cleanup(com.rabbitmq.client.Channel channel, String exchange) throws Exception {
            channel.exchangeDelete(exchange);
            System.out.println("    提示：如需保留 Exchange，注释掉 cleanup() 调用即可");
        }

        /** Topic Exchange：通配符路由 */
        static void demoRealTopic(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — Topic Exchange】");

            String exchange = "demo.topic";
            String queueAll = "demo.log.all";
            String queueError = "demo.log.error";

            channel.exchangeDeclare(exchange, "topic", false, true, null);
            channel.queueDeclare(queueAll, false, false, true, null);
            channel.queueDeclare(queueError, false, false, true, null);

            channel.queueBind(queueAll, exchange, "log.#");       // 匹配所有 log 开头
            channel.queueBind(queueError, exchange, "log.error");  // 只匹配 log.error

            channel.basicPublish(exchange, "log.info", null, "普通日志".getBytes("UTF-8"));
            channel.basicPublish(exchange, "log.error", null, "错误日志".getBytes("UTF-8"));
            channel.basicPublish(exchange, "log.error.detail", null, "错误详情".getBytes("UTF-8"));
            System.out.println("    发布 3 条消息（log.info / log.error / log.error.detail）");

            Thread.sleep(100);

            // 统计各队列消息数
            int allCount = 0;
            while (channel.basicGet(queueAll, true) != null) allCount++;
            int errorCount = 0;
            while (channel.basicGet(queueError, true) != null) errorCount++;

            System.out.printf("    %s (log.#) 收到: %d 条（匹配所有）%n", queueAll, allCount);
            System.out.printf("    %s (log.error) 收到: %d 条（仅精确匹配）%n", queueError, errorCount);

            // 清理（如需保留在管理界面查看，注释掉即可）
            cleanup(channel, exchange);
            System.out.println();
        }

        /** Fanout Exchange：广播 */
        static void demoRealFanout(com.rabbitmq.client.Channel channel) throws Exception {
            System.out.println("  【真实 RabbitMQ — Fanout Exchange】");

            String exchange = "demo.fanout";
            String q1 = "demo.email";
            String q2 = "demo.sms";
            String q3 = "demo.push";

            channel.exchangeDeclare(exchange, "fanout", false, true, null);
            channel.queueDeclare(q1, false, false, true, null);
            channel.queueDeclare(q2, false, false, true, null);
            channel.queueDeclare(q3, false, false, true, null);

            channel.queueBind(q1, exchange, "");
            channel.queueBind(q2, exchange, "");
            channel.queueBind(q3, exchange, "");

            channel.basicPublish(exchange, "", null, "系统公告：今晚维护".getBytes("UTF-8"));
            System.out.println("    发布 1 条广播消息");

            Thread.sleep(100);

            // 验证所有队列都收到
            for (String q : new String[]{q1, q2, q3}) {
                com.rabbitmq.client.GetResponse r = channel.basicGet(q, true);
                System.out.printf("    %s 收到: %s%n", q,
                        r != null ? new String(r.getBody(), "UTF-8") : "空");
            }

            // 清理（如需保留在管理界面查看，注释掉即可）
            cleanup(channel, exchange);
            System.out.println();
        }
    }
}

package com.example.mqtt;

/**
 * MQTT 协议演示（混合模式）
 *
 * <p>Part A：用观察者模式模拟 MQTT 发布订阅 + QoS 级别（直接运行）
 * <ul>
 *   <li>发布/订阅模型（Topic 通配符：+ 和 #）</li>
 *   <li>QoS 0/1/2 三种服务质量级别</li>
 *   <li>遗嘱消息（Last Will and Testament）</li>
 *   <li>保留消息（Retained Message）</li>
 *   <li>会话保持（Clean Session）</li>
 * </ul>
 *
 * <p>Part B：用 Eclipse Paho MQTT 客户端连接真实 MQTT Broker
 *
 * <p>Part B 运行前启动 MQTT Broker（可用 RabbitMQ 的 MQTT 插件或 Mosquitto）：
 * {@code docker compose -f docker/docker-compose.mq.yml up -d rabbitmq}
 * <p>RabbitMQ 默认启用 MQTT 插件，端口 1883
 *
 * <h3>MQTT 核心概念：</h3>
 * <pre>
 *  Publisher → Broker → Subscriber
 *
 *  Topic 层级：home/living-room/temperature
 *  通配符：
 *    + 匹配单层：home/+/temperature → home/living-room/temperature ✓
 *                                    → home/bedroom/temperature ✓
 *                                    → home/living-room/humidity ✗
 *    # 匹配多层：home/# → home/living-room/temperature ✓
 *                        → home/bedroom/humidity ✓
 *                        → home ✓
 *
 *  QoS 级别：
 *    QoS 0：最多一次（Fire and Forget）
 *    QoS 1：至少一次（可能重复）
 *    QoS 2：恰好一次（四次握手）
 * </pre>
 */
public class MQTTDemo {

    // ==================== Part A：模拟 MQTT Broker ====================

    /** 模拟 MQTT 消息 */
    static class MqttMessage {
        final String topic;
        final String payload;
        final int qos;
        final boolean retained;
        final String clientId;

        MqttMessage(String topic, String payload, int qos, boolean retained, String clientId) {
            this.topic = topic;
            this.payload = payload;
            this.qos = qos;
            this.retained = retained;
            this.clientId = clientId;
        }
    }

    /** 模拟 MQTT 订阅者 */
    static class Subscriber {
        final String clientId;
        final String topicFilter;   // 订阅的 Topic（可含通配符）
        final int qos;
        final java.util.List<MqttMessage> received = new java.util.ArrayList<>();

        Subscriber(String clientId, String topicFilter, int qos) {
            this.clientId = clientId;
            this.topicFilter = topicFilter;
            this.qos = qos;
        }

        void onMessage(MqttMessage msg) {
            // 实际 QoS = min(发布 QoS, 订阅 QoS)
            int effectiveQos = Math.min(msg.qos, this.qos);
            received.add(msg);
            System.out.printf("    [%s] 收到 topic=%s, payload=%s, QoS=%d(effective)%n",
                    clientId, msg.topic, msg.payload, effectiveQos);
        }
    }

    /** 模拟 MQTT Broker */
    static class SimulatedBroker {
        private final java.util.List<Subscriber> subscribers = new java.util.ArrayList<>();
        // 保留消息：topic → 最新的 retained message
        private final java.util.Map<String, MqttMessage> retainedMessages = new java.util.LinkedHashMap<>();
        // 遗嘱消息：clientId → will message
        private final java.util.Map<String, MqttMessage> willMessages = new java.util.LinkedHashMap<>();
        // QoS 1/2 的消息确认计数
        private final java.util.Map<String, Integer> ackCounts = new java.util.concurrent.ConcurrentHashMap<>();

        /** 客户端连接（可设置遗嘱消息） */
        void connect(String clientId, MqttMessage willMessage) {
            if (willMessage != null) {
                willMessages.put(clientId, willMessage);
            }
            System.out.printf("    [Broker] 客户端 %s 已连接%n", clientId);
        }

        /** 客户端异常断开 → 发布遗嘱消息 */
        void disconnectUnexpectedly(String clientId) {
            MqttMessage will = willMessages.remove(clientId);
            if (will != null) {
                System.out.printf("    [Broker] 客户端 %s 异常断开，发布遗嘱消息%n", clientId);
                publish(will);
            }
        }

        /** 订阅 */
        void subscribe(Subscriber subscriber) {
            subscribers.add(subscriber);
            // 发送保留消息给新订阅者
            for (var entry : retainedMessages.entrySet()) {
                if (topicMatch(subscriber.topicFilter, entry.getKey())) {
                    subscriber.onMessage(entry.getValue());
                }
            }
        }

        /** 发布消息 */
        void publish(MqttMessage message) {
            // 保留消息处理
            if (message.retained) {
                if (message.payload.isEmpty()) {
                    retainedMessages.remove(message.topic); // 空 payload 清除保留消息
                } else {
                    retainedMessages.put(message.topic, message);
                }
            }

            // QoS 模拟
            String msgId = message.topic + "-" + System.nanoTime();
            switch (message.qos) {
                case 0:
                    // QoS 0：直接投递，不确认
                    break;
                case 1:
                    // QoS 1：Broker 发送 PUBACK
                    ackCounts.merge(msgId, 1, Integer::sum);
                    break;
                case 2:
                    // QoS 2：四次握手（PUBREC → PUBREL → PUBCOMP）
                    ackCounts.merge(msgId, 3, Integer::sum);
                    break;
            }

            // 匹配订阅者并投递
            for (Subscriber sub : subscribers) {
                if (topicMatch(sub.topicFilter, message.topic)) {
                    sub.onMessage(message);
                }
            }
        }

        /**
         * MQTT Topic 通配符匹配。
         * + 匹配单层，# 匹配零层或多层（只能在末尾）
         */
        static boolean topicMatch(String filter, String topic) {
            String[] filterParts = filter.split("/");
            String[] topicParts = topic.split("/");

            for (int i = 0; i < filterParts.length; i++) {
                if (filterParts[i].equals("#")) {
                    return true; // # 匹配剩余所有层级
                }
                if (i >= topicParts.length) {
                    return false;
                }
                if (!filterParts[i].equals("+") && !filterParts[i].equals(topicParts[i])) {
                    return false;
                }
            }
            return filterParts.length == topicParts.length;
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：发布/订阅 + Topic 通配符 */
    static void demoPubSub() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：发布/订阅 + Topic 通配符匹配");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedBroker broker = new SimulatedBroker();

        // 订阅者
        Subscriber sub1 = new Subscriber("sensor-monitor", "home/+/temperature", 1);
        Subscriber sub2 = new Subscriber("home-dashboard", "home/#", 0);
        Subscriber sub3 = new Subscriber("bedroom-monitor", "home/bedroom/+", 1);

        broker.subscribe(sub1);
        broker.subscribe(sub2);
        broker.subscribe(sub3);

        System.out.println("\n  订阅关系：");
        System.out.println("    sensor-monitor  → home/+/temperature（单层通配）");
        System.out.println("    home-dashboard  → home/#（多层通配）");
        System.out.println("    bedroom-monitor → home/bedroom/+（卧室所有传感器）");

        // 发布消息
        System.out.println("\n  发布消息：");
        System.out.println("  → home/living-room/temperature = 26.5°C");
        broker.publish(new MqttMessage("home/living-room/temperature", "26.5°C", 1, false, "sensor-1"));

        System.out.println("\n  → home/bedroom/temperature = 24.0°C");
        broker.publish(new MqttMessage("home/bedroom/temperature", "24.0°C", 1, false, "sensor-2"));

        System.out.println("\n  → home/bedroom/humidity = 60%");
        broker.publish(new MqttMessage("home/bedroom/humidity", "60%", 0, false, "sensor-3"));

        System.out.println("\n  匹配结果：");
        System.out.printf("    sensor-monitor  收到 %d 条（只匹配 temperature）%n", sub1.received.size());
        System.out.printf("    home-dashboard  收到 %d 条（匹配所有 home/ 开头）%n", sub2.received.size());
        System.out.printf("    bedroom-monitor 收到 %d 条（只匹配 bedroom 下的）%n", sub3.received.size());
        System.out.println();
    }

    /** 演示2：QoS 0/1/2 对比 */
    static void demoQoS() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：QoS 0/1/2 三种服务质量级别");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println();
        System.out.printf("  %-8s %-15s %-15s %-20s%n", "QoS", "语义", "握手次数", "适用场景");
        System.out.println("  " + "─".repeat(60));
        System.out.printf("  %-8s %-15s %-15s %-20s%n", "QoS 0", "最多一次", "0（Fire&Forget）", "传感器数据（允许丢失）");
        System.out.printf("  %-8s %-15s %-15s %-20s%n", "QoS 1", "至少一次", "2（PUBLISH+PUBACK）", "消息通知（可重复）");
        System.out.printf("  %-8s %-15s %-15s %-20s%n", "QoS 2", "恰好一次", "4（四次握手）", "支付/计费（不可重复）");

        System.out.println("\n  QoS 2 四次握手流程：");
        System.out.println("    Publisher → Broker: PUBLISH");
        System.out.println("    Broker → Publisher: PUBREC（已收到）");
        System.out.println("    Publisher → Broker: PUBREL（请释放）");
        System.out.println("    Broker → Publisher: PUBCOMP（已完成）");

        System.out.println("\n  实际 QoS = min(发布 QoS, 订阅 QoS)");
        System.out.println("  例：Publisher QoS=2, Subscriber QoS=1 → 实际 QoS=1");
        System.out.println();
    }

    /** 演示3：遗嘱消息 + 保留消息 */
    static void demoWillAndRetained() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：遗嘱消息 + 保留消息");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedBroker broker = new SimulatedBroker();

        // 设备连接时设置遗嘱消息
        System.out.println("\n  【遗嘱消息】设备连接时设置 Will Message：");
        MqttMessage will = new MqttMessage("device/sensor-1/status", "offline", 1, true, "sensor-1");
        broker.connect("sensor-1", will);

        // 订阅设备状态
        Subscriber monitor = new Subscriber("monitor", "device/+/status", 1);
        broker.subscribe(monitor);

        // 发布在线状态（保留消息）
        System.out.println("\n  发布保留消息：device/sensor-1/status = online");
        broker.publish(new MqttMessage("device/sensor-1/status", "online", 1, true, "sensor-1"));

        // 新订阅者加入 → 立即收到保留消息
        System.out.println("\n  【保留消息】新订阅者加入，立即收到最新保留消息：");
        Subscriber newSub = new Subscriber("new-monitor", "device/+/status", 1);
        broker.subscribe(newSub);

        // 设备异常断开 → 发布遗嘱消息
        System.out.println("\n  设备 sensor-1 异常断开：");
        broker.disconnectUnexpectedly("sensor-1");

        System.out.println("\n  遗嘱消息应用场景：");
        System.out.println("    1. IoT 设备在线/离线状态监控");
        System.out.println("    2. 聊天应用用户在线状态");
        System.out.println("    3. 分布式系统节点健康检测");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MQTT 协议演示（混合模式）                              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟 MQTT 发布订阅 ══════════");
        System.out.println();
        demoPubSub();
        demoQoS();
        demoWillAndRetained();

        // ===== Part B：连接真实 MQTT Broker =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：Paho MQTT 客户端 ══════════");
            System.out.println();
            RealMqtt.run();
        } else {
            System.out.println("提示：运行 Part B（真实 MQTT）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 MQTT ====================

    static class RealMqtt {

        static void run() throws Exception {
            String broker = "tcp://localhost:1883";
            String publisherId = "demo-publisher";
            String subscriberId = "demo-subscriber";

            // 1. 创建订阅者
            org.eclipse.paho.client.mqttv3.MqttClient subscriber =
                    new org.eclipse.paho.client.mqttv3.MqttClient(broker, subscriberId);
            org.eclipse.paho.client.mqttv3.MqttConnectOptions subOpts =
                    new org.eclipse.paho.client.mqttv3.MqttConnectOptions();
            subOpts.setCleanSession(true);
            subscriber.connect(subOpts);

            // 订阅 Topic
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(3);
            subscriber.subscribe("demo/+/data", 1, (topic, msg) -> {
                System.out.printf("    [Subscriber] topic=%s, payload=%s, QoS=%d%n",
                        topic, new String(msg.getPayload()), msg.getQos());
                latch.countDown();
            });
            System.out.println("  订阅: demo/+/data (QoS=1)");

            // 2. 创建发布者
            org.eclipse.paho.client.mqttv3.MqttClient publisher =
                    new org.eclipse.paho.client.mqttv3.MqttClient(broker, publisherId);
            publisher.connect();

            // 发布消息
            System.out.println("\n  发布消息：");
            String[][] messages = {
                    {"demo/sensor-1/data", "温度=26.5°C"},
                    {"demo/sensor-2/data", "湿度=60%"},
                    {"demo/sensor-1/data", "温度=27.0°C"},
            };

            for (String[] m : messages) {
                org.eclipse.paho.client.mqttv3.MqttMessage msg =
                        new org.eclipse.paho.client.mqttv3.MqttMessage(m[1].getBytes("UTF-8"));
                msg.setQos(1);
                publisher.publish(m[0], msg);
                System.out.printf("    [Publisher] topic=%s, payload=%s%n", m[0], m[1]);
            }

            // 等待消息接收
            boolean received = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            System.out.printf("\n  消息接收: %s%n", received ? "✓ 全部收到" : "⚠ 超时");

            // 清理（如需保留连接观察，注释掉即可）
            publisher.disconnect();
            subscriber.disconnect();
            publisher.close();
            subscriber.close();
            System.out.println("  提示：如需保留 MQTT 连接，注释掉 disconnect/close 即可");
        }
    }
}

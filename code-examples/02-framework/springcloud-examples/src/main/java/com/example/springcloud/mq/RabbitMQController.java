package com.example.springcloud.mq;

import com.example.springcloud.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 10.A.15 RabbitMQ 消息队列实战 Controller
 *
 * <p>演示 RabbitMQ 的核心功能：
 * <ul>
 *   <li>TopicExchange + Queue + Binding 自动声明</li>
 *   <li>RabbitTemplate 发送消息</li>
 *   <li>@RabbitListener 自动消费消息</li>
 *   <li>消息发送/消费时间记录</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 发送消息
 * curl -X POST "http://localhost:8090/demo/mq/rabbit/send?msg=hello-rabbit"
 *
 * # 查看消费历史
 * curl http://localhost:8090/demo/mq/rabbit/history
 * </pre>
 */
@RestController
@RequestMapping("/demo/mq/rabbit")
public class RabbitMQController {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQController.class);

    /** Exchange 名称 */
    public static final String EXCHANGE_NAME = "demo.exchange";
    /** Queue 名称 */
    public static final String QUEUE_NAME = "demo.queue";
    /** Routing Key */
    public static final String ROUTING_KEY = "demo.routing.#";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 消费历史记录（线程安全，最多保留 100 条） */
    private final CopyOnWriteArrayList<Map<String, Object>> consumeHistory = new CopyOnWriteArrayList<>();

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送消息到 RabbitMQ
     *
     * <pre>
     * curl -X POST "http://localhost:8090/demo/mq/rabbit/send?msg=hello-rabbit"
     * </pre>
     *
     * @param msg 消息内容
     * @return 发送结果
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestParam String msg) {
        String sendTime = LocalDateTime.now().format(FMT);
        // 将发送时间附加到消息体中，方便消费端记录
        String payload = msg + "|sendTime=" + sendTime;

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "demo.routing.test", payload);
        log.info("[RabbitMQ] 消息已发送: msg={}, sendTime={}", msg, sendTime);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "消息已发送到 RabbitMQ");
        data.put("exchange", EXCHANGE_NAME);
        data.put("routingKey", "demo.routing.test");
        data.put("消息内容", msg);
        data.put("发送时间", sendTime);
        return Result.ok(data);
    }

    /**
     * 查看已消费的消息历史
     *
     * <pre>
     * curl http://localhost:8090/demo/mq/rabbit/history
     * </pre>
     *
     * @return 消费历史列表
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> history() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "RabbitMQ 消费历史（最近 100 条）");
        data.put("总条数", consumeHistory.size());
        data.put("消息列表", consumeHistory);
        return Result.ok(data);
    }

    /**
     * RabbitMQ 消费者 — 自动监听 demo.queue 队列
     *
     * @param message 接收到的消息
     */
    @RabbitListener(queues = QUEUE_NAME)
    public void consume(String message) {
        String consumeTime = LocalDateTime.now().format(FMT);
        log.info("[RabbitMQ] 收到消息: {}, consumeTime={}", message, consumeTime);

        // 解析发送时间
        String msgContent = message;
        String sendTime = "未知";
        if (message.contains("|sendTime=")) {
            String[] parts = message.split("\\|sendTime=");
            msgContent = parts[0];
            sendTime = parts[1];
        }

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("消息内容", msgContent);
        record.put("发送时间", sendTime);
        record.put("消费时间", consumeTime);
        record.put("队列", QUEUE_NAME);

        consumeHistory.add(record);

        // 保留最近 100 条
        while (consumeHistory.size() > 100) {
            consumeHistory.remove(0);
        }
    }

    /**
     * RabbitMQ 配置类 — 自动声明 Exchange、Queue、Binding
     */
    @Configuration
    static class RabbitConfig {

        /** 声明 TopicExchange */
        @Bean
        public TopicExchange demoExchange() {
            return new TopicExchange(EXCHANGE_NAME, true, false);
        }

        /** 声明 Queue */
        @Bean
        public Queue demoQueue() {
            return QueueBuilder.durable(QUEUE_NAME).build();
        }

        /** 绑定 Queue 到 Exchange */
        @Bean
        public Binding demoBinding(Queue demoQueue, TopicExchange demoExchange) {
            return BindingBuilder.bind(demoQueue).to(demoExchange).with(ROUTING_KEY);
        }
    }
}

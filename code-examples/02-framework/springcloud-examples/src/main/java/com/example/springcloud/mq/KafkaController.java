package com.example.springcloud.mq;

import com.example.springcloud.common.Result;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 10.A.16 Kafka 消息队列实战 Controller
 *
 * <p>演示 Kafka 的核心功能：
 * <ul>
 *   <li>KafkaTemplate 发送消息到指定 topic</li>
 *   <li>@KafkaListener 自动消费消息</li>
 *   <li>展示 partition、offset 等元数据</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 发送消息
 * curl -X POST "http://localhost:8090/demo/mq/kafka/send?msg=hello-kafka"
 *
 * # 查看消费历史
 * curl http://localhost:8090/demo/mq/kafka/history
 * </pre>
 */
@RestController
@RequestMapping("/demo/mq/kafka")
public class KafkaController {

    private static final Logger log = LoggerFactory.getLogger(KafkaController.class);

    /** Topic 名称 */
    private static final String TOPIC = "demo-topic";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 消费历史记录（线程安全，最多保留 100 条） */
    private final CopyOnWriteArrayList<Map<String, Object>> consumeHistory = new CopyOnWriteArrayList<>();

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送消息到 Kafka
     *
     * <pre>
     * curl -X POST "http://localhost:8090/demo/mq/kafka/send?msg=hello-kafka"
     * </pre>
     *
     * @param msg 消息内容
     * @return 发送结果
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestParam String msg) {
        String sendTime = LocalDateTime.now().format(FMT);
        String key = UUID.randomUUID().toString().substring(0, 8);

        kafkaTemplate.send(TOPIC, key, msg);
        log.info("[Kafka] 消息已发送: topic={}, key={}, msg={}, sendTime={}", TOPIC, key, msg, sendTime);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "消息已发送到 Kafka");
        data.put("topic", TOPIC);
        data.put("key", key);
        data.put("消息内容", msg);
        data.put("发送时间", sendTime);
        return Result.ok(data);
    }

    /**
     * 查看已消费的消息历史
     *
     * <pre>
     * curl http://localhost:8090/demo/mq/kafka/history
     * </pre>
     *
     * @return 消费历史列表
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> history() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "Kafka 消费历史（最近 100 条）");
        data.put("总条数", consumeHistory.size());
        data.put("消息列表", consumeHistory);
        return Result.ok(data);
    }

    /**
     * Kafka 消费者 — 自动监听 demo-topic
     *
     * @param record Kafka 消费记录（包含 partition、offset 等元数据）
     */
    @KafkaListener(topics = TOPIC, groupId = "demo-group")
    public void consume(ConsumerRecord<String, String> record) {
        String consumeTime = LocalDateTime.now().format(FMT);
        String recordTimestamp = Instant.ofEpochMilli(record.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(FMT);

        log.info("[Kafka] 收到消息: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("消息内容", record.value());
        entry.put("key", record.key());
        entry.put("topic", record.topic());
        entry.put("partition", record.partition());
        entry.put("offset", record.offset());
        entry.put("Kafka时间戳", recordTimestamp);
        entry.put("消费时间", consumeTime);

        consumeHistory.add(entry);

        // 保留最近 100 条
        while (consumeHistory.size() > 100) {
            consumeHistory.remove(0);
        }
    }
}

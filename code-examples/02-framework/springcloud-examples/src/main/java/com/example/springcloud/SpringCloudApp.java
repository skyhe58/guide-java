package com.example.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Cloud 综合实战项目启动类
 *
 * 一个项目体验 Spring Cloud 全家桶：
 * - 注册发现（Consul/Nacos/ZK 可切换）
 * - 声明式调用（OpenFeign + Fallback）
 * - 熔断降级（Resilience4j）
 * - 消息队列（RabbitMQ + Kafka）
 * - 缓存（Redis + @Cacheable）
 * - 数据库（MySQL + JdbcTemplate）
 * - 搜索（Elasticsearch）
 * - 文档数据库（MongoDB）
 * - 文件存储（MinIO）
 * - 链路追踪（Micrometer Tracing）
 * - 定时任务（@Scheduled + Redis 分布式锁）
 *
 * 启动前需要启动中间件：
 * {@code docker compose -f docker/docker-compose.yml up -d}
 * {@code docker compose -f docker/docker-compose.consul.yml up -d}
 * {@code docker compose -f docker/docker-compose.mq.yml up -d}
 * {@code docker compose -f docker/docker-compose.es.yml up -d}
 *
 * 启动命令：
 * {@code mvn spring-boot:run}
 *
 * 切换注册中心：
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=nacos}
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=zk}
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class SpringCloudApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudApp.class, args);
    }
}

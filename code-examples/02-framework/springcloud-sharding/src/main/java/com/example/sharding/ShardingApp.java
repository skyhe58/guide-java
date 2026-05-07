package com.example.sharding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 分库分表服务启动类
 *
 * 独立模块原因：ShardingSphere 接管 DataSource，影响其他模块普通 SQL
 *
 * 功能：
 * - 单库分表：order_0~order_3，按 order_id 取模
 * - 分库分表：ds_0/ds_1 × order_0~order_3
 * - 读写分离：主写从读
 * - 分布式主键：雪花算法
 *
 * 启动前需要：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}
 * {@code docker compose -f docker/docker-compose.consul.yml up -d}
 *
 * 启动命令：
 * {@code mvn spring-boot:run}                                          # 单库分表
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=db-table}      # 分库分表
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=readwrite}     # 读写分离
 *
 * 端口：8091
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ShardingApp {

    public static void main(String[] args) {
        SpringApplication.run(ShardingApp.class, args);
    }
}

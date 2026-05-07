package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户微服务启动类
 *
 * 独立微服务，被 springcloud-examples 通过 Feign 调用
 * 演示完整微服务调用链路：
 * 客户端 → Gateway(8080) → springcloud-demo(8090) → [Feign] → user-service(8092)
 *
 * 功能：
 * - 用户 CRUD（JdbcTemplate + MySQL）
 * - Redis 缓存（用户信息缓存）
 * - 服务注册到 Consul
 *
 * 启动前需要：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql redis}
 * {@code docker compose -f docker/docker-compose.consul.yml up -d}
 *
 * 启动命令：
 * {@code mvn spring-boot:run}
 *
 * 端口：8092
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApp.class, args);
    }
}

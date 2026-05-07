package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway 网关启动类
 *
 * 独立模块原因：WebFlux 和 WebMVC 不能共存
 *
 * 路由规则：
 * - /api/demo/** → lb://springcloud-demo（业务服务）
 * - /api/user/** → lb://user-service（用户服务）
 * - /api/sharding/** → lb://sharding-service（分库分表服务）
 *
 * 全局过滤器：
 * - AuthGlobalFilter：鉴权（检查 Authorization 头）
 * - LogGlobalFilter：日志（记录请求路径、耗时）
 * - RateLimitFilter：限流（Redis 令牌桶）
 *
 * 启动前需要：
 * {@code docker compose -f docker/docker-compose.consul.yml up -d}
 * {@code docker compose -f docker/docker-compose.yml up -d redis}
 *
 * 启动命令：
 * {@code mvn spring-boot:run}
 *
 * 端口：8080
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApp {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApp.class, args);
    }
}

package com.example.springboot.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Actuator 监控与健康检查演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>自定义 HealthIndicator — 健康检查</li>
 *   <li>自定义 Endpoint — 自定义监控端点</li>
 * </ul>
 *
 * <p>访问方式：</p>
 * <ul>
 *   <li>健康检查：GET /actuator/health</li>
 *   <li>自定义端点：GET /actuator/appinfo</li>
 * </ul>
 */
public class ActuatorDemo {

    // ==================== 1. 自定义健康检查 ====================

    /**
     * 自定义健康检查指标
     *
     * <p>实现 HealthIndicator 接口，Spring Boot 会自动注册到 /actuator/health。
     * 可以检查外部依赖（数据库、Redis、消息队列等）的可用性。</p>
     */
    @Component
    public static class AppHealthIndicator implements HealthIndicator {

        @Override
        public Health health() {
            // 检查应用状态
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            double memoryUsage = 1.0 - (double) freeMemory / totalMemory;

            if (memoryUsage < 0.9) {
                return Health.up()
                        .withDetail("memoryUsage", String.format("%.1f%%", memoryUsage * 100))
                        .withDetail("freeMemory", formatBytes(freeMemory))
                        .withDetail("totalMemory", formatBytes(totalMemory))
                        .build();
            }
            return Health.down()
                    .withDetail("memoryUsage", String.format("%.1f%%", memoryUsage * 100))
                    .withDetail("reason", "内存使用率过高")
                    .build();
        }

        private String formatBytes(long bytes) {
            return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        }
    }

    // ==================== 2. 自定义端点 ====================

    /**
     * 自定义 Actuator 端点
     *
     * <p>通过 @Endpoint 注解创建自定义端点，
     * 访问路径为 /actuator/{id}。</p>
     *
     * <p>@ReadOperation 对应 GET 请求，
     * @WriteOperation 对应 POST 请求，
     * @DeleteOperation 对应 DELETE 请求。</p>
     */
    @Component
    @Endpoint(id = "appinfo")
    public static class AppInfoEndpoint {

        @ReadOperation
        public Map<String, Object> info() {
            Map<String, Object> info = new HashMap<>();
            info.put("appName", "Spring Boot Demo");
            info.put("version", "1.0.0");
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("startTime", LocalDateTime.now().toString());

            // JVM 运行时信息
            var runtime = ManagementFactory.getRuntimeMXBean();
            info.put("uptime", Duration.ofMillis(runtime.getUptime()).toString());
            info.put("pid", runtime.getPid());

            return info;
        }
    }
}

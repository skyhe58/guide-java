package com.example.springboot.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * 配置文件体系演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>@ConfigurationProperties 属性绑定</li>
 *   <li>松散绑定（camelCase / kebab-case / snake_case）</li>
 *   <li>@Validated 配置校验</li>
 *   <li>复杂类型绑定（List、Duration）</li>
 * </ul>
 *
 * <p>对应配置文件 application.yml 中 app 前缀的配置项。</p>
 */
@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class ConfigDemo {

    private static final Logger log = LoggerFactory.getLogger(ConfigDemo.class);

    /**
     * 应用名称 — 对应 app.name
     */
    @NotBlank(message = "应用名称不能为空")
    private String name = "springboot-demo";

    /**
     * 最大重试次数 — 对应 app.max-retry（松散绑定）
     */
    @Min(1)
    @Max(10)
    private int maxRetry = 3;

    /**
     * 超时时间 — 对应 app.timeout，支持 Duration 格式（如 30s、5m）
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 服务器列表 — 对应 app.servers，支持 List 类型
     */
    private List<String> servers = List.of("localhost");

    /**
     * 打印当前配置信息
     */
    public void printConfig() {
        log.info("=== 应用配置信息 ===");
        log.info("应用名称: {}", name);
        log.info("最大重试次数: {}", maxRetry);
        log.info("超时时间: {}", timeout);
        log.info("服务器列表: {}", servers);
    }

    // Getter / Setter

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }
}

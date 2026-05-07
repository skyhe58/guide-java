package com.example.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 主启动类
 *
 * <p>演示 Spring Boot 应用的入口，集成了缓存和定时任务功能。
 * 启动后可通过 http://localhost:8080 访问。</p>
 *
 * @see com.example.springboot.ioc.IoCDemo
 * @see com.example.springboot.aop.AopDemo
 * @see com.example.springboot.web.WebDemo
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class SpringBootApp {

    public static void main(String[] args) {
        var context = SpringApplication.run(SpringBootApp.class, args);
        System.out.println("=== Spring Boot 应用启动成功 ===");
        System.out.println("Bean 总数: " + context.getBeanDefinitionCount());
    }
}

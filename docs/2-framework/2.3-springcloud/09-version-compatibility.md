---
title: "Spring Boot 与 Spring Cloud 版本兼容性对照表"
module: "springcloud"
difficulty: "beginner"
interviewFrequency: "medium"
tags:
  - "版本兼容性"
  - "Spring Boot"
  - "Spring Cloud"
codeExample: ""
relatedEntries:
  - "/2-framework/2.3-springcloud/00-index"
  - "/2-framework/2.2-springboot/"
prerequisites: []
estimatedTime: "15min"
---

# Spring Boot 与 Spring Cloud 版本兼容性对照表

## 概念说明

Spring Cloud 的版本号与 Spring Boot 的版本号有严格的对应关系。使用不兼容的版本组合会导致各种奇怪的问题（启动失败、功能异常等）。在创建项目时，**必须先确认版本兼容性**。

> ⚠️ 这是新手最容易踩的坑之一：Spring Boot 和 Spring Cloud 版本不匹配。

## 版本对照表

### Spring Boot 3.x 与 Spring Cloud 2023.x/2022.x

| Spring Cloud 版本 | Spring Boot 版本 | 发布时间 | 说明 |
|-------------------|-----------------|----------|------|
| **2023.0.x (Leyton)** | **3.2.x** | 2023-12 | 当前推荐版本 ✅ |
| 2023.0.0 | 3.2.0 - 3.2.x | 2023-12 | |
| 2023.0.1 | 3.2.3 - 3.2.x | 2024-03 | 本项目使用版本 |
| 2023.0.2 | 3.2.5 - 3.2.x | 2024-05 | |
| **2022.0.x (Kilburn)** | **3.0.x - 3.1.x** | 2022-12 | |
| 2022.0.0 | 3.0.0 - 3.0.x | 2022-12 | |
| 2022.0.4 | 3.0.9 - 3.1.x | 2023-07 | |
| 2022.0.5 | 3.0.13 - 3.1.x | 2023-11 | |

### Spring Boot 2.x 与 Spring Cloud（历史版本）

| Spring Cloud 版本 | Spring Boot 版本 | 说明 |
|-------------------|-----------------|------|
| 2021.0.x (Jubilee) | 2.6.x - 2.7.x | |
| 2020.0.x (Ilford) | 2.4.x - 2.5.x | Ribbon 被移除 |
| Hoxton.SR12 | 2.3.x | |
| Greenwich.SR6 | 2.1.x | |
| Finchley.SR4 | 2.0.x | |

### 版本命名规则变化

Spring Cloud 的版本命名经历了两次变化：

| 阶段 | 命名方式 | 示例 |
|------|----------|------|
| 早期 | 伦敦地铁站名（字母序） | Angel → Brixton → Camden → ... → Hoxton |
| 过渡期 | 年份.次版本号 | 2020.0.x → 2021.0.x → 2022.0.x |
| 当前 | 年份.次版本号 + 代号 | 2023.0.x (Leyton) |

## 核心组件版本对应

### Spring Cloud 2023.0.x 组件版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Cloud Gateway | 4.1.x | API 网关 |
| Spring Cloud OpenFeign | 4.1.x | 声明式 HTTP 客户端 |
| Spring Cloud LoadBalancer | 4.1.x | 客户端负载均衡 |
| Spring Cloud Consul | 4.1.x | Consul 集成 |
| Spring Cloud CircuitBreaker | 3.1.x | 熔断器抽象 |

### Spring Cloud Alibaba 版本对应

| Spring Cloud Alibaba | Spring Cloud | Spring Boot | 说明 |
|----------------------|-------------|-------------|------|
| 2023.0.1.x | 2023.0.x | 3.2.x | 最新版本 |
| 2022.0.0.0 | 2022.0.x | 3.0.x | |
| 2021.0.5.0 | 2021.0.x | 2.6.x - 2.7.x | |

## 版本选择建议

### 新项目推荐

```xml
<!-- 推荐版本组合（本项目使用） -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2023.0.1</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 版本升级注意事项

| 升级路径 | 关键变化 |
|----------|----------|
| Boot 2.x → 3.x | Jakarta EE（javax → jakarta）、Java 17+ 必须 |
| Cloud 2021 → 2022 | Sleuth → Micrometer Tracing |
| Cloud 2020 → 2021 | Ribbon → Spring Cloud LoadBalancer |
| Cloud Hoxton → 2020 | Netflix 组件大量移除 |

## 常见面试题

### Q1: Spring Boot 和 Spring Cloud 的版本如何对应？

**难度**：⭐⭐ | **频率**：🔥🔥

**答题思路**：

1. 说明版本对应关系
2. 说明版本命名规则的变化
3. 给出当前推荐版本

**标准答案**：

Spring Cloud 的版本与 Spring Boot 有严格的对应关系。当前推荐组合是 Spring Boot 3.2.x + Spring Cloud 2023.0.x。Spring Cloud 的版本命名从早期的伦敦地铁站名（如 Hoxton）变为年份制（如 2023.0.x）。选择版本时应参考 Spring Cloud 官方的版本兼容性矩阵，使用不兼容的版本会导致启动失败或功能异常。

**深入追问**：

- Spring Boot 2.x 升级到 3.x 有哪些重大变化？
- Spring Cloud Netflix 组件为什么被移除了？

### Q2: Spring Boot 3.x 相比 2.x 有哪些重大变化？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

Spring Boot 3.x 的重大变化包括：（1）最低要求 Java 17；（2）Jakarta EE 迁移（javax.* 包名改为 jakarta.*）；（3）Spring Cloud Sleuth 被 Micrometer Tracing 替代；（4）GraalVM 原生镜像支持；（5）部分自动配置类路径变化。升级时需要注意包名替换和依赖更新。

**深入追问**：

- javax 到 jakarta 的迁移会影响哪些代码？

### Q3: 如何查看当前项目使用的 Spring Cloud 版本？

**难度**：⭐ | **频率**：🔥

**标准答案**：

三种方式：（1）查看 pom.xml 中的 `spring-cloud.version` 属性或 `spring-cloud-dependencies` 的版本号；（2）启动应用时查看控制台日志中的 Spring Cloud 版本信息；（3）通过 Actuator 的 `/actuator/info` 端点查看。

## 参考资料

- [Spring Cloud 版本兼容性官方说明](https://spring.io/projects/spring-cloud)
- [Spring Cloud Alibaba 版本说明](https://github.com/alibaba/spring-cloud-alibaba/wiki/版本说明)

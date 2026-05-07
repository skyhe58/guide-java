---
layout: home

hero:
  name: Java 知识库
  text: 系统化的 Java 技术知识体系
  tagline: 从基础到架构，从原理到面试，循序渐进掌握 Java 全栈技术
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/getting-started
    - theme: alt
      text: 学习路径
      link: /learning-paths/beginner
    - theme: alt
      text: GitHub
      link: https://github.com

features:
  - icon: ☕
    title: 第一层：语言基础
    details: Java 基础语法、面向对象、集合框架、泛型、反射、注解、IO/NIO、Lambda 与 Stream、JDK 新特性
    link: /1-java-core/1.1-java-basics/01-data-types
    linkText: 开始学习

  - icon: 🔬
    title: 第二层：语言深入
    details: 并发编程、JVM 原理、Java 进阶（源码分析/动态代理/SPI）、设计模式、数据结构与算法
    link: /1-java-core/1.3-concurrent/01-thread-lifecycle
    linkText: 深入探索

  - icon: 🚀
    title: 第三层：框架应用
    details: Spring Boot 全面解析、MySQL 深入、网络与协议，掌握主流框架和工具
    link: /2-framework/2.2-springboot/01-ioc-di
    linkText: 框架实战

  - icon: 🌐
    title: 第四层：分布式体系
    details: Spring Cloud 微服务、Redis、消息队列、注册中心（Consul 优先）、配置中心
    link: /2-framework/2.3-springcloud/01-registry
    linkText: 分布式架构

  - icon: 🏗️
    title: 第五层：综合进阶
    details: Elasticsearch、Nginx、分布式理论、Docker/K8s、AI 应用、架构设计场景
    link: /3-data-store/3.3-elasticsearch/01-inverted-index
    linkText: 进阶提升

  - icon: 🎯
    title: 面试准备
    details: 每个模块配备面试指南，高频题目、答题思路、深入追问，助你高效备战
    link: /learning-paths/interview-sprint
    linkText: 面试突击
---

## 📚 知识库概览

本知识库按照**循序渐进**的学习理念，将 Java 技术体系划分为五个层次：

| 层次 | 模块 | 说明 |
|------|------|------|
| 🟢 语言基础 | Java 基础 | 打牢根基，掌握核心语法和 API |
| 🔵 语言深入 | 并发 / JVM / 进阶 / 设计模式 / 算法 | 深入底层原理，理解"为什么" |
| 🟡 框架应用 | Spring Boot / 数据库 / 网络 | 掌握主流框架，解决"怎么用" |
| 🟠 分布式体系 | Spring Cloud / Redis / MQ / 注册中心 | 进入分布式领域，理解微服务架构 |
| 🔴 综合进阶 | ES / Nginx / 分布式理论 / Docker / AI / 架构 | 综合运用，面向实际场景和面试 |

## 💻 代码示例

所有知识点都配有可独立运行的代码示例，位于 `code-examples/` 目录下的 Maven 多模块项目中。

```bash
# 克隆项目
git clone https://github.com/your-repo/java-knowledge-base.git

# 编译所有代码示例
cd code-examples
mvn compile
```

## 🐳 中间件环境

依赖外部服务的示例提供 Docker Compose 一键启动：

```bash
# 启动 Redis + MySQL
docker compose -f docker/docker-compose.yml up -d

# 启动消息队列
docker compose -f docker/docker-compose.mq.yml up -d
```

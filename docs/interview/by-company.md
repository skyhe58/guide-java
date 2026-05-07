# 按公司类型分类面试重点

## 概述

不同类型的公司对 Java 开发者的考察侧重点不同。本文按大厂、中厂、创业公司三类，梳理各自的面试重点模块和高频题目，帮助你有针对性地准备。

## 大厂面试重点

> 代表公司：阿里、字节、腾讯、美团、京东、百度、快手等

大厂面试特点：**深度优先**，注重底层原理和源码分析，系统设计题必考。

### 重点模块与高频题目

#### 1. Java 基础与集合（必考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| HashMap 底层原理、扩容机制、红黑树转换 | ⭐⭐⭐ | [集合框架](/1-java-core/1.1-java-basics/05-collections) |
| ConcurrentHashMap 实现原理（JDK 7 vs 8） | ⭐⭐⭐ | [集合源码分析](/1-java-core/1.2-java-advanced/01-collections-source) |
| String 不可变性原因、String Pool 机制 | ⭐⭐ | [String 深入](/1-java-core/1.1-java-basics/03-string-deep-dive) |
| equals 和 hashCode 的关系 | ⭐⭐ | [面向对象](/1-java-core/1.1-java-basics/04-oop) |

#### 2. 并发编程（必考，深度追问）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| synchronized 锁升级过程 | ⭐⭐⭐ | [synchronized 原理](/1-java-core/1.3-concurrent/02-synchronized) |
| AQS 原理、ReentrantLock 源码 | ⭐⭐⭐ | [ReentrantLock/AQS](/1-java-core/1.3-concurrent/03-reentrantlock-aqs) |
| 线程池核心参数、拒绝策略、如何设置线程数 | ⭐⭐⭐ | [线程池原理](/1-java-core/1.3-concurrent/05-thread-pool) |
| volatile 原理、内存屏障 | ⭐⭐⭐ | [volatile 原理](/1-java-core/1.3-concurrent/04-volatile) |
| ThreadLocal 内存泄漏原因 | ⭐⭐ | [ThreadLocal](/1-java-core/1.3-concurrent/07-threadlocal) |

#### 3. JVM（必考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| JVM 内存区域划分 | ⭐⭐ | [JVM 内存模型](/1-java-core/1.4-jvm/01-memory-model) |
| G1/ZGC 收集器原理 | ⭐⭐⭐ | [GC 算法与收集器](/1-java-core/1.4-jvm/02-gc) |
| 类加载机制、双亲委派 | ⭐⭐ | [类加载过程](/1-java-core/1.4-jvm/03-classloading) |
| 线上 OOM/CPU 飙高排查流程 | ⭐⭐⭐ | [诊断工具](/1-java-core/1.4-jvm/06-diagnostic) |

#### 4. Spring 生态（必考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Spring IoC 容器原理、Bean 生命周期 | ⭐⭐⭐ | [IoC/DI](/2-framework/2.2-springboot/01-ioc-di) |
| Spring AOP 原理、事务失效场景 | ⭐⭐⭐ | [AOP 原理](/2-framework/2.2-springboot/02-aop) |
| 循环依赖三级缓存 | ⭐⭐⭐ | [循环依赖](/2-framework/2.2-springboot/03-circular-dependency) |
| Spring Boot 自动配置原理 | ⭐⭐ | [启动流程](/2-framework/2.2-springboot/04-startup) |

#### 5. MySQL（必考，深度追问）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| B+树索引原理、索引失效场景 | ⭐⭐⭐ | [索引原理](/3-data-store/3.1-database/01-index-theory) |
| 事务隔离级别、MVCC 实现 | ⭐⭐⭐ | [事务与隔离级别](/3-data-store/3.1-database/02-transaction) |
| 行锁/间隙锁/临键锁加锁规则 | ⭐⭐⭐ | [锁机制](/3-data-store/3.1-database/03-lock) |
| 慢查询优化、EXPLAIN 分析 | ⭐⭐ | [SQL 优化](/3-data-store/3.1-database/04-optimization) |

#### 6. Redis（必考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Redis 数据结构与底层编码 | ⭐⭐⭐ | [数据结构](/3-data-store/3.2-redis/01-data-structures) |
| 缓存穿透/击穿/雪崩解决方案 | ⭐⭐⭐ | [缓存问题](/3-data-store/3.2-redis/04-cache-problems) |
| Redis 分布式锁实现（Redisson） | ⭐⭐⭐ | [分布式锁](/3-data-store/3.2-redis/05-distributed-lock) |
| Redis 集群方案对比 | ⭐⭐ | [主从/哨兵/Cluster](/3-data-store/3.2-redis/03-replication) |

#### 7. 分布式与系统设计（高级岗必考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| 分布式事务方案对比 | ⭐⭐⭐ | [分布式事务](/5-distributed/5.1-distributed/04-distributed-transaction) |
| 秒杀系统设计 | ⭐⭐⭐ | [秒杀系统](/8-architecture/01-seckill) |
| 缓存与数据库双写一致性 | ⭐⭐⭐ | [缓存一致性](/8-architecture/08-cache-db-consistency) |
| CAP 理论、Raft 算法 | ⭐⭐⭐ | [CAP/BASE 理论](/5-distributed/5.1-distributed/01-cap-base) |

---

## 中厂面试重点

> 代表公司：得物、小红书、B 站、携程、网易、有赞、货拉拉等

中厂面试特点：**广度与深度兼顾**，注重实际项目经验和问题解决能力。

### 重点模块与高频题目

#### 1. Java 基础（必考，中等深度）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| HashMap 原理与线程安全问题 | ⭐⭐ | [集合框架](/1-java-core/1.1-java-basics/05-collections) |
| 线程池使用与参数配置 | ⭐⭐ | [线程池原理](/1-java-core/1.3-concurrent/05-thread-pool) |
| JVM 内存模型与 GC 调优 | ⭐⭐ | [GC 算法](/1-java-core/1.4-jvm/02-gc) |
| synchronized vs ReentrantLock | ⭐⭐ | [synchronized](/1-java-core/1.3-concurrent/02-synchronized) |

#### 2. Spring Boot 实战（重点）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Spring Boot 自动配置原理 | ⭐⭐ | [启动流程](/2-framework/2.2-springboot/04-startup) |
| 事务管理与失效场景 | ⭐⭐ | [AOP 原理](/2-framework/2.2-springboot/02-aop) |
| RESTful API 设计与全局异常处理 | ⭐⭐ | [Web 开发](/2-framework/2.2-springboot/07-web) |
| MyBatis/MyBatis-Plus 使用 | ⭐⭐ | [数据访问](/2-framework/2.2-springboot/08-data-access) |

#### 3. MySQL 实战（重点）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| 索引优化与 EXPLAIN 分析 | ⭐⭐ | [SQL 优化](/3-data-store/3.1-database/04-optimization) |
| 事务隔离级别与实际应用 | ⭐⭐ | [事务](/3-data-store/3.1-database/02-transaction) |
| 分库分表方案 | ⭐⭐ | [分库分表](/3-data-store/3.1-database/05-sharding) |
| 连接池配置与优化 | ⭐ | [连接池](/3-data-store/3.1-database/10-pool) |

#### 4. Redis 实战（重点）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Redis 常用数据类型与使用场景 | ⭐⭐ | [数据结构](/3-data-store/3.2-redis/01-data-structures) |
| 缓存穿透/击穿/雪崩 | ⭐⭐ | [缓存问题](/3-data-store/3.2-redis/04-cache-problems) |
| Redis 与 Spring Boot 集成 | ⭐⭐ | [Spring 集成](/3-data-store/3.2-redis/06-spring-integration) |

#### 5. 消息队列（常考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| RabbitMQ/Kafka 选型对比 | ⭐⭐ | [RabbitMQ](/4-middleware/4.1-mq-rabbitmq/01-rabbitmq) |
| 消息可靠性保证 | ⭐⭐ | [消息可靠性](/4-middleware/4.1-mq-rabbitmq/02-rabbitmq-reliability) |
| 消息幂等性处理 | ⭐⭐ | [幂等性设计](/5-distributed/5.1-distributed/05-idempotent) |

#### 6. 微服务（常考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| 服务注册发现原理 | ⭐⭐ | [服务注册发现](/2-framework/2.3-springcloud/01-registry) |
| 熔断降级策略 | ⭐⭐ | [熔断降级](/2-framework/2.3-springcloud/04-circuit-breaker) |
| 网关设计 | ⭐⭐ | [Gateway](/2-framework/2.3-springcloud/05-gateway) |

#### 7. Docker/K8s（加分项）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Docker 基本使用与 Dockerfile | ⭐ | [Docker 基础](/6-devops/6.1-docker-k8s/01-docker-basics) |
| K8s 核心概念 | ⭐⭐ | [K8s 架构](/6-devops/6.1-docker-k8s/06-k8s-architecture) |

---

## 创业公司面试重点

> 代表公司：各类 A-C 轮创业公司、中小型技术团队

创业公司面试特点：**实战优先**，注重快速上手能力和全栈思维，技术深度要求相对较低。

### 重点模块与高频题目

#### 1. Java 基础（基本功验证）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| 集合框架基本使用 | ⭐ | [集合框架](/1-java-core/1.1-java-basics/05-collections) |
| 多线程基础 | ⭐ | [线程生命周期](/1-java-core/1.3-concurrent/01-thread-lifecycle) |
| 异常处理 | ⭐ | [异常处理](/1-java-core/1.1-java-basics/06-exceptions) |

#### 2. Spring Boot 实战（核心考察）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Spring Boot 项目搭建与配置 | ⭐ | [配置文件](/2-framework/2.2-springboot/06-config-files) |
| RESTful API 开发 | ⭐ | [Web 开发](/2-framework/2.2-springboot/07-web) |
| 数据库操作（MyBatis） | ⭐ | [数据访问](/2-framework/2.2-springboot/08-data-access) |
| 定时任务 | ⭐ | [定时任务](/2-framework/2.2-springboot/12-task) |

#### 3. MySQL 基础（实用为主）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| SQL 编写与优化 | ⭐ | [SQL 优化](/3-data-store/3.1-database/04-optimization) |
| 索引使用 | ⭐ | [索引原理](/3-data-store/3.1-database/01-index-theory) |
| 事务基本概念 | ⭐ | [事务](/3-data-store/3.1-database/02-transaction) |

#### 4. Redis 基础（常考）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Redis 基本数据类型与使用 | ⭐ | [数据结构](/3-data-store/3.2-redis/01-data-structures) |
| 缓存使用场景 | ⭐ | [缓存问题](/3-data-store/3.2-redis/04-cache-problems) |
| Spring Boot 集成 Redis | ⭐ | [Spring 集成](/3-data-store/3.2-redis/06-spring-integration) |

#### 5. 项目经验（重点考察）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| 接口幂等性如何保证 | ⭐ | [幂等性方案](/8-architecture/05-idempotent-design) |
| 大文件上传方案 | ⭐ | [文件上传](/8-architecture/07-file-upload) |
| 分布式 Session 方案 | ⭐ | [分布式 Session](/8-architecture/06-distributed-session) |

#### 6. DevOps（加分项）

| 高频题目 | 难度 | 文档链接 |
|----------|------|----------|
| Docker 基本使用 | ⭐ | [Docker 基础](/6-devops/6.1-docker-k8s/01-docker-basics) |
| CI/CD 流水线 | ⭐ | [GitHub Actions](/6-devops/6.2-cicd/02-github-actions) |
| Linux 常用命令 | ⭐ | [常用命令](/6-devops/6.4-linux/01-commands) |

---

## 面试准备策略总结

| 公司类型 | 准备周期 | 核心策略 | 推荐学习路径 |
|----------|----------|----------|-------------|
| 大厂 | 4-8 周 | 深度优先，源码级理解，系统设计必练 | [高级深入路径](/learning-paths/advanced) |
| 中厂 | 2-4 周 | 广度与深度兼顾，项目经验是关键 | [面试突击路径](/learning-paths/interview-sprint) |
| 创业公司 | 1-2 周 | 实战优先，快速上手能力 | [中级进阶路径](/learning-paths/intermediate) |

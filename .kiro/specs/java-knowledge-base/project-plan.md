# Java 个人知识库 — 完整项目计划

> 📌 **当前唯一的项目计划文件**。合并了原 `tasks.md`（文档+代码任务）和 `code-quality-improvement.md`（代码质量改进），按模块组织，每个模块只出现一次。后续所有开发、调整、补充都以本文件为准。
> 搜索 `TODO-PENDING` 可快速定位所有待处理的可选/后续任务。

## 一、项目概述

### 项目简介

按照循序渐进的原则，先搭建项目骨架（仓库结构、Maven 父 POM、VitePress 配置、CI/CD），再按五层学习架构逐步填充各模块内容。每个模块包含 Markdown 知识文档和对应的 Maven 子模块代码示例。

### 核心规范

- 每个知识点必须有对应的可运行代码示例（链接到 code-examples 子模块）
- 涉及复杂流程的知识点必须包含 Mermaid 流程图或时序图

**文档与代码对应规则**：
- 文档位置：`docs/{分组名}/{模块名}/{序号-知识点}.md`（如 `docs/1-java-core/1.1-java-basics/01-data-types.md`）
- 代码位置：`code-examples/{分组名}/{子模块名}/`
- 任务中的文档文件名和代码子模块名保持一致的命名风格，方便互相查找

**编号说明**：
- 阶段编号 = 项目分组编号，子任务编号 = docs 子目录编号
- 阶段 0：项目骨架与基础设施（无对应 docs 分组）
- 阶段 1~7：对应 `docs/1-java-core/` ~ `docs/7-ai/` 和 `code-examples/01-java-core/` ~ `code-examples/07-ai/`
- 阶段 8：架构设计场景（对应 `docs/8-architecture/`）
- 阶段 9~11：学习路径/面试、导航系统、最终检查点
- 子任务编号如 1.1、1.2 对应 `docs/1-java-core/1.1-java-basics/`、`docs/1-java-core/1.2-java-advanced/` 等实际目录

### 技术栈

- Java 21
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1

## 二、代码质量标准

### 改进标准

1. 每个 Java 文件必须有真实可运行的代码案例，像教科书一样
2. println 只用于输出运行结果，不用于讲解知识点。描述和讲解内容用注释写，注释内容清晰有条理
3. 改进后 println 占代码行数（排除注释）不超过 20%，正常输出结果的 println 是需要的
4. demo 要丰富、案例详细，有利于高效学习和面试
5. 对应 md 文档也一起丰富优化

### Demo 模式（混合方案）

每个文件根据内容特点，采用以下一种或两种模式混合：

#### 模式一：纯 Java 内存模拟（适合原理/算法类）
- 直接运行，不依赖外部环境
- 用 Java 数据结构模拟中间件内部行为
- 适合：索引原理、事务隔离、锁机制、分片算法、雪花 ID、TCC 状态机、网关过滤器链、JVM 内存/GC/类加载等

#### 模式二：连接真实中间件（适合 API/操作类）
- 需要 Docker 启动中间件，体验真实 API 和响应
- 文件头 Javadoc 标注 Docker 启动命令
- 需要在 pom.xml 中添加对应客户端依赖
- 适合：ES CRUD/聚合、Redis 数据结构操作、RabbitMQ/Kafka 消息收发、MongoDB CRUD、MinIO 文件操作、Consul/ZK/Nacos 注册发现等

#### 模式一 + 模式二混合（推荐）
- 文件中同时包含两部分：
  - Part A：原理模拟（纯 Java，直接运行理解原理）
  - Part B：真实连接（启动 Docker 后体验真实 API）
- 适合：既有原理又有操作的场景，如 Redis 数据结构（模拟底层编码 + Jedis 操作）

```java
/**
 * Redis 数据结构演示
 *
 * Part A：纯 Java 模拟，直接运行
 * Part B：连接真实 Redis，需先启动：
 * {@code docker compose -f docker/docker-compose.yml up -d redis}
 */
```

### Part B 入口代码规范

后续新文件统一用注释方式，不加 else 提示：
```java
// Part B：连接真实 Redis，需传入参数 'real'，启动命令：docker compose -f docker/docker-compose.yml up -d redis
if (args.length > 0 && "real".equals(args[0])) {
    RealRedis.run();
}
```

### Part B 数据管理规范

1. 每个 Part B 方法自带 `prepareTestData()`，自动创建表/索引并写入测试数据
2. 清理逻辑统一抽取到 `cleanup()` 方法，注释说明用户可以注释掉以保留数据
3. 用户可以在管理界面（Kibana / RabbitMQ Management / Navicat 等）查看数据后再手动清理

### 图形演示规范

1. 对应 md 文档中优先使用 Mermaid 图（流程图/时序图/状态图），VitePress 原生支持
2. Java 文件 Javadoc 中视情况加 ASCII 图辅助理解
3. 适用场景：
   - 流程图：Binlog 主从复制流程、连接池获取归还流程、分片路由流程
   - 时序图：事务隔离级别并发行为、消息确认流程、分布式锁竞争
   - 状态图：熔断器状态转换、连接池连接生命周期
   - 结构图：B+树索引结构、倒排索引结构、一致性哈希环

## 三、中间件配置规范

### Docker 对照表

| Compose 文件 | 服务 | 代码模块 | 需添加的客户端依赖 |
|-------------|------|---------|------------------|
| `docker/docker-compose.yml` | redis | redis-examples | `jedis` 或 `lettuce-core` |
| `docker/docker-compose.yml` | mysql | database-examples | `mysql-connector-j` |
| `docker/docker-compose.yml` | mongodb | mongodb-examples | `mongodb-driver-sync` |
| `docker/docker-compose.yml` | minio | minio-examples | `minio` |
| `docker/docker-compose.mq.yml` | rabbitmq | mq-rabbitmq-examples | `amqp-client` |
| `docker/docker-compose.mq.yml` | kafka | mq-kafka-examples | `kafka-clients` |
| `docker/docker-compose.mq.yml` | zookeeper | registry-examples | `curator-framework` |
| `docker/docker-compose.es.yml` | elasticsearch | elasticsearch-examples | `elasticsearch-rest-client` |
| `docker/docker-compose.consul.yml` | consul | registry-examples | `consul-api` |
| `docker/docker-compose.apollo.yml` | apollo | config-center-examples | （模拟为主） |
| `docker/docker-compose.nginx.yml` | nginx | nginx-examples | （配置文件为主） |

### 中间件连接配置统一规范

所有文件中相同中间件的连接地址、端口、用户名、密码保持一致：

| 中间件 | 地址 | 用户/密码 | Docker 启动命令 |
|--------|------|----------|----------------|
| MySQL | `localhost:3306` | `root` / `root123` | `docker compose -f docker/docker-compose.yml up -d mysql` |
| Redis | `localhost:6379` | 无密码 | `docker compose -f docker/docker-compose.yml up -d redis` |
| RabbitMQ | `localhost:5672`（管理界面 15672） | `guest` / `guest` | `docker compose -f docker/docker-compose.mq.yml up -d rabbitmq` |
| Kafka | `localhost:9092` | 无认证 | `docker compose -f docker/docker-compose.mq.yml up -d kafka` |
| MQTT | `tcp://localhost:1883` | 无认证 | `docker compose -f docker/docker-compose.mq.yml up -d rabbitmq`（MQTT 插件） |
| Elasticsearch | `localhost:9200`（Kibana 5601） | 无认证 | `docker compose -f docker/docker-compose.es.yml up -d elasticsearch` |
| MongoDB | `localhost:27017` | 无认证 | `docker compose -f docker/docker-compose.yml up -d mongodb` |
| MinIO | `http://localhost:9000`（控制台 9001） | `minioadmin` / `minioadmin` | `docker compose -f docker/docker-compose.yml up -d minio` |
| Consul | `localhost:8500` | 无认证 | `docker compose -f docker/docker-compose.consul.yml up -d consul` |
| ZooKeeper | `localhost:2181` | 无认证 | `docker compose -f docker/docker-compose.mq.yml up -d zookeeper` |
| Apollo | `localhost:8080`（Portal） | `apollo` / `admin` | `docker compose -f docker/docker-compose.apollo.yml up -d` |

Java 文件中的 Javadoc 统一格式：
```java
/**
 * Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d redis}
 */
```

### 依赖管理（根 pom.xml dependencyManagement）

根 pom.xml（`code-examples/pom.xml`）的 `<dependencyManagement>` 已统一管理所有中间件客户端版本。
各子模块 pom.xml 按需引用，不写版本号。

| 依赖 | groupId:artifactId | 版本 | 使用模块 |
|------|-------------------|------|---------|
| Jedis | `redis.clients:jedis` | 5.1.0 | redis-examples |
| ES Java Client | `co.elastic.clients:elasticsearch-java` | 8.12.1 | elasticsearch-examples |
| RabbitMQ | `com.rabbitmq:amqp-client` | 5.20.0 | mq-rabbitmq-examples |
| Kafka | `org.apache.kafka:kafka-clients` | 3.7.0 | mq-kafka-examples |
| MongoDB | `org.mongodb:mongodb-driver-sync` | 5.1.0 | mongodb-examples |
| MinIO | `io.minio:minio` | 8.5.14 | minio-examples |
| MQTT Paho | `org.eclipse.paho:org.eclipse.paho.client.mqttv3` | 1.2.5 | mq-mqtt-examples |
| Consul API | `com.ecwid.consul:consul-api` | 1.4.5 | registry-examples |
| Curator | `org.apache.curator:curator-framework` | 5.6.0 | registry-examples |

子模块 pom.xml 示例：
```xml
<!-- redis-examples/pom.xml 中只写 groupId + artifactId，版本由根 pom 管理 -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

## 四、任务清单（按执行顺序）

### 阶段 0：项目骨架与基础设施

- [x] 0.1 创建项目根目录结构和根 README.md
  - 创建顶层目录：`docs/`、`code-examples/`、`.github/`、`docker/`
  - 编写 `README.md`（项目简介、快速开始、目录结构、完成度追踪表）
  - 创建 `docs/templates/entry-template.md`（知识条目模板）

- [x] 0.2 创建 Maven 父 POM 和所有子模块骨架
  - `code-examples/pom.xml` — 根 POM（Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.1）
  - 7 个分组模块：01-java-core、02-framework、03-data-store、04-middleware、05-distributed、06-devops、07-ai
  - 分组下共 22 个子模块（含 mongodb-examples、minio-examples、mq-mqtt-examples、cicd-examples、monitoring-examples）
  - 确保 `mvn compile` 编译通过

- [x] 0.3 初始化 VitePress 站点配置
  - `docs/.vitepress/config.mts` — 主配置（中文、本地搜索、Mermaid、代码行号）
  - `docs/.vitepress/sidebar.mts` — 侧边栏配置
  - `docs/index.md` — 首页
  - `docs/guide/getting-started.md` — 快速开始
  - `docs/guide/how-to-use.md` — 使用指南
  - `docs/package.json` — pnpm + VitePress 依赖

- [x] 0.4 配置 GitHub Actions CI/CD
  - `.github/workflows/deploy.yml` — 自动构建 + 部署 + Maven 编译验证 + 断链检查

- [x] 0.5 创建 Docker Compose 配置文件
  - `docker/docker-compose.yml` — Redis、MySQL、MongoDB、MinIO
  - `docker/docker-compose.mq.yml` — RabbitMQ、Kafka、ZooKeeper
  - `docker/docker-compose.es.yml` — Elasticsearch
  - `docker/docker-compose.consul.yml` — Consul
  - `docker/docker-compose.apollo.yml` — Apollo
  - `docker/docker-compose.nginx.yml` — Nginx

- [x] 0.6 检查点 - 项目骨架验证
  - `mvn compile` 通过、`pnpm run build` 通过、Docker Compose 语法正确

### 阶段 1：Java 核心（docs/1-java-core/ → code-examples/01-java-core/）

> 合并原阶段 2（Java 基础）+ 阶段 3（Java 深入）+ 阶段 13 批次 9 中 JVM/Java 进阶/设计模式/Java 基础的代码质量改进。
> 每个子模块一次性完成：md 文档 + 高质量 Demo（Part A + Part B）。

#### 1.1 Java 基础（docs/1-java-core/1.1-java-basics/ → 01-java-core/java-basics/）

- [x] 1.1 Java 基础模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `01-data-types.md` | 数据类型/装箱拆箱/缓存池 |
  | `02-value-passing.md` | 值传递与引用传递 |
  | `03-string-deep-dive.md` | String 不可变性/Pool/intern |
  | `04-oop.md` | 封装/继承/多态/内部类/Object 方法 |
  | `05-collections.md` | ArrayList/HashMap/TreeMap/遍历陷阱/LRU |
  | `06-exceptions.md` | Checked vs Unchecked/自定义异常 |
  | `07-generics.md` | 类型擦除/通配符/上下界 |
  | `08-reflection.md` | Class/Field/Method/Constructor |
  | `09-annotations.md` | 自定义注解/元注解/处理器 |
  | `10-io-streams.md` | IO/NIO/内存映射 |
  | `11-lambda-stream.md` | Lambda/Stream/并行流 |
  | `12-new-features.md` | JDK 8/17/21 特性演进 |
  | `99-interview.md` | Java 基础面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 9 的 DataTypesDemo 改进）：

  | 代码目录 | 示例内容 | 对应文档 |
  |---------|---------|---------|
  | datatypes/ | `DataTypesDemo.java` — 装箱拆箱陷阱+Integer 缓存池+浮点精度+性能对比（高质量版本） | `01-data-types.md`, `02-value-passing.md` |
  | string/ | String Pool、StringBuilder 对比 | `03-string-deep-dive.md` |
  | oop/ | 多态、内部类、equals/hashCode | `04-oop.md` |
  | collections/ | HashMap 扩容、LRU 缓存、ConcurrentModificationException | `05-collections.md` |
  | exceptions/ | 自定义异常、异常处理最佳实践 | `06-exceptions.md` |
  | generics/ | 类型擦除、通配符 | `07-generics.md` |
  | reflection/ | 反射操作全套 | `08-reflection.md` |
  | annotations/ | 自定义注解 + 处理器 | `09-annotations.md` |
  | io/ | IO 流、NIO、MappedByteBuffer | `10-io-streams.md` |
  | stream/ | Stream 常用操作、并行流 | `11-lambda-stream.md` |
  | features/ | JDK 8/17/21 新特性 | `12-new-features.md` |

  - 每个示例包含 main 方法 + 中文注释 + JUnit 5 测试

  - [ ]* 1.1.opt 编写 Java 基础模块单元测试（可选） <!-- TODO-PENDING: 可选任务 -->
    - 为 collections、generics、stream 等核心示例编写 JUnit 5 + AssertJ 测试

#### 1.2 Java 进阶（docs/1-java-core/1.2-java-advanced/ → 01-java-core/java-advanced/）

- [x] 1.2 Java 进阶模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `01-collections-source.md` | HashMap/ConcurrentHashMap 源码 |
  | `02-classloader.md` | 双亲委派/打破双亲委派 |
  | `03-dynamic-proxy.md` | JDK/CGLIB/ByteBuddy 代理 |
  | `04-spi.md` | SPI 机制 |
  | `05-serialization.md` | Java/JSON/Protobuf 序列化 |
  | `06-network-programming.md` | BIO/NIO/AIO/Netty |
  | `07-jmm.md` | JMM/happens-before |
  | `99-interview.md` | Java 进阶面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 9 的代码改进）：

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `SPIDemo.java` | 手写 ServiceLoader+SPI 原理+Java/Spring/Dubbo SPI 对比 | 纯模拟 |
  | `ClassLoaderDemo.java` | 三层加载器+双亲委派+自定义 ClassLoader+打破委派 | 纯模拟 |
  | 其他 Demo | 动态代理、序列化、JMM 等 | 纯模拟 |

#### 1.3 并发编程（docs/1-java-core/1.3-concurrent/ → 01-java-core/concurrent-programming/）

- [x] 1.3 并发编程模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `01-thread-lifecycle.md` | 线程生命周期 |
  | `02-synchronized.md` | synchronized/锁升级 |
  | `03-reentrantlock-aqs.md` | ReentrantLock/AQS 源码 |
  | `04-volatile.md` | volatile/内存屏障 |
  | `05-thread-pool.md` | 线程池原理/参数/拒绝策略 |
  | `06-concurrent-tools.md` | CountDownLatch/CyclicBarrier/Semaphore |
  | `07-threadlocal.md` | ThreadLocal/内存泄漏 |
  | `08-completable-future.md` | CompletableFuture 异步编程 |
  | `09-cas-atomic.md` | CAS/原子类/LongAdder |
  | `10-deadlock.md` | 死锁检测与避免 |
  | `99-interview.md` | 并发编程面试指南 |

  **Java Demo 文件**：每个文档对应一个 Demo 文件，println ≤ 20%，纯模拟模式

#### 1.4 JVM（docs/1-java-core/1.4-jvm/ → 01-java-core/jvm-deep-dive/）

- [x] 1.4 JVM 模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `01-memory-model.md` | 内存模型与内存区域 |
  | `02-gc.md` | GC 算法/CMS/G1/ZGC |
  | `03-classloading.md` | 类加载过程 |
  | `04-jit.md` | JIT 编译/逃逸分析 |
  | `05-tuning.md` | JVM 调优参数/GC 日志 |
  | `06-diagnostic.md` | 内存泄漏/jstack/arthas |
  | `99-interview.md` | JVM 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 9 的代码改进）：

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `MemoryAreaDemo.java` | 堆/栈/方法区/直接内存+MXBean 监控+OOM 场景 | 纯模拟 |
  | `GCDemo.java` | 四种引用类型+GC 算法模拟+对象晋升+收集器对比 | 纯模拟 |
  | `ClassLoadingDemo.java` | 类加载五阶段+主动/被动引用+clinit 线程安全 | 纯模拟 |
  | `TuningDemo.java` | MXBean 监控+内存分配策略+GC 日志解读+调优参数 | 纯模拟 |
  | `DiagnosticDemo.java` | 内存泄漏+CPU 飙高+死锁模拟+排查工具 | 纯模拟 |
  | `JITDemo.java` | 逃逸分析+标量替换+锁消除+锁粗化 | 纯模拟 |

#### 1.5 设计模式（docs/1-java-core/1.5-design-patterns/ → 01-java-core/design-patterns/）

- [x] 1.5 设计模式模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `01-creational.md` | 单例/工厂/建造者/原型 |
  | `02-structural.md` | 代理/适配器/装饰器/门面 |
  | `03-behavioral.md` | 策略/模板方法/观察者/责任链 |
  | `04-spring-patterns.md` | Spring 中的设计模式 |
  | `05-principles.md` | SOLID/DRY/KISS |
  | `99-interview.md` | 设计模式面试指南 |

  **Java Demo 文件清单**（含原批次 9 的 SpringPatternsDemo 改进）：

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `SpringPatternsDemo.java` | BeanFactory+AOP 代理+JdbcTemplate+事件机制（高质量版本） | 纯模拟 |
  | 其他 Demo | 各设计模式实现 | 纯模拟 |

#### 1.6 数据结构与算法（docs/1-java-core/1.6-algorithm/ → 01-java-core/java-basics/ 的 algorithm 目录）

- [x] 1.6 数据结构与算法模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `01-linked-list.md` | 反转链表/合并/环形检测 |
  | `02-stack-queue.md` | 有效括号/最小栈 |
  | `03-hash-table.md` | 两数之和/LRU 缓存 |
  | `04-binary-tree.md` | 遍历/BST/公共祖先 |
  | `05-heap.md` | TopK/合并 K 个链表 |
  | `06-sorting.md` | 快排/归并/堆排序 |
  | `07-binary-search.md` | 二分查找/旋转数组 |
  | `08-two-pointers.md` | 双指针/滑动窗口 |
  | `09-dynamic-programming.md` | 爬楼梯/LIS/背包/编辑距离 |
  | `10-backtracking.md` | 全排列/子集/N 皇后 |
  | `99-interview.md` | 算法面试指南 |

  - 优先覆盖 LeetCode Hot 100 高频题

#### 检查点

- [x] 1.x 检查点 - Java 核心验证
  - 文档格式、frontmatter 完整性、代码编译、站点构建无断链

### 阶段 2：框架层（docs/2-framework/ → code-examples/02-framework/）

> 合并原阶段 5 网络/Spring Boot + Spring Cloud 全家桶（含实战项目 4 模块 + 18 篇文档更新）。
> Spring Cloud（2.3）是最大的部分，包含知识文档 + Part A Demo + Controller 层 + 3 个独立微服务模块 + md 文档同步更新。

#### 2.1 网络与协议（docs/2-framework/2.1-network/ → 02-framework/network-programming/）

- [x] 2.1 网络与协议模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `tcp-ip.md` | TCP/IP 协议栈/三次握手 |
  | `http.md` | HTTP/HTTPS/HTTP2/HTTP3 |
  | `websocket.md` | WebSocket 协议 |
  | `dns-cdn.md` | DNS/CDN 原理 |
  | `security.md` | XSS/CSRF/SQL 注入 |
  | `restful.md` | RESTful API 设计 |
  | `rpc.md` | Dubbo/gRPC 原理 |
  | `interview.md` | 网络面试指南 |

  **Java Demo 文件**：HTTP/TCP/WebSocket/RPC Demo，纯模拟模式

#### 2.2 Spring Boot（docs/2-framework/2.2-springboot/ → 02-framework/springboot-examples/）

- [x] 2.2 Spring Boot 模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `ioc-di.md` | IoC/DI/Bean 生命周期 |
  | `aop.md` | AOP/事务失效场景 |
  | `circular-dependency.md` | 循环依赖三级缓存 |
  | `startup.md` | 启动流程/自动配置 |
  | `starter.md` | Starter 机制/自定义 Starter |
  | `config-files.md` | yml/properties/Profile |
  | `web.md` | RESTful/拦截器/全局异常 |
  | `data-access.md` | JPA/MyBatis/MyBatis-Plus |
  | `security.md` | Spring Security/OAuth2 |
  | `logging.md` | SLF4J+Logback/MDC/ELK |
  | `cache.md` | @Cacheable/Redis 缓存 |
  | `task.md` | @Scheduled/XXL-Job |
  | `actuator.md` | Actuator 监控/健康检查 |
  | `interview.md` | Spring Boot 面试指南 |

  **Java Demo 文件**：SpringBoot 模块当前 0% println，纯注解/配置代码，质量良好

#### 2.3 Spring Cloud 全家桶（docs/2-framework/2.3-springcloud/ → 02-framework/springcloud-*）

> **重点合并**：知识文档 + 高质量 Part A Demo + Controller 层 + 3 个独立微服务模块 + md 文档同步更新。

##### 2.3.1 Spring Cloud 知识文档（10 篇 md）

- [x] 2.3.1 Spring Cloud 知识文档（docs/2-framework/2.3-springcloud/）

  | 文档文件 | 知识点 |
  |---------|--------|
  | `registry.md` | 服务注册与发现（Consul 优先） |
  | `loadbalancer.md` | Ribbon/LoadBalancer |
  | `feign.md` | OpenFeign/超时重试 |
  | `circuit-breaker.md` | Sentinel/Resilience4j |
  | `gateway.md` | Gateway 路由/过滤器/限流 |
  | `config.md` | Apollo/Nacos Config |
  | `tracing.md` | Sleuth/Zipkin/SkyWalking |
  | `transaction.md` | Seata AT/TCC |
  | `version-compatibility.md` | 版本兼容性对照表 |
  | `interview.md` | Spring Cloud 面试指南 |

##### 2.3.2 Part A 原理模拟 Demo（6 个文件）

路径前缀：`code-examples/02-framework/springcloud-examples/src/main/java/com/example/springcloud/`

> Part A Demo 通过 `main()` 方法直接运行理解原理，与 Controller 层共存互补。

| Demo 文件 | 内容 | 模式 |
|-----------|------|------|
| `transaction/TransactionDemo.java` | 状态机模拟 TCC（Try/Confirm/Cancel）+ 空回滚/悬挂/幂等 | 纯模拟 |
| `gateway/GatewayDemo.java` | Map+责任链模拟路由匹配+过滤器链（限流/鉴权/日志/路径重写） | 纯模拟 |
| `registry/RegistryDemo.java` | ConcurrentHashMap 模拟注册表+心跳续约+服务剔除 | 纯模拟 |
| `feign/FeignDemo.java` | JDK 动态代理+负载均衡（轮询/随机/加权）+Fallback 降级 | 纯模拟 |
| `tracing/TracingDemo.java` | TraceId/SpanId+MDC 日志+采样策略 | 纯模拟 |
| `circuitbreaker/CircuitBreakerDemo.java` | 状态机熔断器三态转换+滑动窗口+Resilience4j 配置 | 纯模拟 |

##### 2.3.3 实战项目 — 模块 A（springcloud-examples，端口 8090，WebMVC）

> 业务服务，覆盖注册发现、Feign 调用、熔断降级、消息队列、缓存、数据库、搜索、文件存储、定时任务、链路追踪、分布式限流/会话/缓存一致性等核心功能。
> 每个 Controller 通过 REST 接口验证，支持 Profile 切换不同中间件实现。

```
springcloud-examples/
├── pom.xml                              ← Spring Cloud 全家桶依赖
├── src/main/java/com/example/springcloud/
│   ├── SpringCloudApp.java              ← 启动类
│   │   // @SpringBootApplication + @EnableFeignClients + @EnableDiscoveryClient + @EnableScheduling
│   │
│   │   ══════ 注册发现 ══════
│   ├── registry/
│   │   ├── RegistryDemo.java            ← Part A 原理模拟
│   │   └── RegistryController.java      ← REST 接口（DiscoveryClient）
│   │
│   │   ══════ 声明式调用 ══════
│   ├── feign/
│   │   ├── FeignDemo.java               ← Part A 原理模拟
│   │   ├── UserFeignClient.java         ← @FeignClient 声明式接口
│   │   ├── UserFeignFallbackFactory.java ← Fallback 工厂
│   │   ├── UserDTO.java                 ← 数据传输对象
│   │   └── FeignController.java         ← REST 接口
│   │
│   │   ══════ 熔断降级 ══════
│   ├── circuitbreaker/
│   │   ├── CircuitBreakerDemo.java      ← Part A 原理模拟
│   │   └── CircuitBreakerController.java ← REST 接口（@CircuitBreaker）
│   │
│   │   ══════ Spring Boot 基础实战 ══════
│   ├── boot/
│   │   ├── IoCController.java           ← @Autowired/@Qualifier/@Primary
│   │   ├── AopController.java           ← @Aspect 切面
│   │   ├── ExceptionController.java     ← @ControllerAdvice 全局异常
│   │   └── ValidateController.java      ← @Valid + @Validated 参数校验
│   │
│   │   ══════ 消息队列 ══════
│   ├── mq/
│   │   ├── RabbitMQController.java      ← RabbitTemplate + @RabbitListener
│   │   ├── KafkaController.java         ← KafkaTemplate + @KafkaListener
│   │   └── MqttController.java          ← spring-integration-mqtt（可选）
│   │
│   │   ══════ 缓存 + 分布式锁 ══════
│   ├── cache/
│   │   └── RedisCacheController.java    ← RedisTemplate + @Cacheable + Redisson 分布式锁
│   │
│   │   ══════ 数据库 ══════
│   ├── db/
│   │   └── JdbcController.java          ← JdbcTemplate + HikariCP 连接池状态
│   │
│   │   ══════ 搜索 ══════
│   ├── search/
│   │   ├── ArticleDocument.java         ← @Document 实体类
│   │   ├── ArticleRepository.java       ← ElasticsearchRepository 接口
│   │   └── EsController.java            ← REST 接口
│   │
│   │   ══════ MongoDB ══════
│   ├── mongo/
│   │   ├── UserDocument.java            ← @Document 实体类
│   │   ├── UserMongoRepository.java     ← MongoRepository 接口
│   │   └── MongoController.java         ← REST 接口
│   │
│   │   ══════ 文件存储 ══════
│   ├── file/
│   │   └── MinioController.java         ← MinIO SDK 文件上传/下载/预签名
│   │
│   │   ══════ 定时任务 ══════
│   ├── task/
│   │   └── ScheduledTaskController.java ← @Scheduled + Redis 分布式锁防重复 + 动态 cron
│   │
│   │   ══════ 链路追踪 ══════
│   ├── tracing/
│   │   └── TracingDemo.java             ← Part A 原理模拟
│   │
│   │   ══════ 分布式事务 ══════
│   ├── transaction/
│   │   └── TransactionDemo.java         ← Part A 纯模拟
│   │
│   │   ══════ 分布式限流 ══════
│   ├── ratelimit/
│   │   └── RateLimitController.java     ← RedisTemplate + Lua 三种限流算法 + 压测对比
│   │
│   │   ══════ 分布式会话 ══════
│   ├── session/
│   │   ├── SessionController.java       ← Redis Session + JWT Token 对比
│   │   └── JwtUtil.java                 ← JWT 工具类
│   │
│   │   ══════ 缓存一致性 ══════
│   └── consistency/
│       └── CacheConsistencyController.java ← Cache Aside / Write Through / 延迟双删
│
├── src/main/resources/
│   ├── application.yml                  ← 默认配置（Consul + Redis + RabbitMQ + MySQL + ES + MongoDB + MinIO）
│   ├── application-consul.yml           ← Consul 注册发现配置
│   ├── application-nacos.yml            ← Nacos 注册发现配置（切换用）
│   ├── application-zk.yml              ← ZooKeeper 注册发现配置（切换用）
│   ├── application-rabbitmq.yml         ← RabbitMQ 配置
│   ├── application-kafka.yml            ← Kafka 配置
│   ├── application-sentinel.yml         ← Sentinel 熔断配置（切换用）
│   └── logback-spring.xml              ← 日志配置（含 traceId）
```

**模块 A 任务清单（2.3.A.1 ~ 2.3.A.26）**：

基础设施（2.3.A.1 ~ 2.3.A.6）：
- [x] 2.3.A.1 pom.xml — 更新 Spring Cloud 全家桶依赖 ✅
- [x] 2.3.A.2 SpringCloudApp.java — 启动类（@SpringBootApplication + @EnableFeignClients + @EnableDiscoveryClient + @EnableScheduling）✅
- [x] 2.3.A.3 application.yml — 默认配置（Consul + Redis + RabbitMQ + MySQL + ES + MongoDB + MinIO）✅
- [x] 2.3.A.4 application-consul.yml / application-nacos.yml / application-zk.yml — 注册中心 Profile 配置 ✅
- [x] 2.3.A.5 application-rabbitmq.yml / application-kafka.yml / application-sentinel.yml — 中间件 Profile 配置 ✅
- [x] 2.3.A.6 logback-spring.xml — 日志配置（含 traceId）✅

Spring Boot 基础实战（2.3.A.7 ~ 2.3.A.10）：
- [x] 2.3.A.7 boot/IoCController.java — @Autowired/@Qualifier/@Primary 注入方式演示 ✅
- [x] 2.3.A.8 boot/AopController.java — @Aspect 切面（日志/耗时/权限切面）✅
- [x] 2.3.A.9 boot/ExceptionController.java — @ControllerAdvice 全局异常处理 + 统一响应格式 ✅
- [x] 2.3.A.10 boot/ValidateController.java — @Valid + @Validated 参数校验 + 自定义校验注解 ✅

Spring Cloud 核心（2.3.A.11 ~ 2.3.A.14）：
- [x] 2.3.A.11 registry/RegistryController.java — DiscoveryClient 注册发现 ✅
- [x] 2.3.A.12 feign/UserFeignClient.java + UserFeignFallbackFactory.java + UserDTO.java + FeignController.java — 声明式调用 + Fallback ✅
- [x] 2.3.A.13 circuitbreaker/CircuitBreakerController.java — Resilience4j 熔断降级 ✅
- [x] 2.3.A.14 tracing/ — 链路追踪（已有 TracingDemo.java 不动，Micrometer 自动集成）✅

消息队列（2.3.A.15 ~ 2.3.A.17）：
- [x] 2.3.A.15 mq/RabbitMQController.java — RabbitTemplate + @RabbitListener ✅
- [x] 2.3.A.16 mq/KafkaController.java — KafkaTemplate + @KafkaListener ✅
- [ ]* 2.3.A.17 mq/MqttController.java — spring-integration-mqtt（可选） <!-- TODO-PENDING: 可选任务 -->

数据存储（2.3.A.18 ~ 2.3.A.22）：
- [x] 2.3.A.18 cache/RedisCacheController.java — RedisTemplate + @Cacheable + Redisson 分布式锁 ✅
- [x] 2.3.A.19 db/JdbcController.java — JdbcTemplate + HikariCP 连接池状态 ✅
- [x] 2.3.A.20 search/ArticleDocument.java + ArticleRepository.java + EsController.java — Spring Data ES ✅
- [x] 2.3.A.21 mongo/UserDocument.java + UserMongoRepository.java + MongoController.java — Spring Data MongoDB ✅
- [x] 2.3.A.22 file/MinioController.java — MinIO SDK 文件上传/下载/预签名 ✅

定时任务（2.3.A.23）：
- [x] 2.3.A.23 task/ScheduledTaskController.java — @Scheduled + Redis 分布式锁防重复 + 动态 cron ✅

分布式（2.3.A.24 ~ 2.3.A.26）：
- [x] 2.3.A.24 ratelimit/RateLimitController.java — RedisTemplate + Lua 三种限流算法 + 压测对比 ✅
- [x] 2.3.A.25 session/SessionController.java + JwtUtil.java — Redis Session + JWT Token 对比 ✅
- [x] 2.3.A.26 consistency/CacheConsistencyController.java — Cache Aside / Write Through / 延迟双删 ✅

**pom.xml 依赖清单**：

```
必须依赖：
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-cloud-starter-consul-discovery        （注册发现）
- spring-cloud-starter-openfeign               （声明式调用）
- spring-cloud-starter-loadbalancer            （负载均衡）
- spring-cloud-starter-circuitbreaker-resilience4j （熔断）
- spring-boot-starter-data-redis               （Redis 缓存）
- spring-boot-starter-amqp                     （RabbitMQ）
- spring-kafka                                 （Kafka）
- spring-boot-starter-jdbc                     （MySQL）
- mysql-connector-j                            （MySQL 驱动）
- spring-boot-starter-data-elasticsearch       （ES）
- spring-boot-starter-data-mongodb             （MongoDB）
- io.minio:minio                               （MinIO SDK）
- micrometer-tracing-bridge-brave              （链路追踪）

可选依赖（Profile 切换时启用）：
- spring-cloud-starter-alibaba-nacos-discovery  （Nacos 注册发现）
- spring-cloud-starter-zookeeper-discovery      （ZK 注册发现）
- spring-cloud-starter-alibaba-sentinel         （Sentinel 熔断）
- spring-integration-mqtt                       （MQTT）
- org.redisson:redisson-spring-boot-starter     （Redisson 分布式锁）
- io.jsonwebtoken:jjwt-api + jjwt-impl + jjwt-jackson （JWT Token）
- spring-session-data-redis                     （Spring Session，可选）
```

##### 2.3.4 实战项目 — 模块 B（springcloud-gateway，端口 8080，WebFlux）

> 独立模块原因：WebFlux 和 WebMVC 不能共存

```
springcloud-gateway/
├── pom.xml                              ← spring-cloud-starter-gateway + consul-discovery
├── src/main/java/com/example/gateway/
│   ├── GatewayApp.java                  ← @SpringBootApplication + @EnableDiscoveryClient
│   ├── config/
│   │   └── GatewayRouteConfig.java      ← Java 代码配置路由（RouteLocator）
│   └── filter/
│       ├── AuthGlobalFilter.java        ← 全局鉴权过滤器
│       ├── LogGlobalFilter.java         ← 全局日志过滤器
│       └── RateLimitFilter.java         ← Redis 令牌桶限流过滤器
├── src/main/resources/
│   └── application.yml                  ← 网关配置（路由规则 + Consul 注册）
```

**模块 B 任务清单（2.3.B.1 ~ 2.3.B.7）**：

- [x] 2.3.B.1 pom.xml — Gateway + Consul + Redis Reactive ✅
- [x] 2.3.B.2 GatewayApp.java — 启动类 ✅
- [x] 2.3.B.3 application.yml — 网关配置（路由规则 + Consul 注册）✅
- [x] 2.3.B.4 config/GatewayRouteConfig.java — Java 代码配置路由（RouteLocator）✅
- [x] 2.3.B.5 filter/AuthGlobalFilter.java — 全局鉴权过滤器 ✅
- [x] 2.3.B.6 filter/LogGlobalFilter.java — 全局日志过滤器 ✅
- [x] 2.3.B.7 filter/RateLimitFilter.java — Redis 令牌桶限流过滤器 ✅

**pom.xml 依赖**：
```
- spring-cloud-starter-gateway                 （网关核心，WebFlux）
- spring-cloud-starter-consul-discovery        （注册到 Consul）
- spring-boot-starter-data-redis-reactive      （限流用 Redis）
```

##### 2.3.5 实战项目 — 模块 C（springcloud-sharding，端口 8091）

> 独立模块原因：ShardingSphere 接管 DataSource，影响其他模块普通 SQL

```
springcloud-sharding/
├── pom.xml                              ← ShardingSphere Spring Boot Starter + MySQL + Consul
├── src/main/java/com/example/sharding/
│   ├── ShardingApp.java                 ← @SpringBootApplication + @EnableDiscoveryClient
│   ├── controller/
│   │   └── ShardingController.java      ← 分片 CRUD + 路由信息 + 方案对比
│   ├── entity/
│   │   └── OrderEntity.java             ← 订单实体
│   └── config/
│       └── ShardingDataSourceConfig.java ← ShardingSphere 数据源配置
├── src/main/resources/
│   ├── application.yml                  ← 默认配置（单库分表 + Consul 注册）
│   ├── application-db-table.yml         ← 分库分表配置（多库多表）
│   └── application-readwrite.yml        ← 读写分离配置
```

**模块 C 任务清单（2.3.C.1 ~ 2.3.C.6）**：

- [x] 2.3.C.1 pom.xml — ShardingSphere Spring Boot Starter + MySQL + Consul ✅
- [x] 2.3.C.2 ShardingApp.java — 启动类 ✅
- [x] 2.3.C.3 application.yml — 默认配置（单库分表 + Consul 注册）+ sharding-config.yaml ✅
- [x] 2.3.C.4 application-db-table.yml / application-readwrite.yml — 分库分表 / 读写分离 Profile ✅
- [x] 2.3.C.5 entity/OrderEntity.java — 订单实体 ✅
- [x] 2.3.C.6 controller/ShardingController.java — 分片 CRUD + 路由信息 + 方案对比 ✅

**pom.xml 依赖**：
```
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-cloud-starter-consul-discovery        （注册到 Consul）
- org.apache.shardingsphere:shardingsphere-jdbc-spring-boot-starter （分库分表核心）
- mysql-connector-j                            （MySQL 驱动）
- spring-boot-starter-jdbc                     （JDBC）
```

功能覆盖：
- 默认 Profile：单库分表（order_0~order_3，按 order_id 取模）
- `--spring.profiles.active=db-table`：分库分表（ds_0/ds_1 × order_0~order_3）
- `--spring.profiles.active=readwrite`：读写分离（主写从读）
- 分布式主键：雪花算法自动生成
- 广播表 + 绑定表

使用场景注释：
- 单表 > 500 万行 → 分表
- 单库 QPS > 5000 或数据量 > 1 亿 → 分库分表
- 读写比 > 7:3 → 读写分离

##### 2.3.6 实战项目 — 模块 D（springcloud-user-service，端口 8092）

> 独立模块原因：独立微服务，被 Feign 调用，演示真实跨服务调用链路

```
springcloud-user-service/
├── pom.xml                              ← spring-boot-starter-web + consul-discovery + jdbc + redis
├── src/main/java/com/example/user/
│   ├── UserServiceApp.java              ← @SpringBootApplication + @EnableDiscoveryClient
│   ├── controller/
│   │   └── UserController.java          ← REST 接口（被 Feign 调用的提供方）
│   ├── entity/
│   │   └── User.java                   ← 用户实体
│   └── service/
│       └── UserService.java            ← 业务逻辑（JdbcTemplate + Redis 缓存）
├── src/main/resources/
│   └── application.yml                  ← 端口 8092 + Consul 注册
```

**模块 D 任务清单（2.3.D.1 ~ 2.3.D.5）**：

- [x] 2.3.D.1 pom.xml — spring-boot-starter-web + consul-discovery + jdbc + redis ✅
- [x] 2.3.D.2 UserServiceApp.java — 启动类 ✅
- [x] 2.3.D.3 application.yml — 端口 8092 + Consul 注册（spring.application.name=user-service）✅
- [x] 2.3.D.4 entity/User.java + service/UserService.java — 用户实体 + 业务逻辑（JdbcTemplate + Redis 缓存）✅
- [x] 2.3.D.5 controller/UserController.java — REST 接口（GET/POST /users，被 Feign 调用）✅

Feign 调用说明：
- springcloud-examples 中的 UserFeignClient 声明 `@FeignClient(name = "user-service")`
- 通过 Consul 服务发现找到 user-service(8092) 的实例
- 演示完整链路：服务注册 → 服务发现 → 负载均衡 → Feign 调用 → Fallback 降级
- user-service 未启动时自动触发 FallbackFactory 返回降级数据

完整微服务调用链路：
```
客户端 → Gateway(8080) → springcloud-demo(8090) → [Feign] → user-service(8092)
                                                  → [Feign] → sharding-service(8091)
```

四个模块都注册到 Consul，Gateway 可路由到所有服务。

后期扩展预留： <!-- TODO-PENDING: 后期扩展 -->
- Spring Security → springcloud-examples 加 Profile `security` <!-- TODO-PENDING: 后期扩展 -->
- Seata 分布式事务 → 新建 `springcloud-seata/` 模块 <!-- TODO-PENDING: 后期扩展 -->

##### 2.3.7 md 文档同步更新（18 篇文档添加"在 Spring Cloud 项目中体验"章节）

Spring Boot 文档（docs/2-framework/2.2-springboot/）：
- [x] 01-ioc-di.md — 更新代码示例链接指向 springcloud-examples/boot/IoCController.java ✅
- [x] 02-aop.md — 更新代码示例链接指向 springcloud-examples/boot/AopController.java ✅
- [x] 07-web.md — 更新代码示例链接，补充全局异常处理和参数校验 ✅
- [x] 10-logging.md — 更新链接，补充 traceId 日志配置 ✅
- [x] 11-cache.md — 更新链接指向 RedisCacheController ✅
- [x] 12-task.md — 更新链接指向 ScheduledTaskController ✅
- [x] 13-actuator.md — 更新链接，补充 Prometheus 指标导出 ✅

Spring Cloud 文档（docs/2-framework/2.3-springcloud/）：
- [x] 01-registry.md — 更新代码示例链接指向 RegistryController ✅
- [x] 02-loadbalancer.md — 补充 Spring Cloud LoadBalancer 实战说明 ✅
- [x] 03-feign.md — 更新链接指向 FeignController + UserFeignClient ✅
- [x] 04-circuit-breaker.md — 更新链接指向 CircuitBreakerController ✅
- [x] 05-gateway.md — 更新链接指向 springcloud-gateway 模块 ✅
- [x] 06-config.md — 补充 Consul KV 配置中心实战 ✅
- [x] 07-tracing.md — 补充 Micrometer Tracing 实战配置 ✅

其他 md 文档更新：
- [x] `docs/3-data-store/3.1-database/05-sharding.md` — 补充 ShardingSphere Spring Boot 实战 + 方案对比 + Mermaid 图 ✅
- [x] `docs/5-distributed/5.1-distributed/06-rate-limiting.md` — 补充 Redis+Lua 限流实战 + 方案对比 ✅
- [x] `docs/8-architecture/06-distributed-session.md` — 补充 Redis Session + JWT 实战 + 方案对比 ✅
- [x] `docs/8-architecture/08-cache-db-consistency.md` — 补充双写一致性实战 + 方案对比 ✅

每个 md 文档底部补充：
```markdown
## 在 Spring Cloud 项目中体验

启动 Spring Cloud 项目后，通过 REST 接口直接验证：
\`\`\`bash
# 启动中间件
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.consul.yml up -d

# 启动项目
cd code-examples/02-framework/springcloud-examples
mvn spring-boot:run

# 验证接口
curl http://localhost:8090/demo/xxx
\`\`\`
```

#### 中间件可切换方案

通过 `--spring.profiles.active=xxx` 切换，代码层不变：

| 模块 | 默认实现 | 可切换 | Profile 参数 | 统一接口 | 需要的依赖 |
|------|---------|--------|-------------|---------|-----------|
| 注册发现 | Consul | Nacos | `--spring.profiles.active=nacos` | DiscoveryClient | spring-cloud-starter-alibaba-nacos-discovery |
| 注册发现 | Consul | ZooKeeper | `--spring.profiles.active=zk` | DiscoveryClient | spring-cloud-starter-zookeeper-discovery |
| 配置中心 | Consul KV | Nacos Config | `nacos-config` | @Value + @RefreshScope | spring-cloud-starter-alibaba-nacos-config |
| 消息队列 | RabbitMQ | Kafka | 两个都启用，各自 Controller | RabbitTemplate / KafkaTemplate | spring-boot-starter-amqp / spring-kafka |
| 缓存 | Redis(Lettuce) | Caffeine(本地) | `caffeine` | @Cacheable | spring-boot-starter-cache + caffeine |
| 数据库 | MySQL(HikariCP) | MySQL(Druid) | `druid` | JdbcTemplate | druid-spring-boot-starter |
| 熔断 | Resilience4j | Sentinel | `sentinel` | @CircuitBreaker | spring-cloud-starter-alibaba-sentinel |
| 链路追踪 | Micrometer→控制台 | Micrometer→Zipkin | `zipkin` | 自动集成 | micrometer-tracing-bridge-brave + zipkin-reporter |

#### 不加入 Spring Cloud 项目的模块 <!-- TODO-PENDING: 未开发，保持现状 -->

| 模块 | 原因 | 保持位置 |
|------|------|---------|
| MinIO 原理模拟 | 用原生 SDK，在 SC 项目中只封装 REST 接口 | minio-examples（Part A 原理模拟） |
| Spring Security | 太重，独立学习更好 | 后续单独模块 |
| Apollo 配置中心 | 用 Consul KV 代替 | config-center-examples（纯模拟） |
| Seata 分布式事务 | 需要 Seata Server + 多微服务，太重 | springcloud-examples/transaction（纯模拟） |

#### 方案对比设计

##### 分库分表（模块 C：springcloud-sharding）

功能覆盖：
- 单库分表：order_0~order_3，按 order_id 取模
- 分布式主键：雪花算法自动生成
- 广播表：config 表所有分片同步
- 方案对比（Javadoc + 接口返回）：ShardingSphere-JDBC / Proxy / MyCat / Vitess / TiDB

##### 分布式限流（模块 A：springcloud-examples/ratelimit/）

方案对比（Javadoc + 接口返回）：

| 方案 | 精度 | 实现复杂度 | 适用场景 |
|------|------|-----------|---------|
| 固定窗口 | 低（边界突刺） | 简单 | 粗粒度限流 |
| 滑动窗口 | 高 | 中等 | API 限流 |
| 令牌桶 | 高（允许突发） | 较复杂 | 流量整形 |
| Sentinel | 高 | 低（框架） | Spring Cloud 集成 |
| Gateway RequestRateLimiter | 高 | 低（配置） | 网关层限流 |

##### 分布式会话（模块 A：springcloud-examples/session/）

方案对比（Javadoc + 接口返回）：

| 方案 | 状态 | 扩展性 | 安全性 | 适用场景 |
|------|------|--------|--------|---------|
| Redis Session | 有状态 | 需 Redis | 服务端控制 | 传统 Web |
| JWT Token | 无状态 | 天然分布式 | 无法主动失效 | 微服务/移动端 |
| JWT + Redis | 混合 | 需 Redis | 可主动失效 | 兼顾两者 |
| Spring Session | 有状态 | 框架封装 | 服务端控制 | Spring 项目 |

##### 缓存一致性（模块 A：springcloud-examples/consistency/）

方案对比（Javadoc + 接口返回）：

| 方案 | 一致性 | 复杂度 | 性能 | 适用场景 |
|------|--------|--------|------|---------|
| Cache Aside | 最终一致 | 简单 | 高 | 大多数场景（推荐） |
| Write Through | 强一致 | 中等 | 中 | 一致性要求高 |
| Write Behind | 最终一致 | 复杂 | 最高 | 写密集场景 |
| 延迟双删 | 最终一致 | 中等 | 高 | 并发更新场景 |
| Binlog 同步 | 最终一致 | 较复杂 | 高 | 大规模系统 |

#### Docker 启动命令

```bash
# 启动全部中间件（一键启动）
docker compose -f docker/docker-compose.yml up -d          # MySQL + Redis + MongoDB + MinIO
docker compose -f docker/docker-compose.consul.yml up -d    # Consul
docker compose -f docker/docker-compose.mq.yml up -d        # RabbitMQ + Kafka + ZooKeeper
docker compose -f docker/docker-compose.es.yml up -d        # Elasticsearch
```

#### 启动与验证流程

```bash
# 1. 启动中间件
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.consul.yml up -d
docker compose -f docker/docker-compose.mq.yml up -d
docker compose -f docker/docker-compose.es.yml up -d

# 2. 启动业务服务（默认 Consul + RabbitMQ + Resilience4j）
cd code-examples/02-framework/springcloud-examples
mvn spring-boot:run

# 3. 可选：启动网关
cd code-examples/02-framework/springcloud-gateway
mvn spring-boot:run

# 4. 可选：启动分库分表服务
cd code-examples/02-framework/springcloud-sharding
mvn spring-boot:run

# 5. 可选：启动用户服务
cd code-examples/02-framework/springcloud-user-service
mvn spring-boot:run

# 6. 验证接口
curl http://localhost:8090/demo/registry/services          # 注册发现
curl http://localhost:8090/demo/cache/set?key=test&value=hello  # Redis
curl -X POST http://localhost:8090/demo/mq/rabbit/send?msg=hello  # RabbitMQ
curl http://localhost:8090/demo/db/init                    # MySQL 初始化
curl http://localhost:8090/demo/es/init                    # ES 初始化
curl http://localhost:8090/demo/es/search?keyword=Java     # ES 搜索
curl http://localhost:8091/demo/sharding/init              # 分库分表初始化
curl http://localhost:8090/actuator/health                 # 健康检查

# 7. 切换注册中心为 Nacos
mvn spring-boot:run -Dspring-boot.run.profiles=nacos

# 8. 切换分库分表模式
cd code-examples/02-framework/springcloud-sharding
mvn spring-boot:run -Dspring-boot.run.profiles=db-table    # 分库分表
mvn spring-boot:run -Dspring-boot.run.profiles=readwrite   # 读写分离
```

#### 检查点

- [x] 2.x 检查点 - 框架层验证
  - Spring Cloud 编译通过、4 个微服务模块可启动、18 篇文档链接有效

### 阶段 3：数据存储（docs/3-data-store/ → code-examples/03-data-store/）

> 合并原阶段 5 数据库 + 阶段 8 ES/MongoDB/MinIO + 阶段 13 批次 1/2/5 的代码质量改进 + 阶段 15 Redis Part B 补充。
> 每个子模块一次性完成：md 文档 + 高质量 Demo（Part A 原理模拟 + Part B 真实连接）。

#### 3.1 MySQL 数据库（docs/3-data-store/3.1-database/ → 03-data-store/database-examples/）

- [x] 3.1 MySQL 数据库模块（文档 + 8 个高质量 Demo + Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `index.md` | B+树/索引原理 |
  | `transaction.md` | 事务/隔离级别/MVCC |
  | `lock.md` | 行锁/间隙锁/临键锁 |
  | `optimization.md` | SQL 优化/EXPLAIN |
  | `sharding.md` | 分库分表/ShardingSphere |
  | `binlog.md` | Binlog/Canal/主从同步 |
  | `log-system.md` | Redo/Undo Log/Buffer Pool |
  | `high-availability.md` | 主从/MGR/Proxy |
  | `distributed-id.md` | 雪花算法/Leaf |
  | `pool.md` | HikariCP/Druid |
  | `interview.md` | 数据库面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 1 的代码改进 + Part B 补充）：

  路径前缀：`code-examples/03-data-store/database-examples/src/main/java/com/example/database/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `index_demo/IndexDemo.java` | B+树索引查找、覆盖索引、最左前缀匹配 | 纯模拟 |
  | `transaction/TransactionDemo.java` | MVCC 版本链+四种隔离级别并发行为 | 纯模拟 |
  | `lock/LockDemo.java` | 行锁/间隙锁/临键锁/死锁检测 | 纯模拟 |
  | `optimization/OptimizationDemo.java` | 慢查询分析+EXPLAIN+成本估算 | 混合（Part A 模拟 + Part B JDBC 真实 EXPLAIN） |
  | `binlog/BinlogDemo.java` | Binlog 格式+主从复制+PITR+Canal 同步 | 混合（Part A 模拟 + Part B mysql-binlog-connector 真实监听） |
  | `pool/ConnectionPoolDemo.java` | Semaphore+BlockingQueue 连接池 | 混合（Part A 模拟 + Part B HikariCP 真实连接池+并发压测） |
  | `sharding/ShardingDemo.java` | 取模/范围/一致性哈希三种分片对比 | 纯模拟 |
  | `id/DistributedIdDemo.java` | UUID/自增/雪花/号段四种方案对比 | 纯模拟 |

  **pom.xml 依赖**：`mysql-connector-j`、`HikariCP`、`mysql-binlog-connector-java`（Part B 用）
  **Docker 启动**：`docker compose -f docker/docker-compose.yml up -d mysql`

#### 3.2 Redis（docs/3-data-store/3.2-redis/ → 03-data-store/redis-examples/）

- [x] 3.2 Redis 模块（文档 + Demo 含 Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `data-structures.md` | 数据结构与底层实现 |
  | `persistence.md` | RDB/AOF 持久化 |
  | `replication.md` | 主从/哨兵/Cluster |
  | `cache-problems.md` | 穿透/击穿/雪崩 |
  | `distributed-lock.md` | 分布式锁实现 |
  | `spring-integration.md` | Spring Boot 集成 |
  | `interview.md` | Redis 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 5 改进 + 阶段 15 Part B 补充）：

  路径前缀：`code-examples/03-data-store/redis-examples/src/main/java/com/example/redis/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `datastructure/DataStructureDemo.java` | 模拟五种结构+编码切换 + Jedis 真实操作 | 混合 |
  | `cache/CacheProblemsDemo.java` | 缓存穿透/击穿/雪崩模拟 + Jedis 空值/互斥锁/随机过期 | 混合 |
  | `lock/DistributedLockDemo.java` | 分布式锁原理 + Jedis SET NX EX + Lua 释放锁 + 锁竞争 | 混合 |
  | `spring/RedisIntegrationDemo.java` | Spring 集成模拟 + Jedis 五种数据类型操作 + Pipeline 批量 | 混合 |

  **pom.xml 依赖**：`jedis`
  **Docker 启动**：`docker compose -f docker/docker-compose.yml up -d redis`

#### 3.3 Elasticsearch（docs/3-data-store/3.3-elasticsearch/ → 03-data-store/elasticsearch-examples/）

- [x] 3.3 Elasticsearch 模块（文档 + 5 个混合 Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `inverted-index.md` | 倒排索引原理 |
  | `mapping.md` | 映射与分析器 |
  | `crud.md` | CRUD 操作 |
  | `dsl-query.md` | DSL 复合查询 |
  | `aggregation.md` | 聚合分析 |
  | `spring-data.md` | Spring Data ES 集成 |
  | `interview.md` | ES 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 2 改进 + Part B 补充）：

  路径前缀：`code-examples/03-data-store/elasticsearch-examples/src/main/java/com/example/es/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `aggregation/AggregationDemo.java` | Stream 模拟聚合 + ES RestClient 真实 terms/stats/range/嵌套聚合 | 混合 |
  | `query/QueryDemo.java` | Predicate 模拟查询 + ES RestClient 真实 match/term/range/bool 查询 | 混合 |
  | `crud/CrudDemo.java` | 版本控制+乐观锁模拟 + ES RestClient 真实 CRUD+Bulk | 混合 |
  | `index_demo/IndexDemo.java` | 倒排索引+分词+TF-IDF + ES 索引管理+Analyze API | 混合 |
  | `spring/SpringDataDemo.java` | 模拟 Repository+分页 + ES Client 模拟 Repository 查询 | 混合 |

  **pom.xml 依赖**：`elasticsearch-rest-client`、`elasticsearch-java`
  **Docker 启动**：`docker compose -f docker/docker-compose.es.yml up -d elasticsearch`

#### 3.4 MongoDB（docs/3-data-store/3.4-mongodb/ → 03-data-store/mongodb-examples/）

- [x] 3.4 MongoDB 模块（文档 + Demo 含 Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `document-model.md` | 文档模型/BSON/Schema 设计 |
  | `crud.md` | CRUD 操作/查询优化 |
  | `aggregation.md` | 聚合管道/MapReduce |
  | `index.md` | 索引类型/复合索引/TTL 索引 |
  | `spring-data.md` | Spring Data MongoDB 集成 |
  | `interview.md` | MongoDB 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 5 改进）：

  路径前缀：`code-examples/03-data-store/mongodb-examples/src/main/java/com/example/mongodb/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `MongoDBDemo.java` | 模拟文档 CRUD+聚合 + MongoClient 真实操作 | 混合 |

  **pom.xml 依赖**：`mongodb-driver-sync`
  **Docker 启动**：`docker compose -f docker/docker-compose.yml up -d mongodb`

#### 3.5 MinIO（docs/3-data-store/3.5-minio/ → 03-data-store/minio-examples/）

- [x] 3.5 MinIO 模块（文档 + Demo 含 Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `architecture.md` | MinIO 架构/对象存储原理 |
  | `bucket-management.md` | 桶管理/权限策略 |
  | `file-operations.md` | 文件上传下载/分片上传 |
  | `interview.md` | 对象存储面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 5 改进）：

  路径前缀：`code-examples/03-data-store/minio-examples/src/main/java/com/example/minio/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `MinIODemo.java` | 模拟桶管理+分片上传 + MinioClient 真实操作 | 混合 |

  **pom.xml 依赖**：`minio`
  **Docker 启动**：`docker compose -f docker/docker-compose.yml up -d minio`

### 阶段 4：中间件（docs/4-middleware/ → code-examples/04-middleware/）

> 合并原阶段 6 RabbitMQ/Kafka/注册中心/配置中心 + 阶段 8 Nginx/MQTT + 阶段 13 批次 3/4/7 的代码质量改进。
> 每个子模块一次性完成：md 文档 + 高质量 Demo（Part A + Part B）。

#### 4.1 RabbitMQ（docs/4-middleware/4.1-mq-rabbitmq/ → 04-middleware/mq-rabbitmq-examples/）

- [x] 4.1 RabbitMQ 模块（文档 + 4 个混合 Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `rabbitmq.md` | RabbitMQ 核心概念 |
  | `rabbitmq-reliability.md` | RabbitMQ 消息可靠性/幂等性 |
  | `rabbitmq-advanced.md` | 死信队列/延迟消息/优先级队列 |
  | `rabbitmq-spring.md` | Spring Boot 集成 RabbitMQ |
  | `interview.md` | RabbitMQ 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 3 改进 + Part B 补充）：

  路径前缀：`code-examples/04-middleware/mq-rabbitmq-examples/src/main/java/com/example/mq/rabbitmq/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `core/RabbitMQCoreDemo.java` | Exchange 路由模拟 + amqp-client 真实 Direct/Topic/Fanout 收发 | 混合 |
  | `reliability/ReliabilityDemo.java` | Confirm/Return/ACK/幂等/重试模拟 + amqp-client 真实 Confirm+ACK | 混合 |
  | `advanced/AdvancedDemo.java` | DelayQueue 延迟消息+死信+优先级 + 真实 TTL+DLX+优先级队列 | 混合 |
  | `spring/SpringIntegrationDemo.java` | 模拟 Template/Converter/Container + amqp-client JSON 收发 | 混合 |

  **pom.xml 依赖**：`amqp-client`
  **Docker 启动**：`docker compose -f docker/docker-compose.mq.yml up -d rabbitmq`

#### 4.2 Kafka（docs/4-middleware/4.2-mq-kafka/ → 04-middleware/mq-kafka-examples/）

- [x] 4.2 Kafka 模块（文档 + 4 个混合 Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `kafka.md` | Kafka 架构与原理 |
  | `kafka-reliability.md` | Kafka 消息可靠性/顺序性 |
  | `kafka-advanced.md` | 分区策略/消费者组/Rebalance |
  | `kafka-spring.md` | Spring Boot 集成 Kafka |
  | `interview.md` | Kafka 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 4 改进）：

  路径前缀：`code-examples/04-middleware/mq-kafka-examples/src/main/java/com/example/mq/kafka/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `core/KafkaCoreDemo.java` | 模拟 Topic/Partition/Offset/ConsumerGroup + kafka-clients 真实生产消费 | 混合 |
  | `reliability/KafkaReliabilityDemo.java` | 模拟 acks/ISR/幂等 + 不同 acks 耗时对比 | 混合 |
  | `advanced/KafkaAdvancedDemo.java` | 模拟 Range/RoundRobin/Sticky 分配+Rebalance+Exactly-Once | 混合 |
  | `spring/KafkaSpringDemo.java` | 模拟 KafkaTemplate/@KafkaListener + 配置最佳实践 | 混合 |

  **pom.xml 依赖**：`kafka-clients`
  **Docker 启动**：`docker compose -f docker/docker-compose.mq.yml up -d kafka`

#### 4.3 MQTT（docs/4-middleware/4.3-mq-mqtt/ → 04-middleware/mq-mqtt-examples/）

- [x] 4.3 MQTT 模块（文档 + Demo 含 Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `mqtt-protocol.md` | MQTT 协议原理/QoS 级别 |
  | `mqtt-broker.md` | EMQX/Mosquitto Broker 部署 |
  | `mqtt-spring.md` | Spring Boot 集成 MQTT |
  | `interview.md` | MQTT/IoT 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 4 改进）：

  路径前缀：`code-examples/04-middleware/mq-mqtt-examples/src/main/java/com/example/mqtt/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `MQTTDemo.java` | 模拟发布订阅/QoS/遗嘱/保留消息 + Paho MQTT 真实收发 | 混合 |

  **pom.xml 依赖**：`org.eclipse.paho.client.mqttv3`
  **Docker 启动**：`docker compose -f docker/docker-compose.mq.yml up -d rabbitmq`（MQTT 插件）

#### 4.4 配置中心（docs/4-middleware/4.4-config-center/ → 04-middleware/config-center-examples/）

- [x] 4.4 配置中心模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `apollo.md` | Apollo 架构/热更新/灰度 |
  | `nacos-config.md` | Nacos Config 使用 |
  | `comparison.md` | Apollo vs Nacos vs Spring Cloud Config |
  | `security.md` | 配置加密与安全 |
  | `interview.md` | 配置中心面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 7 改进）：

  路径前缀：`code-examples/04-middleware/config-center-examples/src/main/java/com/example/middleware/config/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `apollo/ApolloDemo.java` | 命名空间+多环境+热更新+灰度发布 | 纯模拟 |
  | `nacos/NacosConfigDemo.java` | 长轮询+配置监听+动态刷新 | 纯模拟 |
  | `security/ConfigSecurityDemo.java` | AES/RSA 加密+Jasypt+密钥轮转 | 纯模拟 |

  **Docker 启动**：`docker compose -f docker/docker-compose.apollo.yml up -d`（Apollo，模拟为主）

#### 4.5 注册中心（docs/4-middleware/4.5-registry/ → 04-middleware/registry-examples/）

- [x] 4.5 注册中心模块（文档 + Demo 含 Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `principles.md` | 服务注册与发现原理 |
  | `consul.md` | Consul 架构/健康检查/ACL |
  | `zookeeper.md` | ZAB/临时节点/Watcher |
  | `nacos.md` | Nacos AP/CP 切换 |
  | `comparison.md` | 注册中心选型对比表 |
  | `interview.md` | 注册中心面试指南 |

  Consul 优先，代码示例在 04-middleware/registry-examples 子模块中

  **Java Demo 文件清单**（高质量版本，含原批次 7 改进）：

  路径前缀：`code-examples/04-middleware/registry-examples/src/main/java/com/example/middleware/registry/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `consul/ConsulDemo.java` | 模拟注册/发现/KV + consul-api 真实操作 | 混合 |
  | `nacos/NacosDemo.java` | AP/CP 模式+分组+命名空间+权重路由 | 纯模拟 |
  | `zookeeper/ZookeeperDemo.java` | 模拟 ZNode 树 + Curator 真实操作 | 混合 |

  **pom.xml 依赖**：`consul-api`、`curator-framework`
  **Docker 启动**：
  - Consul：`docker compose -f docker/docker-compose.consul.yml up -d consul`
  - ZooKeeper：`docker compose -f docker/docker-compose.mq.yml up -d zookeeper`

#### 4.6 Nginx（docs/4-middleware/4.6-nginx/ → 04-middleware/nginx-examples/）

- [x] 4.6 Nginx 模块（文档 + 配置示例）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `architecture.md` | Master-Worker 模型 |
  | `reverse-proxy.md` | 反向代理配置 |
  | `load-balance.md` | 负载均衡策略 |
  | `https.md` | HTTPS/证书管理 |
  | `rate-limit.md` | 限流防刷 |
  | `cors.md` | 跨域配置 |
  | `advanced.md` | OpenResty/高可用/性能调优 |
  | `interview.md` | Nginx 面试指南 |

  代码示例为 Nginx 配置文件，放在 04-middleware/nginx-examples/conf/ 下

  **Docker 启动**：`docker compose -f docker/docker-compose.nginx.yml up -d`

### 阶段 5：分布式（docs/5-distributed/ → code-examples/05-distributed/）

> 合并原阶段 8 分布式理论 + 阶段 13 批次 8 的代码质量改进。

#### 5.1 分布式系统理论（docs/5-distributed/5.1-distributed/ → 05-distributed/distributed-examples/）

- [x] 5.1 分布式系统理论模块（文档 + Demo 含 Part B）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `cap-base.md` | CAP/BASE 理论 |
  | `consensus.md` | Raft/Paxos 算法 |
  | `distributed-lock.md` | Redis/ZK/MySQL 分布式锁 |
  | `distributed-transaction.md` | 2PC/TCC/Saga/消息一致性 |
  | `idempotent.md` | 幂等性设计 |
  | `rate-limiting.md` | 令牌桶/漏桶/滑动窗口 |
  | `interview.md` | 分布式面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 8 改进）：

  路径前缀：`code-examples/05-distributed/distributed-examples/src/main/java/com/example/distributed/`

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `DistributedTransactionDemo.java` | 2PC/TCC/Saga 状态机+对比 | 纯模拟 |
  | `DistributedLockCompare.java` | 模拟三种锁 + Jedis 真实 Redis 锁 | 混合 |

  **pom.xml 依赖**：`jedis`（Part B 用）
  **Docker 启动**：`docker compose -f docker/docker-compose.yml up -d redis`

### 阶段 6：DevOps（docs/6-devops/ → code-examples/06-devops/）

> 合并原阶段 8 Linux/Docker/CI/CD/监控 + 阶段 13 批次 8 的代码质量改进。
> 每个子模块一次性完成：md 文档 + 高质量 Demo（Part A + Part B）。

#### 6.1 Docker 与 K8s（docs/6-devops/6.1-docker-k8s/ → 06-devops/docker-k8s-examples/）

- [x] 6.1 Docker 与 K8s 模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `docker-basics.md` | 镜像/容器/仓库 |
  | `dockerfile.md` | 多阶段构建/镜像瘦身 |
  | `docker-network.md` | bridge/host/overlay |
  | `docker-compose.md` | 多服务编排 |
  | `java-docker.md` | JVM 容器调优 |
  | `k8s-architecture.md` | K8s 架构/核心组件 |
  | `k8s-resources.md` | Pod/Deployment/Service |
  | `k8s-health.md` | 健康检查/探针 |
  | `k8s-deploy.md` | 滚动更新/蓝绿/金丝雀 |
  | `k8s-hpa.md` | HPA 自动扩缩容 |
  | `helm.md` | Helm 包管理 |
  | `cheatsheet.md` | Docker/K8s 命令速查表 |
  | `interview.md` | Docker/K8s 面试指南 |

#### 6.2 CI/CD（docs/6-devops/6.2-cicd/ → 06-devops/cicd-examples/）

- [x] 6.2 CI/CD 模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `jenkins.md` | Jenkins Pipeline/Jenkinsfile |
  | `github-actions.md` | GitHub Actions 工作流 |
  | `gitlab-ci.md` | GitLab CI/CD 配置 |
  | `best-practices.md` | CI/CD 最佳实践/流水线设计 |
  | `interview.md` | CI/CD 面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 8 改进）：

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `CICDDemo.java` | 流水线模拟+并行步骤+GitHub Actions 配置 | 纯模拟 |

#### 6.3 监控体系（docs/6-devops/6.3-monitoring/ → 06-devops/monitoring-examples/）

- [x] 6.3 监控体系模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `prometheus.md` | Prometheus 指标采集/PromQL |
  | `grafana.md` | Grafana 仪表盘/告警配置 |
  | `micrometer.md` | Micrometer + Spring Boot Actuator |
  | `log-monitoring.md` | ELK/Loki 日志监控方案 |
  | `interview.md` | 监控体系面试指南 |

  **Java Demo 文件清单**（高质量版本，含原批次 8 改进）：

  | Demo 文件 | 内容 | 模式 |
  |-----------|------|------|
  | `MonitoringDemo.java` | Counter/Gauge/Timer + Prometheus 格式 | 纯模拟 |

#### 6.4 Linux 运维（docs/6-devops/6.4-linux/，无独立代码子模块）

- [x] 6.4 Linux 运维模块（文档）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `commands.md` | 常用命令速查 |
  | `shell.md` | Shell 脚本基础 |
  | `performance.md` | top/vmstat/iostat/netstat |
  | `log-analysis.md` | grep/awk/sed |
  | `jvm-troubleshooting.md` | CPU 飙高/OOM/死锁排查 |
  | `interview.md` | Linux 面试指南 |

### 阶段 7：AI 应用（docs/7-ai/ → code-examples/07-ai/）

#### 7.1 AI 应用（docs/7-ai/7.1-ai/ → 07-ai/ai-examples/）

- [x] 7.1 AI 应用模块（文档 + Demo）

  **md 文档清单**：

  | 文档文件 | 知识点 |
  |---------|--------|
  | `spring-ai.md` | Spring AI 框架 |
  | `llm-integration.md` | LLM API 集成 |
  | `rag.md` | RAG 检索增强生成 |
  | `vector-db.md` | 向量数据库集成 |
  | `prompt.md` | Prompt Engineering |
  | `agent.md` | AI Agent 开发 |
  | `comparison.md` | Spring AI vs LangChain4j |
  | `interview.md` | AI 面试指南 |

  **Java Demo 文件**：AI 模块 4 个文件 println 均 ≤ 11%，质量良好

### 阶段 8：架构设计场景（docs/8-architecture/，跨多个 code-examples 子模块）

#### 8.1 架构设计场景（文档 + 跨模块代码示例）

- [x] 8.1 架构设计场景模块

  **md 文档清单**：

  | 文档文件 | 知识点 | 关联代码子模块 |
  |---------|--------|--------------|
  | `seckill.md` | 秒杀系统设计 | redis-examples, mq-rabbitmq-examples |
  | `short-url.md` | 短链接系统 | database-examples |
  | `order-timeout.md` | 订单超时取消 | mq-rabbitmq-examples, redis-examples |
  | `cache-strategy.md` | 分布式缓存方案 | redis-examples |
  | `idempotent-design.md` | 接口幂等性方案 | distributed-examples |
  | `distributed-session.md` | 分布式 Session | redis-examples |
  | `file-upload.md` | 大文件上传 | springboot-examples |
  | `cache-db-consistency.md` | 缓存与 DB 双写一致性 | redis-examples |
  | `interview.md` | 架构设计面试指南 | — |

### 阶段 9：学习路径与面试汇总（docs/learning-paths/ + docs/interview/）

#### 9.1 学习路径文档

- [x] 9.1 创建学习路径文档
  - `docs/learning-paths/beginner.md` — Java 初学者路径
  - `docs/learning-paths/intermediate.md` — Java 中级进阶路径
  - `docs/learning-paths/advanced.md` — Java 高级深入路径
  - `docs/learning-paths/interview-sprint.md` — 面试突击路径
  - `docs/learning-paths/architect.md` — 架构师成长路径

#### 9.2 面试汇总文档

- [x] 9.2 创建面试汇总文档
  - `docs/interview/by-company.md` — 按公司类型分类面试重点
  - `docs/interview/knowledge-map.md` — 面试知识图谱

### 阶段 10：导航系统与标签体系

#### 10.1 VitePress 侧边栏和导航

- [x] 10.1 完善 VitePress 侧边栏和导航配置
  - 更新 `docs/.vitepress/sidebar.mts` 和 `docs/.vitepress/config.mts`

#### 10.2 frontmatter 和标签校验

- [x] 10.2 校验所有文档的 frontmatter 和标签
  - 检查 frontmatter 必填字段、标签合法性、代码示例链接有效性

### 阶段 11：最终检查点

- [x] 11. 最终检查点 - 全面验证
  - `mvn compile` 通过、VitePress 构建成功、文档格式统一、链接有效、面试指南完整

### 可选/后续任务 <!-- TODO-PENDING: 以下全部为待处理的可选任务 -->

#### 网络编程模块（当前质量可以，可选补充） <!-- TODO-PENDING: 可选优化 -->

路径前缀：`code-examples/02-framework/network-programming/src/main/java/com/example/network/`

- [ ]* `http/HttpDemo.java`（16%）→ 可选：补充真实 HTTP 请求示例 <!-- TODO-PENDING: 可选任务 -->
- [ ]* `tcp/TCPDemo.java`（9%）→ 质量良好，暂不改动 <!-- TODO-PENDING: 暂不改动 -->
- [ ]* `websocket/WebSocketDemo.java`（7%）→ 质量良好，暂不改动 <!-- TODO-PENDING: 暂不改动 -->
- [ ]* `rpc/RPCDemo.java`（5%）→ 质量良好，暂不改动 <!-- TODO-PENDING: 暂不改动 -->

#### Java 核心模块（边界值可选优化） <!-- TODO-PENDING: 可选优化 -->

- [ ]* `java-advanced/JMMDemo.java`（28%）— 边界值，可选优化 <!-- TODO-PENDING: 可选任务 -->
- [ ]* `java-advanced/CollectionSourceDemo.java`（24%）— 边界值，可选优化 <!-- TODO-PENDING: 可选任务 -->
- [ ]* `java-advanced/SerializationDemo.java`（25%）— 边界值，可选优化 <!-- TODO-PENDING: 可选任务 -->

#### 其他可选 <!-- TODO-PENDING: 可选任务 -->

- [ ]* 2.3.A.17 mq/MqttController.java — spring-integration-mqtt（可选） <!-- TODO-PENDING: 可选任务 -->
- [ ]* 1.1.opt 编写 Java 基础模块单元测试 — JUnit 5 + AssertJ <!-- TODO-PENDING: 可选任务 -->

#### 暂不改动的模块 <!-- TODO-PENDING: 暂不改动，后续视情况优化 -->

- SpringBoot 模块：当前 0% println，纯注解/配置代码
- concurrent-programming：10 个文件，println 均 ≤ 20%
- design-patterns：大部分 ≤ 20%（除 SpringPatternsDemo 已在阶段 1 完成）
- java-basics：大部分 ≤ 25%（除 DataTypesDemo 已在阶段 1 完成）
- AI 模块：4 个文件 println 均 ≤ 11%

## 五、自检清单

### Java 文件自检

- [ ] 编译检查：getDiagnostics 无错误
- [ ] Controller 有真实业务逻辑，不是空方法或只有注释
- [ ] @Autowired 注入的 Bean 有实际调用，不是摆设
- [ ] REST 接口有明确的请求/响应示例（Javadoc 中标注 curl 命令）
- [ ] 中间件连接配置与统一规范一致（地址/端口/用户名/密码）
- [ ] 有 prepareTestData 或 init 接口，方便初始化测试数据
- [ ] 有 cleanup 逻辑（可注释掉保留数据）
- [ ] 中文注释清晰，解释每个接口的用途和对应的 Spring 注解

### 配置文件自检

- [ ] application.yml 中每个中间件配置都有注释说明
- [ ] Profile 切换配置文件（application-xxx.yml）内容完整可用
- [ ] Docker 启动命令在 yml 注释中标注

### md 文档自检

- [ ] 代码示例链接指向正确的文件路径
- [ ] 有"在 Spring Cloud 项目中体验"章节
- [ ] curl 命令可直接复制执行
- [ ] Mermaid 图与代码实现一致

### 整体自检命令

```bash
# 1. 编译检查
mvn compile -pl 02-framework/springcloud-examples -q

# 2. 检查所有 Controller 是否有真实注入（不是空壳）
grep -rn "@Autowired\|@Resource" springcloud-examples/src/ | wc -l

# 3. 检查所有 REST 接口是否有方法体（不是空方法）
grep -A5 "@GetMapping\|@PostMapping\|@DeleteMapping" springcloud-examples/src/ | grep -c "return\|void"

# 4. 检查配置文件中间件地址是否统一
grep -rn "localhost" springcloud-examples/src/main/resources/

# 5. 启动测试（需要 Docker 中间件运行）
mvn spring-boot:run -pl 02-framework/springcloud-examples
```

## 六、进度统计

| 阶段 | 内容 | 状态 |
|------|------|------|
| 0 | 项目骨架与基础设施 | ✅ 已完成 |
| 1 | Java 核心（docs/1-java-core/ → 01-java-core/） | ✅ 已完成 |
| 2 | 框架层（docs/2-framework/ → 02-framework/）含 Spring Cloud 实战项目 4 模块 + 18 篇文档更新 | ✅ 已完成 |
| 3 | 数据存储（docs/3-data-store/ → 03-data-store/） | ✅ 已完成 |
| 4 | 中间件（docs/4-middleware/ → 04-middleware/） | ✅ 已完成 |
| 5 | 分布式（docs/5-distributed/ → 05-distributed/） | ✅ 已完成 |
| 6 | DevOps（docs/6-devops/ → 06-devops/） | ✅ 已完成 |
| 7 | AI 应用（docs/7-ai/ → 07-ai/） | ✅ 已完成 |
| 8 | 架构设计场景（docs/8-architecture/） | ✅ 已完成 |
| 9 | 学习路径与面试汇总 | ✅ 已完成 |
| 10 | 导航系统与标签体系 | ✅ 已完成 |
| 11 | 最终检查点 | ✅ 已完成 |

### Spring Cloud 实战项目明细

| 模块 | 任务数 | 已完成 | 待处理 |
|------|--------|--------|--------|
| A. springcloud-examples | 26 | 25 | 1（2.3.A.17 可选） |
| B. springcloud-gateway | 7 | 7 | 0 |
| C. springcloud-sharding | 6 | 6 | 0 |
| D. springcloud-user-service | 5 | 5 | 0 |
| md 文档同步更新 | 18 | 18 | 0 |
| **实战项目合计** | **62** | **61** | **1（可选）** |

---

> 说明：
> - 标记 `*` 的任务为可选任务
> - 阶段编号 = 项目分组编号，子任务编号 = docs 子目录编号，方便快速定位文件
> - 所有文档使用中文编写
> - 每个模块只出现一次，包含该模块的所有工作（文档 + Demo + Part B + Controller）
> - 原 tasks.md 和 code-quality-improvement.md 保留作为历史参考

> - 个人补充：
20 个 TODO-PENDING 标签已全部添加，覆盖了所有待处理项。搜索 TODO-PENDING 即可找到所有未完成/可选/后期扩展的内容，分类如下：

TODO-PENDING: 可选任务 — 7 个（MQTT Controller、单元测试、HTTP Demo、JMM/Collection/Serialization Demo）
TODO-PENDING: 暂不改动 — 4 个（TCP/WebSocket/RPC Demo + 暂不改动模块汇总）
TODO-PENDING: 后期扩展 — 3 个（Spring Security、Seata、扩展预留标题）
TODO-PENDING: 未开发，保持现状 — 1 个（不加入 SC 项目的模块）
TODO-PENDING: 可选优化 — 3 个（网络编程模块、Java 核心模块标题）
TODO-PENDING: 以下全部为待处理的可选任务 — 1 个（可选/后续任务章节标题）
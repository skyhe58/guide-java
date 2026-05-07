# 实施计划：Java 个人知识库

> ⚠️ **历史文件，仅供参考**。本文件是项目早期的任务清单，已被 `project-plan.md` 替代。后续所有开发请以 `project-plan.md` 为准。

## 概述

按照循序渐进的原则，先搭建项目骨架（仓库结构、Maven 父 POM、VitePress 配置、CI/CD），再按五层学习架构逐步填充各模块内容。每个模块包含 Markdown 知识文档和对应的 Maven 子模块代码示例。

**核心规范**：
- 每个知识点必须有对应的可运行代码示例（链接到 code-examples 子模块）
- 涉及复杂流程的知识点必须包含 Mermaid 流程图或时序图

**文档与代码对应规则**：
- 文档位置：`docs/{分组名}/{模块名}/{序号-知识点}.md`（如 `docs/1-java-core/1.1-java-basics/01-data-types.md`）
- 代码位置：`code-examples/{分组名}/{子模块名}/`
- 任务中的文档文件名和代码子模块名保持一致的命名风格，方便互相查找

**编号说明**：
- 分组编号（1-java-core、2-framework、3-data-store...）：按技术类型归类，方便文件浏览器中查找
- 任务编号（3.1、4.1、6.1、7.1...）：按学习顺序排列，循序渐进
- 两套编号维度不同，不会一一对应。例如任务 7.1 Spring Cloud 在 `2-framework` 分组（它是框架），但学习顺序排在任务 6.3 数据库（`3-data-store` 分组）之后

## 任务列表

- [x] 1. 搭建项目骨架与基础设施
  - [x] 1.1 创建项目根目录结构和根 README.md
    - 创建顶层目录：`docs/`、`code-examples/`、`.github/`、`docker/`
    - 编写 `README.md`（项目简介、快速开始、目录结构、完成度追踪表）
    - 创建 `docs/templates/entry-template.md`（知识条目模板）
    - _需求: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [x] 1.2 创建 Maven 父 POM 和所有子模块骨架
    - `code-examples/pom.xml` — 根 POM（Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.1）
    - 7 个分组模块：01-java-core、02-framework、03-data-store、04-middleware、05-distributed、06-devops、07-ai
    - 分组下共 22 个子模块（含新增的 mongodb-examples、minio-examples、mq-mqtt-examples、cicd-examples、monitoring-examples）
    - 确保 `mvn compile` 编译通过
    - _需求: 3.1, 3.2, 3.3_

  - [x] 1.3 初始化 VitePress 站点配置
    - `docs/.vitepress/config.mts` — 主配置（中文、本地搜索、Mermaid、代码行号）
    - `docs/.vitepress/sidebar.mts` — 侧边栏配置
    - `docs/index.md` — 首页
    - `docs/guide/getting-started.md` — 快速开始
    - `docs/guide/how-to-use.md` — 使用指南
    - `docs/package.json` — pnpm + VitePress 依赖
    - _需求: 11.1, 11.2, 11.4, 9.1, 9.2_

  - [x] 1.4 配置 GitHub Actions CI/CD
    - `.github/workflows/deploy.yml` — 自动构建 + 部署 + Maven 编译验证 + 断链检查
    - _需求: 11.3, 11.5_

  - [x] 1.5 创建 Docker Compose 配置文件
    - `docker/docker-compose.yml` — Redis、MySQL
    - `docker/docker-compose.mq.yml` — RabbitMQ、Kafka
    - `docker/docker-compose.es.yml` — Elasticsearch
    - `docker/docker-compose.consul.yml` — Consul
    - `docker/docker-compose.apollo.yml` — Apollo
    - `docker/docker-compose.nginx.yml` — Nginx
    - _需求: 3.6_

- [x] 2. 检查点 - 项目骨架验证
  - `mvn compile` 通过、`pnpm run build` 通过、Docker Compose 语法正确

- [x] 3. 第一层：Java 语言基础模块
  - [x] 3.1 创建 Java 基础知识文档（docs/1-java-core/1.1-java-basics/ → code-examples/01-java-core/java-basics/）

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

    - _需求: 1.1, 2.1, 2.3, 2.5, 4.1, 4.2, 5.1_

  - [x] 3.2 创建 Java 基础代码示例（code-examples/01-java-core/java-basics/）

    | 代码目录 | 示例内容 | 对应文档 |
    |---------|---------|---------|
    | datatypes/ | 装箱拆箱、缓存池、值传递 | `01-data-types.md`, `02-value-passing.md` |
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
    - _需求: 3.3, 3.4, 3.5, 5.1_

  - [ ]* 3.3 编写 Java 基础模块单元测试
    - 为 collections、generics、stream 等核心示例编写 JUnit 5 + AssertJ 测试
    - _需求: 3.4_

- [x] 4. 第二层：Java 语言深入模块
  - [x] 4.1 创建并发编程知识文档和代码示例（docs/1-java-core/1.3-concurrent/ → code-examples/01-java-core/concurrent-programming/）

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

    - _需求: 1.1, 2.1, 5.3_

  - [x] 4.2 创建 JVM 知识文档和代码示例（docs/1-java-core/1.4-jvm/ → code-examples/01-java-core/jvm-deep-dive/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `01-memory-model.md` | 内存模型与内存区域 |
    | `02-gc.md` | GC 算法/CMS/G1/ZGC |
    | `03-classloading.md` | 类加载过程 |
    | `04-jit.md` | JIT 编译/逃逸分析 |
    | `05-tuning.md` | JVM 调优参数/GC 日志 |
    | `06-diagnostic.md` | 内存泄漏/jstack/arthas |
    | `99-interview.md` | JVM 面试指南 |

    - _需求: 1.1, 2.1, 5.4_

  - [x] 4.3 创建 Java 进阶知识文档和代码示例（docs/1-java-core/1.2-java-advanced/ → code-examples/01-java-core/java-advanced/）

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

    - _需求: 1.1, 2.1, 5.2_

  - [x] 4.4 创建设计模式知识文档和代码示例（docs/1-java-core/1.5-design-patterns/ → code-examples/01-java-core/design-patterns/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `01-creational.md` | 单例/工厂/建造者/原型 |
    | `02-structural.md` | 代理/适配器/装饰器/门面 |
    | `03-behavioral.md` | 策略/模板方法/观察者/责任链 |
    | `04-spring-patterns.md` | Spring 中的设计模式 |
    | `05-principles.md` | SOLID/DRY/KISS |
    | `99-interview.md` | 设计模式面试指南 |

    - _需求: 1.1, 2.1, 5.5_

  - [x] 4.5 创建数据结构与算法知识文档和代码示例（docs/1-java-core/1.6-algorithm/ → code-examples/01-java-core/java-basics/ 的 algorithm 目录）

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
    - _需求: 1.1, 2.1, 17.1, 17.2, 17.3, 17.4, 17.5_

- [x] 5. 检查点 - 第一层和第二层验证
  - 文档格式、frontmatter 完整性、代码编译、站点构建无断链

- [x] 6. 第三层：框架应用模块
  - [x] 6.1 创建网络与协议知识文档和代码示例（docs/2-framework/2.1-network/ → code-examples/02-framework/network-programming/）

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

    - _需求: 1.1, 2.1, 5.6_

  - [x] 6.2 创建 Spring Boot 知识文档和代码示例（docs/2-framework/2.2-springboot/ → code-examples/02-framework/springboot-examples/）

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

    - _需求: 1.1, 2.1, 6.1, 6.3_

  - [x] 6.3 创建数据库/MySQL 知识文档和代码示例（docs/3-data-store/3.1-database/ → code-examples/03-data-store/database-examples/）

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

    - _需求: 1.1, 2.1, 7.4_

- [x] 7. 第四层：分布式体系模块
  - [x] 7.1 创建 Spring Cloud 知识文档和代码示例（docs/2-framework/2.3-springcloud/ → code-examples/02-framework/springcloud-examples/）

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

    - _需求: 1.1, 2.1, 6.2, 6.4_

  - [x] 7.2 创建 Redis 知识文档和代码示例（docs/3-data-store/3.2-redis/ → code-examples/03-data-store/redis-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `data-structures.md` | 数据结构与底层实现 |
    | `persistence.md` | RDB/AOF 持久化 |
    | `replication.md` | 主从/哨兵/Cluster |
    | `cache-problems.md` | 穿透/击穿/雪崩 |
    | `distributed-lock.md` | 分布式锁实现 |
    | `spring-integration.md` | Spring Boot 集成 |
    | `interview.md` | Redis 面试指南 |

    - _需求: 1.1, 2.1, 7.1_

  - [x] 7.3 创建 RabbitMQ 知识文档和代码示例（docs/4-middleware/4.1-mq-rabbitmq/ → code-examples/04-middleware/mq-rabbitmq-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `rabbitmq.md` | RabbitMQ 核心概念 |
    | `rabbitmq-reliability.md` | RabbitMQ 消息可靠性/幂等性 |
    | `rabbitmq-advanced.md` | 死信队列/延迟消息/优先级队列 |
    | `rabbitmq-spring.md` | Spring Boot 集成 RabbitMQ |
    | `interview.md` | RabbitMQ 面试指南 |

    - _需求: 1.1, 2.1, 7.2_

  - [x] 7.4 创建 Kafka 知识文档和代码示例（docs/4-middleware/4.2-mq-kafka/ → code-examples/04-middleware/mq-kafka-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `kafka.md` | Kafka 架构与原理 |
    | `kafka-reliability.md` | Kafka 消息可靠性/顺序性 |
    | `kafka-advanced.md` | 分区策略/消费者组/Rebalance |
    | `kafka-spring.md` | Spring Boot 集成 Kafka |
    | `interview.md` | Kafka 面试指南 |

    - _需求: 1.1, 2.1, 7.2_

  - [x] 7.5 创建注册中心知识文档和代码示例（docs/4-middleware/4.5-registry/ → code-examples/04-middleware/registry-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `principles.md` | 服务注册与发现原理 |
    | `consul.md` | Consul 架构/健康检查/ACL |
    | `zookeeper.md` | ZAB/临时节点/Watcher |
    | `nacos.md` | Nacos AP/CP 切换 |
    | `comparison.md` | 注册中心选型对比表 |
    | `interview.md` | 注册中心面试指南 |

    - Consul 优先，代码示例在 04-middleware/registry-examples 子模块中
    - _需求: 1.1, 2.1, 14.2, 14.4, 14.5_

  - [x] 7.6 创建配置中心知识文档和代码示例（docs/4-middleware/4.4-config-center/ → code-examples/04-middleware/config-center-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `apollo.md` | Apollo 架构/热更新/灰度 |
    | `nacos-config.md` | Nacos Config 使用 |
    | `comparison.md` | Apollo vs Nacos vs Spring Cloud Config |
    | `security.md` | 配置加密与安全 |
    | `interview.md` | 配置中心面试指南 |

    - _需求: 1.1, 2.1, 14.1, 14.3_

- [x] 8. 检查点 - 第三层和第四层验证
  - 文档格式、代码编译、侧边栏更新、站点构建无断链

- [x] 9. 第五层：综合进阶模块
  - [x] 9.1 创建 Elasticsearch 知识文档和代码示例（docs/3-data-store/3.3-elasticsearch/ → code-examples/03-data-store/elasticsearch-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `inverted-index.md` | 倒排索引原理 |
    | `mapping.md` | 映射与分析器 |
    | `crud.md` | CRUD 操作 |
    | `dsl-query.md` | DSL 复合查询 |
    | `aggregation.md` | 聚合分析 |
    | `spring-data.md` | Spring Data ES 集成 |
    | `interview.md` | ES 面试指南 |

    - _需求: 1.1, 2.1, 7.3_

  - [x] 9.2 创建 Nginx 知识文档和配置示例（docs/4-middleware/4.6-nginx/ → code-examples/04-middleware/nginx-examples/）

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

    - 代码示例为 Nginx 配置文件，放在 04-middleware/nginx-examples/conf/ 下
    - _需求: 1.1, 2.1, 13.1, 13.2, 13.3_

  - [x] 9.3 创建分布式系统理论知识文档和代码示例（docs/5-distributed/5.1-distributed/ → code-examples/05-distributed/distributed-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `cap-base.md` | CAP/BASE 理论 |
    | `consensus.md` | Raft/Paxos 算法 |
    | `distributed-lock.md` | Redis/ZK/MySQL 分布式锁 |
    | `distributed-transaction.md` | 2PC/TCC/Saga/消息一致性 |
    | `idempotent.md` | 幂等性设计 |
    | `rate-limiting.md` | 令牌桶/漏桶/滑动窗口 |
    | `interview.md` | 分布式面试指南 |

    - _需求: 1.1, 2.1, 15.1, 15.3_

  - [x] 9.4 创建 Linux 运维基础知识文档（docs/6-devops/6.4-linux/，无独立代码子模块）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `commands.md` | 常用命令速查 |
    | `shell.md` | Shell 脚本基础 |
    | `performance.md` | top/vmstat/iostat/netstat |
    | `log-analysis.md` | grep/awk/sed |
    | `jvm-troubleshooting.md` | CPU 飙高/OOM/死锁排查 |
    | `interview.md` | Linux 面试指南 |

    - _需求: 1.1, 2.1, 15.2_

  - [x] 9.5 创建 Docker 与 K8s 知识文档和示例（docs/6-devops/6.1-docker-k8s/ → code-examples/06-devops/docker-k8s-examples/）

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

    - _需求: 1.1, 2.1, 18.1, 18.2, 18.3, 18.4, 18.5_

  - [x] 9.6 创建 AI 应用知识文档和代码示例（docs/7-ai/7.1-ai/ → code-examples/07-ai/ai-examples/）

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

    - _需求: 1.1, 2.1, 8.1, 8.2, 8.3_

  - [x] 9.7 创建 MongoDB 知识文档和代码示例（docs/3-data-store/3.4-mongodb/ → code-examples/03-data-store/mongodb-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `document-model.md` | 文档模型/BSON/Schema 设计 |
    | `crud.md` | CRUD 操作/查询优化 |
    | `aggregation.md` | 聚合管道/MapReduce |
    | `index.md` | 索引类型/复合索引/TTL 索引 |
    | `spring-data.md` | Spring Data MongoDB 集成 |
    | `interview.md` | MongoDB 面试指南 |

    - _需求: 1.1, 2.1_

  - [x] 9.8 创建 MinIO 知识文档和代码示例（docs/3-data-store/3.5-minio/ → code-examples/03-data-store/minio-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `architecture.md` | MinIO 架构/对象存储原理 |
    | `bucket-management.md` | 桶管理/权限策略 |
    | `file-operations.md` | 文件上传下载/分片上传 |
    | `interview.md` | 对象存储面试指南 |

    - _需求: 1.1, 2.1_

  - [x] 9.9 创建 MQTT 知识文档和代码示例（docs/4-middleware/4.3-mq-mqtt/ → code-examples/04-middleware/mq-mqtt-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `mqtt-protocol.md` | MQTT 协议原理/QoS 级别 |
    | `mqtt-broker.md` | EMQX/Mosquitto Broker 部署 |
    | `mqtt-spring.md` | Spring Boot 集成 MQTT |
    | `interview.md` | MQTT/IoT 面试指南 |

    - _需求: 1.1, 2.1_

  - [x] 9.10 创建 CI/CD 知识文档和示例（docs/6-devops/6.2-cicd/ → code-examples/06-devops/cicd-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `jenkins.md` | Jenkins Pipeline/Jenkinsfile |
    | `github-actions.md` | GitHub Actions 工作流 |
    | `gitlab-ci.md` | GitLab CI/CD 配置 |
    | `best-practices.md` | CI/CD 最佳实践/流水线设计 |
    | `interview.md` | CI/CD 面试指南 |

    - _需求: 1.1, 2.1_

  - [x] 9.11 创建监控体系知识文档和示例（docs/6-devops/6.3-monitoring/ → code-examples/06-devops/monitoring-examples/）

    | 文档文件 | 知识点 |
    |---------|--------|
    | `prometheus.md` | Prometheus 指标采集/PromQL |
    | `grafana.md` | Grafana 仪表盘/告警配置 |
    | `micrometer.md` | Micrometer + Spring Boot Actuator |
    | `log-monitoring.md` | ELK/Loki 日志监控方案 |
    | `interview.md` | 监控体系面试指南 |

    - _需求: 1.1, 2.1_

  - [x] 9.12 创建架构设计场景知识文档和代码示例（docs/8-architecture/ → 跨多个 code-examples 子模块）

    | 文档文件 | 知识点 | 关联代码子模块 |
    |---------|--------|--------------|
    | `seckill.md` | 秒杀系统设计 | 03-data-store/redis-examples, 04-middleware/mq-rabbitmq-examples |
    | `short-url.md` | 短链接系统 | 03-data-store/database-examples |
    | `order-timeout.md` | 订单超时取消 | 04-middleware/mq-rabbitmq-examples, 03-data-store/redis-examples |
    | `cache-strategy.md` | 分布式缓存方案 | 03-data-store/redis-examples |
    | `idempotent-design.md` | 接口幂等性方案 | 05-distributed/distributed-examples |
    | `distributed-session.md` | 分布式 Session | 03-data-store/redis-examples |
    | `file-upload.md` | 大文件上传 | 02-framework/springboot-examples |
    | `cache-db-consistency.md` | 缓存与 DB 双写一致性 | 03-data-store/redis-examples |
    | `interview.md` | 架构设计面试指南 | — |

    - _需求: 1.1, 2.1, 16.1, 16.2, 16.3_

- [x] 10. 检查点 - 第五层验证
  - 文档格式、代码编译、站点构建无断链

- [x] 11. 学习路径与面试汇总
  - [x] 11.1 创建学习路径文档
    - `docs/learning-paths/beginner.md` — Java 初学者路径
    - `docs/learning-paths/intermediate.md` — Java 中级进阶路径
    - `docs/learning-paths/advanced.md` — Java 高级深入路径
    - `docs/learning-paths/interview-sprint.md` — 面试突击路径
    - `docs/learning-paths/architect.md` — 架构师成长路径
    - _需求: 10.1, 10.2, 10.3, 10.4_

  - [x] 11.2 创建面试汇总文档
    - `docs/interview/by-company.md` — 按公司类型分类面试重点
    - `docs/interview/knowledge-map.md` — 面试知识图谱
    - _需求: 4.4, 4.5_

- [x] 12. 导航系统与标签体系完善
  - [x] 12.1 完善 VitePress 侧边栏和导航配置
    - 更新 `docs/.vitepress/sidebar.mts` 和 `docs/.vitepress/config.mts`
    - _需求: 9.1, 9.3, 9.4, 9.5_

  - [x] 12.2 校验所有文档的 frontmatter 和标签
    - 检查 frontmatter 必填字段、标签合法性、代码示例链接有效性
    - _需求: 2.2, 9.3_

- [x] 13. 最终检查点 - 全面验证
  - `mvn compile` 通过、VitePress 构建成功、文档格式统一、链接有效、面试指南完整

## 说明

- 标记 `*` 的任务为可选任务
- 每个任务标题括号中标注了 `文档目录 → 代码子模块` 的对应关系
- 表格列出文档文件名和知识点，开发后根据文件名即可在对应子模块中找到代码
- 所有文档使用中文编写

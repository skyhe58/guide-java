# 代码质量改进计划

> ⚠️ **历史文件，仅供参考**。本文件是代码质量改进的执行记录（批次 1~10 + Part B 补充），已合并到 `project-plan.md` 中。后续所有开发请以 `project-plan.md` 为准。

## 问题

大量 Java 代码文件用 System.out.println() 代替真正的代码逻辑，排除注释后 println 占比超过 30%，没有实际 demo 价值。

## 改进标准（重点）

1. 每个 Java 文件必须有真实可运行的代码案例，像教科书一样
2. println 只用于输出运行结果，不用于讲解知识点。描述和讲解内容用注释写，注释内容清晰有条理
3. 改进后 println 占代码行数（排除注释）不超过 20%，正常输出结果的 println 是需要的
4. demo 要丰富、案例详细，有利于高效学习和面试
5. 对应 md 文档也一起丰富优化

## Demo 模式（混合方案）

每个文件根据内容特点，采用以下一种或两种模式混合：

### 模式一：纯 Java 内存模拟（适合原理/算法类）
- 直接运行，不依赖外部环境
- 用 Java 数据结构模拟中间件内部行为
- 适合：索引原理、事务隔离、锁机制、分片算法、雪花 ID、TCC 状态机、网关过滤器链、JVM 内存/GC/类加载等

### 模式二：连接真实中间件（适合 API/操作类）
- 需要 Docker 启动中间件，体验真实 API 和响应
- 文件头 Javadoc 标注 Docker 启动命令
- 需要在 pom.xml 中添加对应客户端依赖
- 适合：ES CRUD/聚合、Redis 数据结构操作、RabbitMQ/Kafka 消息收发、MongoDB CRUD、MinIO 文件操作、Consul/ZK/Nacos 注册发现等

### 模式一 + 模式二混合（推荐）
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

## Docker 对照表

| Compose 文件 | 服务 | 代码模块 | 需添加的客户端依赖 |
|-------------|------|---------|------------------|
| `docker/docker-compose.yml` | redis | redis-examples | `jedis` 或 `lettuce-core` |
| `docker/docker-compose.yml` | mysql | database-examples | `mysql-connector-j` |
| `docker/docker-compose.mq.yml` | rabbitmq | mq-rabbitmq-examples | `amqp-client` |
| `docker/docker-compose.mq.yml` | kafka | mq-kafka-examples | `kafka-clients` |
| `docker/docker-compose.es.yml` | elasticsearch | elasticsearch-examples | `elasticsearch-rest-client` |
| `docker/docker-compose.consul.yml` | consul | registry-examples | `consul-api` |
| `docker/docker-compose.apollo.yml` | apollo | config-center-examples | （模拟为主） |
| `docker/docker-compose.nginx.yml` | nginx | nginx-examples | （配置文件为主） |

## 执行方式

删除旧文件内容，全部重写。文件名、包名不变，只替换内容。
需要添加客户端依赖的模块，同步更新 pom.xml。

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

Part B 入口代码规范（后续新文件统一用注释方式，不加 else 提示）：
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

### 批次 1 待补充项

以下文件 Part A 已完成，需要补充 Mermaid 图到对应 md 文档，以及部分文件可选补充 Part B：

- [ ] 1.1 IndexDemo → md 已有 Mermaid 图 ✅
- [ ] 1.2 TransactionDemo → md 已有 Mermaid 图 ✅
- [ ] 1.3 LockDemo → md 已有 Mermaid 图 ✅；已修复 TreeSet 编译错误 ✅
- [x] 1.5 BinlogDemo → md 已有 Mermaid 图 ✅；Part B 已补充（mysql-binlog-connector 真实监听）✅
- [ ] 1.7 ShardingDemo → md 已有 Mermaid 图 ✅

---

## 任务清单

### 批次 1：数据库（8 个文件）✅ 已完成

路径前缀：`code-examples/03-data-store/database-examples/src/main/java/com/example/database/`
模式：纯模拟（模式一）— 索引/事务/锁/分片都是原理层面，模拟更直观
混合模式评估：已检查，全部为原理/算法类内容，纯模拟是最佳选择，无需改写。
可选后续补充：OptimizationDemo 可补充真实 EXPLAIN 示例，ConnectionPoolDemo 可补充 JDBC 连接池对比（优先级低）

- [x] 1.1 `index_demo/IndexDemo.java`（82% → 22%）✅ B+树索引查找、覆盖索引、最左前缀匹配
- [x] 1.2 `transaction/TransactionDemo.java`（81% → 20%）✅ MVCC 版本链+四种隔离级别并发行为
- [x] 1.3 `lock/LockDemo.java`（81% → 33%）✅ 行锁/间隙锁/临键锁/死锁检测
- [x] 1.4 `optimization/OptimizationDemo.java`（82% → 29%）✅ 慢查询分析+EXPLAIN+成本估算
- [x] 1.5 `binlog/BinlogDemo.java`（82% → 17%）✅ Binlog 格式+主从复制+PITR+Canal 同步
- [x] 1.6 `pool/ConnectionPoolDemo.java`（76% → 21%）✅ Semaphore+BlockingQueue 连接池
- [x] 1.7 `sharding/ShardingDemo.java`（74% → 21%）✅ 取模/范围/一致性哈希三种分片对比
- [x] 1.8 `id/DistributedIdDemo.java`（65% → 26%）✅ UUID/自增/雪花/号段四种方案对比

### 批次 2：Elasticsearch（5 个文件）

路径前缀：`code-examples/03-data-store/elasticsearch-examples/src/main/java/com/example/es/`
模式：混合 — Part A 模拟倒排索引/评分原理 + Part B 用 ES RestClient 真实操作
依赖：pom.xml 添加 `elasticsearch-rest-client` + `elasticsearch-java`

- [x] 2.1 `aggregation/AggregationDemo.java`（86% → 27%）✅ Part A 完成（Stream 模拟聚合），需补充 Part B（ES RestClient 真实聚合）
- [x] 2.2 `query/QueryDemo.java`（86% → 35%）✅ Part A 完成（Predicate 模拟查询），需补充 Part B（ES RestClient 真实查询 DSL）
- [x] 2.3 `crud/CrudDemo.java`（80%）✅ Part A: 版本控制+乐观锁模拟 + Part B: ES RestClient 真实 CRUD+Bulk
- [x] 2.4 `index_demo/IndexDemo.java`（78%）✅ Part A: 倒排索引+分词+TF-IDF + Part B: ES 索引管理+Analyze API
- [x] 2.5 `spring/SpringDataDemo.java`（63%）✅ Part A: 模拟 Repository+分页 + Part B: ES Client 模拟 Repository 查询

### 批次 3：消息队列 RabbitMQ（4 个文件）

路径前缀：`code-examples/04-middleware/mq-rabbitmq-examples/src/main/java/com/example/mq/rabbitmq/`
模式：混合 — Part A 模拟路由/确认原理 + Part B 用 amqp-client 真实收发
依赖：pom.xml 添加 `amqp-client`

- [x] 3.1 `core/RabbitMQCoreDemo.java`（83% → 16%）✅ Part A 完成（Exchange 路由模拟），补充 Part B（amqp-client 真实收发）
- [x] 3.2 `reliability/ReliabilityDemo.java`（81%）✅ Part A: Confirm/Return/ACK/幂等/重试模拟 + Part B: amqp-client 真实 Confirm+ACK
- [x] 3.3 `advanced/AdvancedDemo.java`（82%）✅ Part A: DelayQueue 延迟消息+死信+优先级 + Part B: 真实 TTL+DLX+优先级队列
- [x] 3.4 `spring/SpringIntegrationDemo.java`（79%）✅ Part A: 模拟 Template/Converter/Container + Part B: amqp-client JSON 收发

### 批次 4：消息队列 Kafka + MQTT（5 个文件）

Kafka 路径前缀：`code-examples/04-middleware/mq-kafka-examples/src/main/java/com/example/mq/kafka/`
MQTT 路径前缀：`code-examples/04-middleware/mq-mqtt-examples/src/main/java/com/example/mqtt/`
模式：混合 — Part A 模拟分区/消费组原理 + Part B 用 kafka-clients 真实生产消费
依赖：Kafka pom.xml 添加 `kafka-clients`；MQTT pom.xml 添加 `org.eclipse.paho.client.mqttv3`

- [x] 4.1 `core/KafkaCoreDemo.java`（85%）✅ Part A: 模拟 Topic/Partition/Offset/ConsumerGroup + Part B: kafka-clients 真实生产消费
- [x] 4.2 `reliability/KafkaReliabilityDemo.java`（83%）✅ Part A: 模拟 acks/ISR/幂等 + Part B: 不同 acks 耗时对比
- [x] 4.3 `advanced/KafkaAdvancedDemo.java`（83%）✅ Part A: 模拟 Range/RoundRobin/Sticky 分配+Rebalance+Exactly-Once
- [x] 4.4 `spring/KafkaSpringDemo.java`（77%）✅ Part A: 模拟 KafkaTemplate/@KafkaListener + 配置最佳实践
- [x] 4.5 `MQTTDemo.java`（80%）✅ Part A: 模拟发布订阅/QoS/遗嘱/保留消息 + Part B: Paho MQTT 真实收发

### 批次 5：Redis + MongoDB + MinIO（3 个文件）

模式：混合 — Part A 模拟底层数据结构 + Part B 用客户端真实操作
依赖：Redis 添加 `jedis`；MongoDB 添加 `mongodb-driver-sync`；MinIO 添加 `minio`

- [x] 5.1 `redis-examples/.../DataStructureDemo.java`（80% → 27%）✅ Part A: 模拟五种结构+编码切换 + Part B: Jedis 真实操作
- [x] 5.2 `mongodb-examples/.../MongoDBDemo.java`（85% → 17%）✅ Part A: 模拟文档CRUD+聚合 + Part B: MongoClient 真实操作
- [x] 5.3 `minio-examples/.../MinIODemo.java`（81% → 25%）✅ Part A: 模拟桶管理+分片上传 + Part B: MinioClient 真实操作

### 批次 6：Spring Cloud（4 个文件）

路径前缀：`code-examples/02-framework/springcloud-examples/src/main/java/com/example/springcloud/`
模式：纯模拟（模式一）— 架构模式/设计模式层面，模拟更清晰

- [x] 6.1 `transaction/TransactionDemo.java`（83% → 23%）✅ 状态机模拟 TCC（Try/Confirm/Cancel）+ 空回滚/悬挂/幂等
- [x] 6.2 `gateway/GatewayDemo.java`（81% → 18%）✅ Map+责任链模拟路由匹配+过滤器链（限流/鉴权/日志/路径重写）
- [x] 6.3 `registry/RegistryDemo.java`（79% → 20%）✅ ConcurrentHashMap 模拟注册表+心跳续约+服务剔除
- [x] 6.4 `feign/FeignDemo.java`（76% → 26%）✅ JDK 动态代理+负载均衡（轮询/随机/加权）+Fallback 降级

### 批次 7：注册中心 + 配置中心（7 个文件）

1. 一个项目启动就能体验 Spring Cloud 全家桶（所有中间件集成）
2. 每个模块有独立的 Controller，通过 REST 接口验证
3. 原有的 Part A（原理模拟 Demo）保留不动，也可以单独 main 方法运行
4. Gateway 独立模块（WebFlux 和 WebMVC 不能共存）
5. 中间件连接配置统一在 application.yml 中
6. 通过 Profile 切换不同中间件实现（如 Consul → Nacos）


### 批次 7：注册中心 + 配置中心（7 个文件）

注册中心路径前缀：`code-examples/04-middleware/registry-examples/src/main/java/com/example/middleware/registry/`
配置中心路径前缀：`code-examples/04-middleware/config-center-examples/src/main/java/com/example/middleware/config/`
模式：混合 — Part A 模拟注册/发现/配置原理 + Part B 用官方 SDK 真实操作
依赖：Consul 添加 `consul-api`；ZK 添加 `curator-framework`

- [x] 7.1 `consul/ConsulDemo.java`（68% → 22%）✅ Part A: 模拟注册/发现/KV + Part B: consul-api 真实操作
- [x] 7.2 `nacos/NacosDemo.java`（65% → 19%）✅ 纯模拟 — AP/CP 模式+分组+命名空间+权重路由
- [x] 7.3 `zookeeper/ZookeeperDemo.java`（56% → 22%）✅ Part A: 模拟 ZNode 树 + Part B: Curator 真实操作
- [x] 7.4 `nacos/NacosConfigDemo.java`（60% → 23%）✅ 纯模拟 — 长轮询+配置监听+动态刷新
- [x] 7.5 `apollo/ApolloDemo.java`（60% → 27%）✅ 纯模拟 — 命名空间+多环境+热更新+灰度发布
- [x] 7.6 `security/ConfigSecurityDemo.java`（59% → 27%）✅ AES/RSA 加密+Jasypt+密钥轮转
- [x] 7.7 `springcloud-examples/.../TracingDemo.java`（51% → 20%）✅ TraceId/SpanId+MDC 日志+采样策略

### 批次 8：分布式 + DevOps（4 个文件）✅ 已完成

- [x] 8.1 `distributed-examples/.../DistributedTransactionDemo.java`（68% → 23%）✅ 纯模拟 — 2PC/TCC/Saga 状态机+对比
- [x] 8.2 `distributed-examples/.../DistributedLockCompare.java`（66% → 26%）✅ Part A: 模拟三种锁 + Part B: Jedis 真实 Redis 锁
- [x] 8.3 `monitoring-examples/.../MonitoringDemo.java`（81% → 31%）✅ Counter/Gauge/Timer + Prometheus 格式
- [x] 8.4 `cicd-examples/.../CICDDemo.java`（79% → 30%）✅ 流水线模拟+并行步骤+GitHub Actions 配置

### 批次 9：JVM + Java 进阶 + 其他（11 个文件）✅ 已完成

模式：纯模拟（模式一）— JVM 和 Java 语言特性本身就是运行时行为

- [x] 9.1 `jvm-deep-dive/.../MemoryAreaDemo.java`（42%）✅ 堆/栈/方法区/直接内存+MXBean 监控+OOM 场景
- [x] 9.2 `java-advanced/.../SPIDemo.java`（41%）✅ 手写 ServiceLoader+SPI 原理+Java/Spring/Dubbo SPI 对比
- [x] 9.3 `java-advanced/.../ClassLoaderDemo.java`（39%）✅ 三层加载器+双亲委派+自定义 ClassLoader+打破委派
- [x] 9.4 `jvm-deep-dive/.../GCDemo.java`（35%）✅ 四种引用类型+GC 算法模拟+对象晋升+收集器对比
- [x] 9.5 `jvm-deep-dive/.../ClassLoadingDemo.java`（35%）✅ 类加载五阶段+主动/被动引用+clinit 线程安全
- [x] 9.6 `springcloud-examples/.../CircuitBreakerDemo.java`（33%）✅ 状态机熔断器三态转换+滑动窗口+Resilience4j 配置
- [x] 9.7 `jvm-deep-dive/.../TuningDemo.java`（52%）✅ MXBean 监控+内存分配策略+GC 日志解读+调优参数
- [x] 9.8 `jvm-deep-dive/.../DiagnosticDemo.java`（52%）✅ 内存泄漏+CPU 飙高+死锁模拟+排查工具
- [x] 9.9 `design-patterns/.../SpringPatternsDemo.java`（79%）✅ BeanFactory+AOP 代理+JdbcTemplate+事件机制
- [x] 9.10 `java-basics/.../DataTypesDemo.java`（30%）✅ 装箱拆箱陷阱+Integer 缓存池+浮点精度+性能对比
- [x] 9.11 `jvm-deep-dive/.../JITDemo.java`（30%）✅ 逃逸分析+标量替换+锁消除+锁粗化

---

## 已完成文件的 Part B 补充清单（优先处理）

以下文件 Part A（原理模拟）已完成，优先补充 Part B（真实中间件连接），因为这些文件之前已经写过，补充 Part B 后即可完结。

### 必须补充 Part B（优先级：P0，最先执行）

- [x] 2.1 `es/aggregation/AggregationDemo.java` → ✅ Part B 已补充：ES RestClient 真实 terms/stats/range/嵌套聚合
- [x] 2.2 `es/query/QueryDemo.java` → ✅ Part B 已补充：ES RestClient 真实 match/term/range/bool 查询

### 补充 Part B（优先级：P1，在 P0 之后执行）

- [x] 3.1 `mq/rabbitmq/core/RabbitMQCoreDemo.java` → ✅ Part B 已补充：amqp-client 真实 Direct/Topic/Fanout 收发
- [x] 1.4 `database/optimization/OptimizationDemo.java` → ✅ Part B 已补充：JDBC 连接真实 MySQL 执行 EXPLAIN
- [x] 1.6 `database/pool/ConnectionPoolDemo.java` → ✅ Part B 已补充：HikariCP 真实连接池配置+并发压测



### 10.1 概述

批次 10 是 Spring Cloud 实战项目，目标是搭建一个一键启动就能体验 Spring Cloud 全家桶的微服务项目。
包含 4 个独立模块，覆盖注册发现、Feign 调用、熔断降级、网关、消息队列、缓存、数据库、搜索、
文件存储、定时任务、链路追踪、分库分表、分布式限流、分布式会话、缓存一致性等核心功能。
每个模块通过 REST 接口验证，支持 Profile 切换不同中间件实现。
原批次 11（分库分表）和批次 12（分布式补充）已合并到本批次。

### 10.2 模块结构（4 个独立模块）

```
02-framework/
├── springcloud-examples/        ← 模块 A：业务服务（端口 8090，WebMVC）
├── springcloud-gateway/         ← 模块 B：API 网关（端口 8080，WebFlux）
├── springcloud-sharding/        ← 模块 C：分库分表（端口 8091，独立 DataSource）
└── springcloud-user-service/    ← 模块 D：用户服务（端口 8092，Feign 被调用方）
```

独立模块原因：
- Gateway：WebFlux 和 WebMVC 不能共存
- Sharding：ShardingSphere 接管 DataSource，影响其他模块普通 SQL
- User-Service：独立微服务，被 Feign 调用，演示真实跨服务调用链路
- 四个模块都注册到 Consul，Gateway 可路由到所有服务

后期扩展预留：
- Spring Security → springcloud-examples 加 Profile `security`
- Seata 分布式事务 → 新建 `springcloud-seata/` 模块
- 第二个微服务 → 新建 `springcloud-user-service/` 模块（见下方预留设计）

#### 模块 D：springcloud-user-service（用户服务，端口 8092，WebMVC，Feign 被调用方）

```
springcloud-user-service/
├── pom.xml                              ← spring-boot-starter-web + consul-discovery + jdbc + redis
├── src/main/java/com/example/user/
│   ├── UserServiceApp.java              ← @SpringBootApplication + @EnableDiscoveryClient
│   ├── controller/
│   │   └── UserController.java          ← REST 接口（被 Feign 调用的提供方）
│   │       // GET /users/{id} → 查询用户
│   │       // POST /users → 创建用户
│   │       // GET /users → 列出所有用户
│   │       // GET /users/search?name=xxx → 按名称搜索
│   ├── entity/
│   │   └── User.java                   ← 用户实体
│   └── service/
│       └── UserService.java            ← 业务逻辑（JdbcTemplate + Redis 缓存）
├── src/main/resources/
│   └── application.yml                  ← 端口 8092 + Consul 注册（spring.application.name=user-service）
```

完整微服务调用链路：
```
客户端 → Gateway(8080) → springcloud-demo(8090) → [Feign] → user-service(8092)
                                                  → [Feign] → sharding-service(8091)
```

Feign 调用说明：
- springcloud-examples 中的 UserFeignClient 声明 `@FeignClient(name = "user-service")`
- 通过 Consul 服务发现找到 user-service(8092) 的实例
- 演示完整链路：服务注册 → 服务发现 → 负载均衡 → Feign 调用 → Fallback 降级
- user-service 未启动时自动触发 FallbackFactory 返回降级数据

#### 模块 A：springcloud-examples（业务服务，端口 8090，WebMVC）

```
springcloud-examples/
├── pom.xml                              ← Spring Cloud 全家桶依赖
├── src/main/java/com/example/springcloud/
│   ├── SpringCloudApp.java              ← 启动类
│   │   // @SpringBootApplication
│   │   // @EnableFeignClients
│   │   // @EnableDiscoveryClient
│   │   // @EnableScheduling
│   │
│   │   ══════ 注册发现 ══════
│   ├── registry/
│   │   ├── RegistryDemo.java            ← Part A 原理模拟（已有，不动）
│   │   └── RegistryController.java      ← REST 接口
│   │       // GET /demo/registry/services → 列出所有注册的服务
│   │       // GET /demo/registry/instances/{serviceId} → 获取实例列表
│   │       // GET /demo/registry/self → 查看自身注册信息
│   │       // 注入 DiscoveryClient（统一接口，不关心底层是 Consul/Nacos/ZK）
│   │
│   │   ══════ 声明式调用 ══════
│   ├── feign/
│   │   ├── FeignDemo.java               ← Part A 原理模拟（已有，不动）
│   │   ├── UserFeignClient.java         ← @FeignClient 声明式接口
│   │   │   // @FeignClient(name = "user-service", fallbackFactory = UserFeignFallbackFactory.class)
│   │   │   // @GetMapping("/users/{id}") UserDTO getUser(@PathVariable Long id);
│   │   │   // @PostMapping("/users") UserDTO createUser(@RequestBody UserDTO user);
│   │   │   // @GetMapping("/users") List<UserDTO> listUsers();
│   │   ├── UserFeignFallbackFactory.java ← Fallback 工厂（可获取异常信息）
│   │   │   // implements FallbackFactory<UserFeignClient>
│   │   │   // 每个方法返回降级数据 + 记录异常日志
│   │   ├── UserDTO.java                 ← 数据传输对象
│   │   └── FeignController.java         ← REST 接口
│   │       // GET /demo/feign/user/{id} → 调用 UserFeignClient.getUser
│   │       // GET /demo/feign/users → 调用 UserFeignClient.listUsers
│   │       // POST /demo/feign/user → 调用 UserFeignClient.createUser
│   │       // GET /demo/feign/fallback → 模拟调用失败触发 Fallback
│   │
│   │   ══════ 熔断降级 ══════
│   ├── circuitbreaker/
│   │   ├── CircuitBreakerDemo.java      ← Part A 原理模拟（已有，不动）
│   │   └── CircuitBreakerController.java ← REST 接口
│   │       // GET /demo/cb/success → 正常调用（CLOSED 状态）
│   │       // GET /demo/cb/fail → 模拟失败（触发熔断 → OPEN）
│   │       // GET /demo/cb/status → 查看熔断器当前状态
│   │       // GET /demo/cb/reset → 重置熔断器
│   │       // @CircuitBreaker(name = "demoService", fallbackMethod = "fallback")
│   │
│   │   ══════ 消息队列 ══════
│   ├── mq/
│   │   ├── RabbitMQController.java      ← REST 接口
│   │   │   // POST /demo/mq/rabbit/send?msg=xxx → RabbitTemplate 发送消息
│   │   │   // GET /demo/mq/rabbit/history → 查看已消费的消息列表
│   │   │   // @RabbitListener(queues = "demo.queue") 自动消费
│   │   │   // 自动声明 Exchange + Queue + Binding
│   │   ├── KafkaController.java         ← REST 接口
│   │   │   // POST /demo/mq/kafka/send?msg=xxx → KafkaTemplate 发送消息
│   │   │   // GET /demo/mq/kafka/history → 查看已消费的消息列表
│   │   │   // @KafkaListener(topics = "demo-topic", groupId = "demo-group")
│   │   └── MqttController.java          ← REST 接口（可选）
│   │       // POST /demo/mq/mqtt/publish?topic=xxx&msg=xxx → 发布 MQTT 消息
│   │       // GET /demo/mq/mqtt/messages → 查看订阅收到的消息
│   │       // 使用 spring-integration-mqtt
│   │
│   │   ══════ 缓存 + 分布式锁 ══════
│   ├── cache/
│   │   └── RedisCacheController.java    ← REST 接口
│   │       // GET /demo/cache/get/{key} → RedisTemplate 读取
│   │       // POST /demo/cache/set?key=xxx&value=xxx&ttl=60 → RedisTemplate 写入
│   │       // DELETE /demo/cache/del/{key} → 删除缓存
│   │       // GET /demo/cache/user/{id} → @Cacheable 注解缓存（自动缓存）
│   │       // DELETE /demo/cache/user/{id} → @CacheEvict 清除缓存
│   │       // POST /demo/cache/lock?key=xxx&ttl=10 → Redisson 分布式锁
│   │       // DELETE /demo/cache/lock/{key} → 释放分布式锁
│   │
│   │   ══════ 数据库 ══════
│   ├── db/
│   │   └── JdbcController.java          ← REST 接口
│   │       // GET /demo/db/users → JdbcTemplate 查询所有用户
│   │       // GET /demo/db/users/{id} → 查询单个用户
│   │       // POST /demo/db/users → 创建用户
│   │       // GET /demo/db/init → 初始化测试表和数据
│   │       // GET /demo/db/pool → 查看连接池状态（HikariCP MXBean）
│   │
│   │   ══════ 搜索 ══════
│   ├── search/
│   │   ├── ArticleDocument.java         ← @Document 实体类
│   │   ├── ArticleRepository.java       ← ElasticsearchRepository 接口
│   │   └── EsController.java            ← REST 接口
│   │       // GET /demo/es/init → 初始化测试数据
│   │       // GET /demo/es/search?keyword=xxx → 全文搜索
│   │       // GET /demo/es/articles → 查询所有
│   │       // POST /demo/es/articles → 创建文档
│   │       // DELETE /demo/es/articles/{id} → 删除文档
│   │
│   │   ══════ MongoDB ══════
│   ├── mongo/
│   │   ├── UserDocument.java            ← @Document 实体类
│   │   ├── UserMongoRepository.java     ← MongoRepository 接口
│   │   └── MongoController.java         ← REST 接口
│   │       // GET /demo/mongo/init → 初始化测试数据
│   │       // GET /demo/mongo/users → 查询所有
│   │       // GET /demo/mongo/users/{id} → 查询单个
│   │       // POST /demo/mongo/users → 创建
│   │       // GET /demo/mongo/users/search?name=xxx → 按名称查询
│   │
│   │   ══════ 文件存储 ══════
│   ├── file/
│   │   └── MinioController.java         ← REST 接口
│   │       // POST /demo/file/upload → 上传文件（MultipartFile）
│   │       // GET /demo/file/download/{fileName} → 下载文件
│   │       // GET /demo/file/list → 列出所有文件
│   │       // DELETE /demo/file/{fileName} → 删除文件
│   │       // GET /demo/file/presigned/{fileName} → 获取预签名下载 URL
│   │       // 使用 MinIO SDK（非 Spring Starter）
│   │
│   │   ══════ 定时任务 ══════
│   ├── task/
│   │   └── ScheduledTaskController.java ← REST 接口 + 定时任务
│   │       // @Scheduled(cron = "0/30 * * * * ?") 每 30 秒执行
│   │       // 使用 Redis 分布式锁防止多实例重复执行
│   │       // GET /demo/task/status → 查看任务执行状态和历史
│   │       // POST /demo/task/trigger → 手动触发一次任务
│   │       // POST /demo/task/cron?expr=xxx → 动态修改 cron 表达式
│   │
│   │   ══════ 链路追踪 ══════
│   ├── tracing/
│   │   └── TracingDemo.java             ← Part A 原理模拟（已有，不动）
│   │       // Micrometer Tracing 自动集成，日志自动带 traceId
│   │       // 无需额外 Controller，所有接口自动有 traceId
│   │
│   │   ══════ 分布式事务 ══════
│   ├── transaction/
│   │   └── TransactionDemo.java         ← Part A 纯模拟（已有，不动，Seata 太重不加）
│   │
│   │   ══════ 分布式限流 ══════
│   ├── ratelimit/
│   │   └── RateLimitController.java     ← REST 接口
│   │       // POST /demo/ratelimit/fixed?key=api1&limit=10&window=60 → 固定窗口
│   │       // POST /demo/ratelimit/sliding?key=api1&limit=10&window=60 → 滑动窗口
│   │       // POST /demo/ratelimit/token-bucket?key=api1&rate=10&capacity=20 → 令牌桶
│   │       // GET /demo/ratelimit/stress-test?type=fixed&qps=50&duration=10 → 压测
│   │       // GET /demo/ratelimit/compare → 限流方案对比
│   │       // 使用 RedisTemplate + Lua 脚本
│   │
│   │   ══════ 分布式会话 ══════
│   ├── session/
│   │   ├── SessionController.java       ← REST 接口
│   │   │   // POST /demo/session/login?username=xxx → Redis Session 登录
│   │   │   // GET /demo/session/info → 获取当前 Session 信息
│   │   │   // POST /demo/session/logout → 销毁 Session
│   │   │   // POST /demo/session/jwt/login?username=xxx → JWT Token 登录
│   │   │   // GET /demo/session/jwt/verify?token=xxx → JWT 验证
│   │   │   // GET /demo/session/compare → Session vs JWT 方案对比
│   │   └── JwtUtil.java                 ← JWT 工具类
│   │
│   │   ══════ 缓存一致性 ══════
│   └── consistency/
│       └── CacheConsistencyController.java ← REST 接口
│           // GET /demo/consistency/init → 初始化测试数据（MySQL + Redis）
│           // PUT /demo/consistency/cache-aside?id=1&name=xxx → Cache Aside 更新
│           // PUT /demo/consistency/write-through?id=1&name=xxx → Write Through 更新
│           // PUT /demo/consistency/delay-double-delete?id=1&name=xxx → 延迟双删
│           // GET /demo/consistency/verify?id=1 → 验证 DB 和缓存一致性
│           // GET /demo/consistency/compare → 缓存一致性方案对比
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
│
└── src/test/java/...                    ← 测试
```

#### 模块 B：springcloud-gateway（网关，端口 8080，WebFlux，独立模块）

```
springcloud-gateway/
├── pom.xml                              ← spring-cloud-starter-gateway + consul-discovery
├── src/main/java/com/example/gateway/
│   ├── GatewayApp.java                  ← @SpringBootApplication + @EnableDiscoveryClient
│   ├── config/
│   │   └── GatewayRouteConfig.java      ← Java 代码配置路由
│   │       // @Bean RouteLocator 定义路由规则
│   │       // /api/demo/** → lb://springcloud-demo（负载均衡到业务服务）
│   │       // 路由断言：Path, Method, Header
│   │       // 过滤器：StripPrefix, AddRequestHeader, RequestRateLimiter
│   ├── filter/
│   │   ├── AuthGlobalFilter.java        ← 全局鉴权过滤器
│   │   │   // implements GlobalFilter, Ordered
│   │   │   // 检查 Authorization 头，无效返回 401
│   │   │   // 白名单路径跳过鉴权（如 /api/demo/registry/**）
│   │   ├── LogGlobalFilter.java         ← 全局日志过滤器
│   │   │   // 记录请求路径、耗时、响应状态码
│   │   └── RateLimitFilter.java         ← 限流过滤器
│   │       // 基于 Redis 的令牌桶限流
│   │       // 或使用 RequestRateLimiter GatewayFilter
│   └── src/main/resources/
│       └── application.yml              ← 网关配置
│           // server.port: 8080
│           // spring.cloud.consul 注册到 Consul
│           // spring.cloud.gateway.routes 路由规则
│           // 也可以从 Consul KV 动态加载路由
```

#### 模块 C：springcloud-sharding（分库分表，端口 8091，独立 DataSource）

```
springcloud-sharding/
├── pom.xml                              ← ShardingSphere Spring Boot Starter + MySQL + Consul
├── src/main/java/com/example/sharding/
│   ├── ShardingApp.java                 ← @SpringBootApplication + @EnableDiscoveryClient
│   ├── controller/
│   │   └── ShardingController.java      ← REST 接口
│   │       // GET /demo/sharding/init → 自动建表（order_0~order_3）+ 插入测试数据
│   │       // POST /demo/sharding/order → 插入订单（自动路由到分片表）
│   │       // GET /demo/sharding/orders → 查询所有订单（归并结果）
│   │       // GET /demo/sharding/orders/{userId} → 按分片键查询
│   │       // GET /demo/sharding/route-info → 查看分片路由信息（数据落在哪张表）
│   │       // GET /demo/sharding/compare → 分库分表方案对比
│   ├── entity/
│   │   └── OrderEntity.java             ← 订单实体
│   └── config/
│       └── ShardingDataSourceConfig.java ← ShardingSphere 数据源配置（Java Config）
├── src/main/resources/
│   ├── application.yml                  ← 默认配置（单库分表 + Consul 注册）
│   ├── application-db-table.yml         ← 分库分表配置（多库多表）
│   └── application-readwrite.yml        ← 读写分离配置
└── src/test/java/...
```

功能覆盖：
- 默认 Profile：单库分表（order_0~order_3，按 order_id 取模）
- `--spring.profiles.active=db-table`：分库分表（ds_0/ds_1 × order_0~order_3）
- `--spring.profiles.active=readwrite`：读写分离（主写从读）
- 分布式主键：雪花算法自动生成
- 广播表 + 绑定表
- 方案对比接口：ShardingSphere-JDBC / Proxy / MyCat / Vitess / TiDB

### 10.3 中间件可切换方案

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

### 10.4 pom.xml 依赖清单

springcloud-examples/pom.xml 需要的依赖：
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

springcloud-gateway/pom.xml 需要的依赖：
```
- spring-cloud-starter-gateway                 （网关核心，WebFlux）
- spring-cloud-starter-consul-discovery        （注册到 Consul）
- spring-boot-starter-data-redis-reactive      （限流用 Redis）
```

springcloud-sharding/pom.xml 需要的依赖：
```
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-cloud-starter-consul-discovery        （注册到 Consul）
- org.apache.shardingsphere:shardingsphere-jdbc-spring-boot-starter （分库分表核心）
- mysql-connector-j                            （MySQL 驱动）
- spring-boot-starter-jdbc                     （JDBC）
```

### 10.5 Docker 启动命令

```bash
# 启动全部中间件（一键启动）
docker compose -f docker/docker-compose.yml up -d          # MySQL + Redis + MongoDB + MinIO
docker compose -f docker/docker-compose.consul.yml up -d    # Consul
docker compose -f docker/docker-compose.mq.yml up -d        # RabbitMQ + Kafka + ZooKeeper
docker compose -f docker/docker-compose.es.yml up -d        # Elasticsearch
```

### 10.6 启动与验证流程

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

# 5. 验证接口
curl http://localhost:8090/demo/registry/services          # 注册发现
curl http://localhost:8090/demo/cache/set?key=test&value=hello  # Redis
curl -X POST http://localhost:8090/demo/mq/rabbit/send?msg=hello  # RabbitMQ
curl http://localhost:8090/demo/db/init                    # MySQL 初始化
curl http://localhost:8090/demo/es/init                    # ES 初始化
curl http://localhost:8090/demo/es/search?keyword=Java     # ES 搜索
curl http://localhost:8091/demo/sharding/init              # 分库分表初始化
curl http://localhost:8090/actuator/health                 # 健康检查

# 6. 切换注册中心为 Nacos
mvn spring-boot:run -Dspring-boot.run.profiles=nacos

# 7. 切换分库分表模式
cd code-examples/02-framework/springcloud-sharding
mvn spring-boot:run -Dspring-boot.run.profiles=db-table    # 分库分表
mvn spring-boot:run -Dspring-boot.run.profiles=readwrite   # 读写分离
```

### 10.7 不加入 Spring Cloud 项目的模块

| 模块 | 原因 | 保持位置 |
|------|------|---------|
| MinIO 原理模拟 | 用原生 SDK，在 SC 项目中只封装 REST 接口 | minio-examples（Part A 原理模拟） |
| Spring Security | 太重，独立学习更好 | 后续单独模块 |
| Apollo 配置中心 | 用 Consul KV 代替 | config-center-examples（纯模拟） |
| Seata 分布式事务 | 需要 Seata Server + 多微服务，太重 | springcloud-examples/transaction（纯模拟） |

### 10.8 自检清单（每个文件写完后必须逐项检查）

#### Java 文件自检：
- [ ] 编译检查：getDiagnostics 无错误
- [ ] Controller 有真实业务逻辑，不是空方法或只有注释
- [ ] @Autowired 注入的 Bean 有实际调用，不是摆设
- [ ] REST 接口有明确的请求/响应示例（Javadoc 中标注 curl 命令）
- [ ] 中间件连接配置与统一规范一致（地址/端口/用户名/密码）
- [ ] 有 prepareTestData 或 init 接口，方便初始化测试数据
- [ ] 有 cleanup 逻辑（可注释掉保留数据）
- [ ] 中文注释清晰，解释每个接口的用途和对应的 Spring 注解

#### 配置文件自检：
- [ ] application.yml 中每个中间件配置都有注释说明
- [ ] Profile 切换配置文件（application-xxx.yml）内容完整可用
- [ ] Docker 启动命令在 yml 注释中标注

#### md 文档自检：
- [ ] 代码示例链接指向正确的文件路径
- [ ] 有"在 Spring Cloud 项目中体验"章节
- [ ] curl 命令可直接复制执行
- [ ] Mermaid 图与代码实现一致

#### 整体自检（每完成一批文件后执行）：
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

#### 模块 A：springcloud-examples 任务清单（按执行顺序）

基础设施（10.A.1 ~ 10.A.6）：
- [x] 10.A.1 pom.xml — 更新 Spring Cloud 全家桶依赖 ✅
- [x] 10.A.2 SpringCloudApp.java — 启动类（@SpringBootApplication + @EnableFeignClients + @EnableDiscoveryClient + @EnableScheduling）✅
- [x] 10.A.3 application.yml — 默认配置（Consul + Redis + RabbitMQ + MySQL + ES + MongoDB + MinIO）✅
- [x] 10.A.4 application-consul.yml / application-nacos.yml / application-zk.yml — 注册中心 Profile 配置 ✅
- [x] 10.A.5 application-rabbitmq.yml / application-kafka.yml / application-sentinel.yml — 中间件 Profile 配置 ✅
- [x] 10.A.6 logback-spring.xml — 日志配置（含 traceId）✅

Spring Boot 基础实战（10.A.7 ~ 10.A.10）：
- [x] 10.A.7 boot/IoCController.java — @Autowired/@Qualifier/@Primary 注入方式演示 ✅
- [x] 10.A.8 boot/AopController.java — @Aspect 切面（日志/耗时/权限切面）✅
- [x] 10.A.9 boot/ExceptionController.java — @ControllerAdvice 全局异常处理 + 统一响应格式 ✅
- [x] 10.A.10 boot/ValidateController.java — @Valid + @Validated 参数校验 + 自定义校验注解 ✅

Spring Cloud 核心（10.A.11 ~ 10.A.14）：
- [x] 10.A.11 registry/RegistryController.java — DiscoveryClient 注册发现 ✅
- [x] 10.A.12 feign/UserFeignClient.java + UserFeignFallbackFactory.java + UserDTO.java + FeignController.java — 声明式调用 + Fallback ✅
- [x] 10.A.13 circuitbreaker/CircuitBreakerController.java — Resilience4j 熔断降级 ✅
- [x] 10.A.14 tracing/ — 链路追踪（已有 TracingDemo.java 不动，Micrometer 自动集成）✅

消息队列（10.A.15 ~ 10.A.17）：
- [x] 10.A.15 mq/RabbitMQController.java — RabbitTemplate + @RabbitListener ✅
- [x] 10.A.16 mq/KafkaController.java — KafkaTemplate + @KafkaListener ✅
- [ ] 10.A.17 mq/MqttController.java — spring-integration-mqtt（可选）

数据存储（10.A.18 ~ 10.A.22）：
- [x] 10.A.18 cache/RedisCacheController.java — RedisTemplate + @Cacheable + Redisson 分布式锁 ✅
- [x] 10.A.19 db/JdbcController.java — JdbcTemplate + HikariCP 连接池状态 ✅
- [x] 10.A.20 search/ArticleDocument.java + ArticleRepository.java + EsController.java — Spring Data ES ✅
- [x] 10.A.21 mongo/UserDocument.java + UserMongoRepository.java + MongoController.java — Spring Data MongoDB ✅
- [x] 10.A.22 file/MinioController.java — MinIO SDK 文件上传/下载/预签名 ✅

定时任务（10.A.23）：
- [x] 10.A.23 task/ScheduledTaskController.java — @Scheduled + Redis 分布式锁防重复 + 动态 cron ✅

分布式（10.A.24 ~ 10.A.26）：
- [x] 10.A.24 ratelimit/RateLimitController.java — RedisTemplate + Lua 三种限流算法 + 压测对比 ✅
- [x] 10.A.25 session/SessionController.java + JwtUtil.java — Redis Session + JWT Token 对比 ✅
- [x] 10.A.26 consistency/CacheConsistencyController.java — Cache Aside / Write Through / 延迟双删 ✅

#### 模块 B：springcloud-gateway 任务清单（10.B.1 ~ 10.B.7）

- [x] 10.B.1 pom.xml — Gateway + Consul + Redis Reactive ✅
- [x] 10.B.2 GatewayApp.java — 启动类 ✅
- [x] 10.B.3 application.yml — 网关配置（路由规则 + Consul 注册）✅
- [x] 10.B.4 config/GatewayRouteConfig.java — Java 代码配置路由（RouteLocator）✅
- [x] 10.B.5 filter/AuthGlobalFilter.java — 全局鉴权过滤器 ✅
- [x] 10.B.6 filter/LogGlobalFilter.java — 全局日志过滤器 ✅
- [x] 10.B.7 filter/RateLimitFilter.java — Redis 令牌桶限流过滤器 ✅

#### 模块 C：springcloud-sharding 任务清单（10.C.1 ~ 10.C.6）

- [x] 10.C.1 pom.xml — ShardingSphere Spring Boot Starter + MySQL + Consul ✅
- [x] 10.C.2 ShardingApp.java — 启动类 ✅
- [x] 10.C.3 application.yml — 默认配置（单库分表 + Consul 注册）+ sharding-config.yaml ✅
- [x] 10.C.4 application-db-table.yml / application-readwrite.yml — 分库分表 / 读写分离 Profile ✅
- [x] 10.C.5 entity/OrderEntity.java — 订单实体 ✅
- [x] 10.C.6 controller/ShardingController.java — 分片 CRUD + 路由信息 + 方案对比 ✅

#### 模块 D：springcloud-user-service 任务清单（10.D.1 ~ 10.D.5）

- [x] 10.D.1 pom.xml — spring-boot-starter-web + consul-discovery + jdbc + redis ✅
- [x] 10.D.2 UserServiceApp.java — 启动类 ✅
- [x] 10.D.3 application.yml — 端口 8092 + Consul 注册（spring.application.name=user-service）✅
- [x] 10.D.4 entity/User.java + service/UserService.java — 用户实体 + 业务逻辑（JdbcTemplate + Redis 缓存）✅
- [x] 10.D.5 controller/UserController.java — REST 接口（GET/POST /users，被 Feign 调用）✅

### 10.9 md 文档同步更新

批次 10 完成后，需要同步更新以下 md 文档：

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



### 批次 11 & 12：合并到批次 10 Spring Cloud 项目

原批次 11（分库分表）→ 独立模块 C（springcloud-sharding），已在 10.C 任务清单中
原批次 12（分布式限流/会话/缓存一致性）→ 集成到模块 A（springcloud-examples），已在 10.A.24~10.A.26 任务清单中

以下为各模块的详细设计说明（接口定义、方案对比等）。

#### 分库分表（模块 C：springcloud-sharding，独立 DataSource）

```
sharding/
├── ShardingController.java              ← REST 接口
│   // GET /demo/sharding/init → 自动建表（order_0~order_3）+ 插入测试数据
│   // POST /demo/sharding/order → 插入订单（自动路由到分片表）
│   // GET /demo/sharding/orders → 查询所有订单（归并结果）
│   // GET /demo/sharding/orders/{userId} → 按分片键查询
│   // GET /demo/sharding/route-info → 查看分片路由信息（数据落在哪张表）
│   // GET /demo/sharding/compare → 分库分表方案对比
├── ShardingConfig.java                  ← ShardingSphere 数据源配置（Java Config 方式）
└── OrderEntity.java                     ← 订单实体
```

功能覆盖：
- 单库分表：order_0~order_3，按 order_id 取模
- 分布式主键：雪花算法自动生成
- 广播表：config 表所有分片同步
- 方案对比（Javadoc + 接口返回）：ShardingSphere-JDBC / Proxy / MyCat / Vitess / TiDB

使用场景注释：
- 单表 > 500 万行 → 分表
- 单库 QPS > 5000 或数据量 > 1 亿 → 分库分表
- 读写比 > 7:3 → 读写分离

配置文件：
- application-sharding.yml — ShardingSphere 分片规则
- 启动方式：`--spring.profiles.active=sharding`

#### 分布式限流（模块 A：springcloud-examples/ratelimit/）

```
ratelimit/
└── RateLimitController.java             ← REST 接口
    // POST /demo/ratelimit/fixed?key=api1&limit=10&window=60 → 固定窗口限流
    // POST /demo/ratelimit/sliding?key=api1&limit=10&window=60 → 滑动窗口限流
    // POST /demo/ratelimit/token-bucket?key=api1&rate=10&capacity=20 → 令牌桶限流
    // GET /demo/ratelimit/stress-test?type=fixed&qps=50&duration=10 → 压测对比
    // GET /demo/ratelimit/compare → 限流方案对比
    // 使用 RedisTemplate + Lua 脚本实现
```

方案对比（Javadoc + 接口返回）：
| 方案 | 精度 | 实现复杂度 | 适用场景 |
|------|------|-----------|---------|
| 固定窗口 | 低（边界突刺） | 简单 | 粗粒度限流 |
| 滑动窗口 | 高 | 中等 | API 限流 |
| 令牌桶 | 高（允许突发） | 较复杂 | 流量整形 |
| Sentinel | 高 | 低（框架） | Spring Cloud 集成 |
| Gateway RequestRateLimiter | 高 | 低（配置） | 网关层限流 |

#### 分布式会话（模块 A：springcloud-examples/session/）

```
session/
├── SessionController.java              ← REST 接口
│   // POST /demo/session/login?username=xxx → Redis Session 登录
│   // GET /demo/session/info → 获取当前 Session 信息
│   // POST /demo/session/logout → 销毁 Session
│   // POST /demo/session/jwt/login?username=xxx → JWT Token 登录
│   // GET /demo/session/jwt/verify?token=xxx → JWT 验证
│   // GET /demo/session/compare → Session vs JWT 方案对比
└── JwtUtil.java                         ← JWT 工具类（生成/验证/刷新）
```

方案对比（Javadoc + 接口返回）：
| 方案 | 状态 | 扩展性 | 安全性 | 适用场景 |
|------|------|--------|--------|---------|
| Redis Session | 有状态 | 需 Redis | 服务端控制 | 传统 Web |
| JWT Token | 无状态 | 天然分布式 | 无法主动失效 | 微服务/移动端 |
| JWT + Redis | 混合 | 需 Redis | 可主动失效 | 兼顾两者 |
| Spring Session | 有状态 | 框架封装 | 服务端控制 | Spring 项目 |

#### 缓存一致性（模块 A：springcloud-examples/consistency/）

```
consistency/
└── CacheConsistencyController.java      ← REST 接口
    // GET /demo/consistency/init → 初始化测试数据（MySQL + Redis）
    // PUT /demo/consistency/cache-aside?id=1&name=xxx → Cache Aside 模式更新
    // PUT /demo/consistency/write-through?id=1&name=xxx → Write Through 模式更新
    // PUT /demo/consistency/delay-double-delete?id=1&name=xxx → 延迟双删模式更新
    // GET /demo/consistency/verify?id=1 → 验证 DB 和缓存是否一致
    // GET /demo/consistency/compare → 缓存一致性方案对比
    // 使用 RedisTemplate + JdbcTemplate
```

方案对比（Javadoc + 接口返回）：
| 方案 | 一致性 | 复杂度 | 性能 | 适用场景 |
|------|--------|--------|------|---------|
| Cache Aside | 最终一致 | 简单 | 高 | 大多数场景（推荐） |
| Write Through | 强一致 | 中等 | 中 | 一致性要求高 |
| Write Behind | 最终一致 | 复杂 | 最高 | 写密集场景 |
| 延迟双删 | 最终一致 | 中等 | 高 | 并发更新场景 |
| Binlog 同步 | 最终一致 | 较复杂 | 高 | 大规模系统 |

#### md 文档更新（原批次 11 + 12 的文档需求合并）

- [x] `docs/3-data-store/3.1-database/05-sharding.md` — 补充 ShardingSphere Spring Boot 实战 + 方案对比 + Mermaid 图 ✅
- [x] `docs/5-distributed/5.1-distributed/06-rate-limiting.md` — 补充 Redis+Lua 限流实战 + 方案对比 ✅
- [x] `docs/8-architecture/06-distributed-session.md` — 补充 Redis Session + JWT 实战 + 方案对比 ✅
- [x] `docs/8-architecture/08-cache-db-consistency.md` — 补充双写一致性实战 + 方案对比 ✅

### 整体执行顺序

```
1. Part B 补充（P0）：2.1、2.2 → 需先给 ES 模块 pom.xml 加依赖
2. Part B 补充（P1）：3.1、1.4、1.6 → 需先给对应模块 pom.xml 加依赖
3. 批次 2 剩余：2.3、2.4、2.5（直接按混合模式写）
4. 批次 3 剩余：3.2、3.3、3.4
5. 批次 4~9：按原顺序执行
6. 补充 Part B（Redis 等不在改进计划中的文件）
7. 批次 10：Spring Cloud 实战项目（含分库分表 + 分布式限流/会话/缓存一致性）
```

---

## 不在改进计划中但需要补充 Part B 的文件

以下文件当前 println 占比 < 30%，代码质量尚可，但缺少真实中间件连接部分，建议后续补充：

### Redis 模块（补充 Jedis 真实操作）

路径前缀：`code-examples/03-data-store/redis-examples/src/main/java/com/example/redis/`

- [x] `cache/CacheProblemsDemo.java`（19%）→ ✅ Part B 已补充：Jedis 演示缓存穿透/空值/互斥锁/随机过期
- [x] `lock/DistributedLockDemo.java`（15%）→ ✅ Part B 已补充：Jedis SET NX EX + Lua 释放锁 + 锁竞争 + 锁过期
- [x] `spring/RedisIntegrationDemo.java`（25%）→ ✅ Part B 已补充：Jedis 五种数据类型操作 + Pipeline 批量

### 网络编程模块（当前质量可以，可选补充）

路径前缀：`code-examples/02-framework/network-programming/src/main/java/com/example/network/`

- [ ] `http/HttpDemo.java`（16%）→ 可选：补充真实 HTTP 请求示例
- [ ] `tcp/TCPDemo.java`（9%）→ 质量良好，暂不改动
- [ ] `websocket/WebSocketDemo.java`（7%）→ 质量良好，暂不改动
- [ ] `rpc/RPCDemo.java`（5%）→ 质量良好，暂不改动

### SpringBoot 模块（当前 0% println，纯注解/配置代码，暂不改动）

### Java 核心模块（当前质量良好，暂不改动）
- concurrent-programming：10 个文件，println 均 ≤ 20%
- design-patterns：大部分 ≤ 20%（除 SpringPatternsDemo 79% 已在批次 9）
- java-basics：大部分 ≤ 25%（除 DataTypesDemo 30% 已在批次 9）
- java-advanced：JMMDemo 28%、CollectionSourceDemo 24%、SerializationDemo 25% — 边界值，可选优化

### AI 模块（当前质量良好，暂不改动）
- 4 个文件 println 均 ≤ 11%

---

## 依赖管理

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

---

## 进度统计

| 批次 | 文件数 | 已完成 | 待处理 | 模式 |
|------|--------|--------|--------|------|
| 1. 数据库 | 8 | 8 | 0 | 纯模拟 |
| 2. Elasticsearch | 5 | 5 | 0 | 混合 |
| 3. RabbitMQ | 4 | 4 | 0 | 混合 |
| 4. Kafka + MQTT | 5 | 5 | 0 | 混合 |
| 5. Redis + MongoDB + MinIO | 3 | 3 | 0 | 混合 |
| 6. Spring Cloud | 4 | 4 | 0 | 纯模拟 |
| 7. 注册中心 + 配置中心 | 7 | 7 | 0 | 混合/纯模拟 |
| 8. 分布式 + DevOps | 4 | 4 | 0 | 混合/纯模拟 |
| 9. JVM + Java 进阶 | 11 | 11 | 0 | 纯模拟 |
| **改进计划合计** | **51** | **51** | **0** | ✅ 全部完成 |
| 已完成文件补 Part B（必须） | 2 | 2 | 0 | ES 聚合+查询 ✅ |
| 已完成文件补 Part B（可选） | 3 | 3 | 0 | RabbitMQ+DB优化+连接池 ✅ |
| 补充 Part B（Redis 等） | 3 | 3 | 0 | 补充真实连接 ✅ |
| 10. Spring Cloud 实战项目 | ~45 | ~45 | 0 | 4 模块（业务服务+网关+分库分表+用户服务）✅ |
| ~~11. 分库分表~~ | — | — | — | 已合并到批次 10 模块 C |
| ~~12. 分布式补充~~ | — | — | — | 已合并到批次 10 模块 A |

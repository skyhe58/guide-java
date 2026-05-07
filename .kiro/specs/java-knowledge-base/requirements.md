# 需求文档：Java 个人知识库

## 简介

本项目旨在构建一个面向 Java 开发者的个人知识库系统，涵盖 Java 技术体系的核心知识点，包括 Java 基础与深入、JVM、Spring Boot、Spring Cloud、中间件（Redis、MQ、Kafka、Elasticsearch）、Nginx、配置中心（Apollo/Nacos）、注册中心、数据库（MySQL 深入/分库分表/Binlog）、分布式系统理论、Linux 运维基础以及 AI 应用等领域。知识库以实际代码示例为核心，服务于日常学习、知识复习和面试准备三大场景。

## 术语表

- **Knowledge_Base（知识库）**: 本系统的核心应用，用于组织、存储和检索 Java 技术知识
- **Knowledge_Module（知识模块）**: 按技术领域划分的独立知识单元，如 "Java 基础"、"Spring Boot" 等
- **Knowledge_Entry（知识条目）**: 知识模块中的具体知识点，包含概念说明、代码示例和面试要点
- **Code_Example（代码示例）**: 可独立运行的代码片段，用于演示具体知识点
- **Interview_Guide（面试指南）**: 针对特定知识点的面试高频问题、答题思路和深入追问
- **Navigation_System（导航系统）**: 知识库的目录结构和索引机制，支持按模块、标签、难度等维度浏览
- **Search_Engine（搜索引擎）**: 知识库的全文检索功能，支持关键词搜索和模糊匹配
- **Tag_System（标签系统）**: 为知识条目添加分类标签，如难度级别、技术领域、面试频率等
- **Learning_Path（学习路径）**: 按照知识依赖关系编排的推荐学习顺序

---

## 实现方式对比与推荐

在创建需求之前，先对比几种常见的个人知识库实现方式，帮助选择最合适的技术方案。

### 方式一：纯 Markdown + Git 仓库（推荐 ⭐⭐⭐⭐⭐）

| 维度 | 说明 |
|------|------|
| 技术栈 | Markdown 文件 + Git + 静态站点生成器（VitePress / Docusaurus / MkDocs） |
| 优点 | 零运维成本；版本控制天然支持；可离线使用；GitHub Pages 免费部署；专注内容本身；代码示例可直接运行 |
| 缺点 | 搜索能力依赖静态站点工具；无动态交互功能 |
| 适合场景 | 个人学习、面试复习、长期维护 |
| 部署方式 | GitHub Pages / Vercel / Netlify 免费托管 |

### 方式二：Spring Boot Web 应用

| 维度 | 说明 |
|------|------|
| 技术栈 | Spring Boot + MySQL/PostgreSQL + Vue/React 前端 |
| 优点 | 可练习全栈开发；支持动态功能（搜索、收藏、笔记）；可扩展性强 |
| 缺点 | 开发和运维成本高；需要服务器；维护精力分散到系统开发而非知识积累 |
| 适合场景 | 想同时练习全栈开发的场景 |
| 部署方式 | 云服务器自行部署 |

### 方式三：Notion / Obsidian 等笔记工具

| 维度 | 说明 |
|------|------|
| 技术栈 | Notion / Obsidian / 语雀 |
| 优点 | 开箱即用；编辑体验好；支持多端同步 |
| 缺点 | 代码示例无法直接运行；数据不完全自主可控；导出迁移困难；不利于展示给面试官 |
| 适合场景 | 快速记录零散笔记 |
| 部署方式 | 平台托管 |

### 方式四：混合方案（推荐采用 ⭐⭐⭐⭐⭐）

| 维度 | 说明 |
|------|------|
| 技术栈 | Markdown 知识文档 + 独立 Java Maven 多模块项目（实际代码） + VitePress 静态站点 |
| 优点 | 知识文档和代码分离但关联；代码可独立编译运行和测试；文档可生成美观站点；Git 管理一切 |
| 缺点 | 初始搭建需要一定工作量 |
| 适合场景 | 长期维护的个人技术品牌，面试展示，持续学习 |
| 部署方式 | GitHub 仓库 + GitHub Pages |

### 推荐方案

采用**方式四：混合方案**，理由如下：
1. 知识文档用 Markdown 编写，通过 VitePress 生成可搜索的静态站点
2. 代码示例放在独立的 Maven 多模块项目中，每个技术领域一个子模块，可独立编译运行
3. 文档中通过链接引用代码，保持文档简洁的同时代码可实际执行
4. 整个项目用 Git 管理，托管在 GitHub，既是知识库也是个人技术展示

---

## 需求

### 需求 1：知识模块体系结构

**用户故事：** 作为一名 Java 开发者，我希望知识库按技术领域划分为清晰的模块，以便我能系统地组织和查阅知识。

#### 验收标准

1. THE Knowledge_Base SHALL 包含以下核心知识模块：Java 基础、Java 进阶、JVM、Spring Boot、Spring Cloud、Redis、消息队列（RabbitMQ/Kafka）、Elasticsearch、数据库（MySQL 深入）、Nginx 与反向代理、配置中心（Apollo/Nacos）、注册中心与服务治理、AI 应用、设计模式、并发编程、网络与协议、Linux 与运维基础、分布式系统理论、数据结构与算法、Docker 与 Kubernetes
2. WHEN 用户访问任意知识模块时，THE Navigation_System SHALL 展示该模块下所有知识条目的目录列表，并按照从基础到进阶的顺序排列
3. THE Knowledge_Module SHALL 包含模块概述、知识点列表、推荐学习顺序和相关模块链接四个组成部分
4. WHEN 用户浏览某个知识模块时，THE Knowledge_Base SHALL 显示该模块与其他模块之间的关联关系

### 需求 2：知识条目内容规范

**用户故事：** 作为一名 Java 开发者，我希望每个知识点都有统一的内容结构，以便我能高效地学习和复习。

#### 验收标准

1. THE Knowledge_Entry SHALL 包含以下结构：概念说明、核心原理、代码示例链接、常见面试题、深入追问、参考资料
2. THE Knowledge_Entry SHALL 标注难度级别（初级、中级、高级）和面试频率（高频、中频、低频）
3. EVERY Knowledge_Entry SHALL 提供至少一个指向 code-examples 子模块中可独立运行的代码示例链接，确保每个知识点都有对应的可执行代码
4. THE Knowledge_Entry SHALL 使用 Markdown 格式编写，支持代码高亮、表格、流程图（Mermaid）等富文本元素
5. IF 知识条目涉及复杂流程或原理（如 HashMap 扩容、Spring Boot 启动流程、GC 过程、线程池执行流程、分布式事务流程等），THEN THE Knowledge_Entry SHALL 包含 Mermaid 流程图或时序图来可视化说明

### 需求 3：可运行代码示例项目

**用户故事：** 作为一名 Java 开发者，我希望知识库中的代码示例可以独立编译和运行，以便我能通过实践加深理解。

#### 验收标准

1. THE Code_Example SHALL 组织为 Maven 多模块项目，每个技术领域对应一个独立子模块
2. THE Code_Example SHALL 包含以下子模块：java-basics、java-advanced、jvm-deep-dive、springboot-examples、springcloud-examples、redis-examples、mq-examples、elasticsearch-examples、database-examples、nginx-examples、config-center-examples、ai-examples、design-patterns、concurrent-programming、distributed-examples、network-programming
3. WHEN 用户克隆项目仓库后，THE Code_Example SHALL 能够通过 `mvn compile` 命令成功编译所有子模块
4. THE Code_Example SHALL 为每个代码示例提供独立的 main 方法或单元测试，使其可以单独运行
5. THE Code_Example SHALL 在代码中包含详细的中文注释，解释关键逻辑和知识点
6. IF 代码示例依赖外部服务（如 Redis、Kafka），THEN THE Code_Example SHALL 提供 Docker Compose 配置文件以便一键启动依赖环境


### 需求 4：面试准备体系

**用户故事：** 作为一名准备面试的 Java 开发者，我希望知识库提供系统的面试指南，以便我能高效地准备技术面试。

#### 验收标准

1. THE Interview_Guide SHALL 为每个知识模块提供高频面试题列表，并按出现频率排序
2. THE Interview_Guide SHALL 为每道面试题提供：标准答案、答题思路、深入追问及应对策略、易错点提醒
3. WHEN 用户查看面试题时，THE Interview_Guide SHALL 标注该题目的难度级别和在实际面试中的出现频率
4. THE Interview_Guide SHALL 提供按公司类型（大厂、中厂、创业公司）分类的面试重点汇总
5. THE Interview_Guide SHALL 为每个技术领域提供面试知识图谱，展示知识点之间的关联和常见追问路径

### 需求 5：Java 核心知识覆盖

**用户故事：** 作为一名 Java 开发者，我希望知识库覆盖 Java 语言从基础到深入的完整知识体系，以便我能夯实基础并深入理解底层原理。

#### 验收标准

1. THE Knowledge_Base SHALL 在 Java 基础模块中覆盖以下主题：数据类型与包装类（自动装箱拆箱/缓存池机制）、值传递与引用传递（Java 只有值传递的本质/对象引用传递的误区）、String 深入（String 不可变性/String Pool/String vs StringBuilder vs StringBuffer/intern 方法）、面向对象（封装原理与访问控制/继承机制与方法重写规则 @Override/多态实现原理与虚方法表/抽象类与接口区别及使用场景/内部类（成员内部类/静态内部类/匿名内部类/局部内部类）/Object 常用方法 equals/hashCode/toString/clone/finalize/向上转型与向下转型/instanceof 与类型转换）、集合框架深入（ArrayList vs LinkedList 区别与源码、ArrayList 扩容机制、LinkedList 双向链表实现、HashSet 去重原理与 hashCode/equals 契约、LinkedHashSet 有序性原理、TreeSet 与 Comparator/Comparable、HashMap 扩容机制与红黑树转换、HashMap 线程不安全分析、LinkedHashMap 实现 LRU 缓存、TreeMap 排序原理与红黑树、Hashtable vs ConcurrentHashMap 对比、List/Set/Map 遍历与删除的坑（ConcurrentModificationException）、Collections.unmodifiableList 与不可变集合、Arrays.asList 的坑、Collections 工具类）、异常处理（Checked vs Unchecked/自定义异常/异常处理最佳实践）、泛型（类型擦除/通配符/上下界）、反射（Class 对象/Field/Method/Constructor 操作、反射性能优化、反射在框架中的应用）、注解（自定义注解/元注解/注解处理器）、文件与流处理（File API/字节流/字符流/缓冲流/转换流/对象流、try-with-resources、NIO Files/Path/Channel/Buffer、大文件读写与内存映射 MappedByteBuffer）、Lambda 与 Stream API（常用操作/并行流/Collector 自定义）、JDK 版本特性演进（JDK 8 核心特性：Lambda/Stream/Optional/Date-Time API/接口默认方法；JDK 17 LTS 特性：Sealed Classes/Pattern Matching for instanceof/Records/Text Blocks/Switch 表达式；JDK 21 LTS 特性：Virtual Threads 虚拟线程/Structured Concurrency/Record Patterns/Sequenced Collections/String Templates；各版本间迁移注意事项与兼容性）
2. THE Knowledge_Base SHALL 在 Java 进阶模块中覆盖以下主题：集合源码分析（HashMap/ConcurrentHashMap/LinkedHashMap/TreeMap）、类加载机制（双亲委派/打破双亲委派）、动态代理（JDK 动态代理/CGLIB/Javassist/ByteBuddy）、SPI 机制、序列化机制（Java 原生/JSON/Protobuf 对比）、网络编程（BIO/NIO/AIO 模型对比、Socket 编程、Netty 框架核心原理、Netty 编解码与粘包拆包、零拷贝原理）、Java 内存模型（JMM）与 happens-before 规则
3. THE Knowledge_Base SHALL 在并发编程模块中覆盖以下主题：线程生命周期、synchronized 原理（偏向锁/轻量级锁/重量级锁升级过程、锁消除/锁粗化）、ReentrantLock 与 AQS 源码分析、ReadWriteLock 与 StampedLock、volatile 原理与内存屏障、线程池原理与最佳实践（核心参数/拒绝策略/动态调参）、并发工具类（CountDownLatch/CyclicBarrier/Semaphore/Phaser/Exchanger）、ThreadLocal 原理与内存泄漏、CompletableFuture 异步编程、CAS 与原子类（AtomicInteger/LongAdder）、死锁检测与避免、无锁编程与 Disruptor
4. THE Knowledge_Base SHALL 在 JVM 模块中覆盖以下主题：内存模型与内存区域、垃圾回收算法与收集器（CMS/G1/ZGC/Shenandoah）、类加载过程、JIT 编译与逃逸分析、JVM 调优参数、GC 日志分析、内存泄漏排查、常用诊断工具（jstack/jmap/jstat/arthas/async-profiler）
5. THE Knowledge_Base SHALL 在设计模式模块中覆盖以下主题：创建型模式（单例/工厂/抽象工厂/建造者/原型）、结构型模式（代理/适配器/装饰器/门面/桥接/组合/享元）、行为型模式（策略/模板方法/观察者/责任链/状态/命令/迭代器）、设计模式在 Spring 框架中的应用（如 Spring 中的工厂模式/代理模式/模板方法/观察者模式）、设计原则（SOLID/DRY/KISS）
6. THE Knowledge_Base SHALL 在网络与协议模块中覆盖以下主题：TCP/IP 协议栈、TCP 三次握手与四次挥手、HTTP/HTTPS 协议详解、HTTP/2 与 HTTP/3 特性、WebSocket 协议、DNS 解析过程、CDN 原理、网络安全基础（XSS/CSRF/SQL 注入防护）、RESTful API 设计规范、RPC 框架原理（Dubbo/gRPC）

### 需求 6：Spring 生态知识覆盖

**用户故事：** 作为一名使用 Spring 技术栈的开发者，我希望知识库深入覆盖 Spring Boot 和 Spring Cloud 的核心知识，以便我能在工作中熟练运用并应对面试。

#### 验收标准

1. THE Knowledge_Base SHALL 在 Spring Boot 模块中覆盖以下主题：Spring 核心原理（IoC 容器/依赖注入/Bean 生命周期/BeanPostProcessor）、AOP 原理（动态代理实现/切面执行顺序/事务失效场景）、循环依赖解决机制（三级缓存原理）、Spring Boot 启动加载流程（SpringApplication.run 全过程/自动配置原理/@EnableAutoConfiguration/spring.factories 与 AutoConfiguration.imports）、Starter 机制与自定义 Starter 开发、配置文件体系（application.yml/application.properties/bootstrap.yml 加载顺序与优先级/Profile 多环境配置/配置属性绑定 @ConfigurationProperties）、Web 开发（RESTful API/拦截器/过滤器/全局异常处理）、数据访问（JPA/MyBatis/MyBatis-Plus）、安全框架（Spring Security/OAuth2）、日志体系（SLF4J + Logback 配置/日志级别动态调整/日志格式自定义/MDC 链路追踪日志/ELK 日志收集方案）、缓存集成（@Cacheable 注解体系/Redis 缓存集成）、定时任务（@Scheduled/XXL-Job）、Actuator 监控与健康检查
2. THE Knowledge_Base SHALL 在 Spring Cloud 模块中覆盖以下主题：服务注册与发现（Consul 优先/Nacos/Eureka）、负载均衡（Ribbon/LoadBalancer）、服务调用（OpenFeign/超时与重试配置）、熔断降级（Sentinel/Resilience4j）、网关（Gateway 路由/过滤器/限流/鉴权）、配置中心（Apollo/Nacos Config）、链路追踪（Sleuth/Micrometer Tracing + Zipkin/SkyWalking、TraceId 透传机制、日志与链路关联）、分布式事务（Seata AT/TCC 模式）
3. WHEN 用户学习 Spring Boot 模块时，THE Knowledge_Base SHALL 提供从零搭建项目到生产部署的完整实战案例
4. THE Knowledge_Base SHALL 提供 Spring Boot 与 Spring Cloud 各组件的版本兼容性对照表

### 需求 7：中间件知识覆盖

**用户故事：** 作为一名 Java 后端开发者，我希望知识库覆盖常用中间件的原理和实践，以便我能在工作中正确使用并排查问题。

#### 验收标准

1. THE Knowledge_Base SHALL 在 Redis 模块中覆盖以下主题：数据结构与底层实现、持久化机制（RDB/AOF）、主从复制、哨兵模式、Cluster 集群、缓存穿透/击穿/雪崩解决方案、分布式锁实现、Redis 与 Spring Boot 集成
2. THE Knowledge_Base SHALL 在消息队列模块中覆盖以下主题：RabbitMQ 核心概念与使用、Kafka 架构与原理、消息可靠性保证、消息幂等性处理、消息顺序性保证、死信队列、延迟消息、与 Spring Boot 集成
3. THE Knowledge_Base SHALL 在 Elasticsearch 模块中覆盖以下主题：倒排索引原理、映射与分析器、CRUD 操作、复合查询（DSL）、聚合分析、与 Spring Data Elasticsearch 集成、性能优化
4. THE Knowledge_Base SHALL 在数据库模块中覆盖以下主题：MySQL 索引原理（B+树）、事务与隔离级别（MVCC 实现原理）、锁机制（行锁/表锁/间隙锁/临键锁）、SQL 优化、执行计划分析（EXPLAIN 详解）、分库分表（ShardingSphere/MyCat）、读写分离、连接池（HikariCP/Druid）、Binlog 原理与应用（主从同步/数据恢复/Canal 数据订阅）、Redo Log 与 Undo Log、Buffer Pool 机制、慢查询分析与优化、数据库高可用方案（主从/MGR/Proxy）、分布式 ID 生成方案（雪花算法/Leaf）

### 需求 8：AI 应用知识覆盖

**用户故事：** 作为一名关注 AI 技术的 Java 开发者，我希望知识库包含 AI 在 Java 生态中的应用知识，以便我能跟上技术趋势。

#### 验收标准

1. THE Knowledge_Base SHALL 在 AI 应用模块中覆盖以下主题：Spring AI 框架使用、LLM API 集成（OpenAI/国内大模型）、RAG（检索增强生成）实现、向量数据库集成、Prompt Engineering 实践、AI Agent 开发
2. WHEN 用户学习 AI 应用模块时，THE Code_Example SHALL 提供可运行的 AI 集成示例代码，包括聊天机器人、文档问答、代码辅助等场景
3. THE Knowledge_Base SHALL 提供 Java 生态中 AI 相关框架和工具的对比分析（Spring AI、LangChain4j 等）

### 需求 9：导航与搜索功能

**用户故事：** 作为知识库的使用者，我希望能快速定位到需要的知识点，以便我能高效地学习和复习。

#### 验收标准

1. THE Navigation_System SHALL 提供多级目录导航，支持按知识模块、难度级别、面试频率三个维度浏览
2. THE Search_Engine SHALL 支持全文关键词搜索，搜索范围覆盖知识条目的标题、正文和代码注释
3. THE Tag_System SHALL 为每个知识条目支持多标签标注，标签类别包括：技术领域、难度级别、面试频率、知识类型（概念/原理/实战/面试）
4. WHEN 用户通过搜索引擎查询时，THE Search_Engine SHALL 在搜索结果中高亮显示匹配的关键词
5. THE Navigation_System SHALL 在每个知识条目页面底部展示相关推荐条目

### 需求 10：学习路径规划

**用户故事：** 作为一名希望系统学习的 Java 开发者，我希望知识库提供推荐的学习路径，以便我能按照合理的顺序循序渐进地学习。

#### 验收标准

1. THE Learning_Path SHALL 提供以下预设学习路径：Java 初学者路径、Java 中级进阶路径、Java 高级深入路径、面试突击路径、架构师成长路径
2. THE Learning_Path SHALL 为每条路径标注预计学习时长和前置知识要求
3. WHEN 用户选择某条学习路径时，THE Learning_Path SHALL 按照知识依赖关系展示有序的学习步骤列表
4. THE Learning_Path SHALL 在每个学习步骤中标注该步骤对应的知识条目链接和建议学习时间

### 需求 11：静态站点生成与部署

**用户故事：** 作为知识库的维护者，我希望知识库能生成美观的静态网站并方便部署，以便我能随时随地访问和分享。

#### 验收标准

1. THE Knowledge_Base SHALL 使用 VitePress 作为静态站点生成器，将 Markdown 文档转换为可浏览的网站
2. WHEN 用户执行构建命令时，THE Knowledge_Base SHALL 生成包含完整导航、搜索和主题切换功能的静态站点
3. THE Knowledge_Base SHALL 提供 GitHub Actions 配置文件，实现推送代码后自动构建并部署到 GitHub Pages
4. THE Knowledge_Base SHALL 支持明暗主题切换，并在移动端设备上保持良好的阅读体验
5. IF 构建过程中检测到 Markdown 文件存在断链（引用了不存在的文件或锚点），THEN THE Knowledge_Base SHALL 在构建日志中输出警告信息

### 需求 12：项目结构与维护规范

**用户故事：** 作为知识库的长期维护者，我希望项目有清晰的结构和规范，以便我能持续高效地添加和更新内容。

#### 验收标准

1. THE Knowledge_Base SHALL 采用以下顶层目录结构：`docs/`（Markdown 知识文档）、`code-examples/`（Maven 多模块代码项目）、`.github/`（CI/CD 配置）、`docker/`（Docker Compose 配置）
2. THE Knowledge_Base SHALL 在项目根目录提供 README.md，包含项目简介、快速开始指南、目录结构说明和贡献指南
3. THE Knowledge_Base SHALL 提供知识条目的 Markdown 模板文件，确保新增内容格式统一
4. WHEN 新增知识条目时，THE Knowledge_Base SHALL 通过模板文件引导维护者填写所有必要字段（概念说明、代码示例、面试题等）
5. THE Knowledge_Base SHALL 在 README.md 中维护一份内容完成度追踪表，标注每个知识模块的完成状态（未开始/进行中/已完成）

### 需求 13：Nginx 与反向代理知识覆盖

**用户故事：** 作为一名 Java 后端开发者，我希望知识库覆盖 Nginx 的核心知识和常见配置，以便我能在工作中正确配置和排查网络层问题。

#### 验收标准

1. THE Knowledge_Base SHALL 在 Nginx 模块中覆盖以下主题：Nginx 架构与工作原理（Master-Worker 模型）、配置文件结构与指令详解、反向代理配置、负载均衡策略（轮询/加权/IP Hash/最少连接）、动静分离、HTTPS 配置与证书管理、限流与防刷（limit_req/limit_conn）、跨域配置（CORS）、Gzip 压缩、日志分析与监控
2. THE Knowledge_Base SHALL 在 Nginx 模块中覆盖以下进阶主题：Nginx + Lua 扩展（OpenResty）、Nginx 与 Spring Cloud Gateway 的对比与配合、WebSocket 代理配置、高可用方案（Keepalived + Nginx）、性能调优参数
3. WHEN 用户学习 Nginx 模块时，THE Code_Example SHALL 提供常见场景的完整配置文件示例和 Docker Compose 一键部署环境

### 需求 14：配置中心与注册中心知识覆盖

**用户故事：** 作为一名微服务架构开发者，我希望知识库深入覆盖配置中心和注册中心的原理与实践，以便我能在分布式系统中正确管理配置和服务。

#### 验收标准

1. THE Knowledge_Base SHALL 在配置中心模块中覆盖以下主题：Apollo 架构设计与核心概念、Apollo 配置管理（Namespace/Cluster/环境隔离）、Apollo 客户端集成与热更新原理、Apollo 灰度发布、Apollo 高可用部署、Nacos Config 对比与使用、Spring Cloud Config 对比分析、配置加密与安全管理
2. THE Knowledge_Base SHALL 在注册中心模块中覆盖以下主题：服务注册与发现原理、Consul 架构与核心特性（服务发现/健康检查/KV 存储/多数据中心）、Consul 与 Spring Cloud 集成、Consul ACL 安全管理、Zookeeper 作为注册中心（ZAB 协议/临时节点/Watcher 机制）、Nacos 注册中心（AP/CP 模式切换）、Eureka 原理与自我保护机制、注册中心选型对比（CAP 理论视角：Consul CP vs Eureka AP vs Nacos 可切换）、服务健康检查机制、服务上下线与优雅停机
3. WHEN 用户学习配置中心模块时，THE Code_Example SHALL 提供 Apollo 和 Nacos 的完整集成示例，包括配置热更新、多环境管理等场景
4. THE Knowledge_Base SHALL 提供各注册中心的功能对比表和选型建议，优先推荐 Consul 作为注册中心方案
5. WHEN 用户学习注册中心模块时，THE Code_Example SHALL 优先提供 Consul + Spring Cloud 的完整集成示例，并补充 Zookeeper、Nacos 的对比示例

### 需求 15：分布式系统与 Linux 运维基础知识覆盖

**用户故事：** 作为一名 Java 后端开发者，我希望知识库覆盖分布式系统理论和 Linux 运维基础，以便我能更好地理解系统架构并应对面试中的相关问题。

#### 验收标准

1. THE Knowledge_Base SHALL 在分布式系统模块中覆盖以下主题：CAP 理论与 BASE 理论、分布式一致性算法（Raft/Paxos）、分布式锁实现方案对比（Redis/Zookeeper/MySQL）、分布式事务方案（2PC/TCC/Saga/消息最终一致性）、幂等性设计、限流算法（令牌桶/漏桶/滑动窗口）、熔断与降级策略、分布式链路追踪原理
2. THE Knowledge_Base SHALL 在 Linux 运维基础模块中覆盖以下主题：常用命令（文件/进程/网络/磁盘）、Shell 脚本基础、性能排查工具（top/vmstat/iostat/netstat）、日志分析（grep/awk/sed）、JVM 线上问题排查流程（CPU 飙高/内存溢出/线程死锁）
3. WHEN 用户学习分布式系统模块时，THE Code_Example SHALL 提供分布式锁、分布式事务等核心场景的可运行代码示例

### 需求 16：面试高频架构设计场景

**用户故事：** 作为一名准备面试的 Java 开发者，我希望知识库包含常见的系统设计面试题和方案分析，以便我能应对面试中的架构设计环节。

#### 验收标准

1. THE Knowledge_Base SHALL 在架构设计场景模块中覆盖以下主题：秒杀系统设计（限流/库存扣减/异步下单）、短链接系统设计、订单超时取消方案（延迟队列/定时任务/Redis 过期回调）、分布式缓存方案设计、接口幂等性设计方案、分布式 Session 方案（Redis/JWT/Spring Session）、大文件上传方案（分片/断点续传）、数据一致性方案（缓存与数据库双写一致性）
2. THE Knowledge_Base SHALL 为每个架构设计场景提供：问题分析、方案对比、推荐方案详解、核心代码实现、常见追问
3. WHEN 用户学习架构设计场景时，THE Code_Example SHALL 提供核心链路的可运行代码示例

### 需求 17：数据结构与算法知识覆盖

**用户故事：** 作为一名准备面试的 Java 开发者，我希望知识库覆盖面试高频算法和数据结构，以便我能系统地准备算法面试环节。

#### 验收标准

1. THE Knowledge_Base SHALL 在数据结构与算法模块中覆盖以下面试高频数据结构：数组与链表（反转链表/合并有序链表/环形链表检测）、栈与队列（有效括号/最小栈/用栈实现队列）、哈希表（两数之和/字母异位词分组/LRU 缓存实现）、树与二叉树（前中后序遍历/层序遍历/二叉搜索树/最近公共祖先/二叉树最大深度）、堆（TopK 问题/合并 K 个有序链表）、图（BFS/DFS 基础应用）
2. THE Knowledge_Base SHALL 在数据结构与算法模块中覆盖以下面试高频算法思想：排序算法（快速排序/归并排序/堆排序的原理与复杂度对比）、二分查找（基础二分/搜索旋转排序数组/查找第一个和最后一个位置）、双指针（三数之和/盛最多水的容器/滑动窗口最大值）、滑动窗口（无重复字符的最长子串/最小覆盖子串）、动态规划（爬楼梯/最长递增子序列/背包问题/编辑距离/最长公共子序列）、回溯算法（全排列/子集/N 皇后）、贪心算法（跳跃游戏/区间调度）
3. THE Knowledge_Base SHALL 为每道算法题提供：题目描述、解题思路、Java 代码实现（含详细注释）、时间空间复杂度分析、常见变体与追问
4. WHEN 用户学习算法模块时，THE Code_Example SHALL 在 `java-basics` 子模块中提供所有算法题的可运行 Java 实现和对应的单元测试
5. THE Knowledge_Base SHALL 按照面试频率对算法题进行排序，优先覆盖 LeetCode Hot 100 中的 Java 后端高频题

### 需求 18：Docker 与 Kubernetes 知识覆盖

**用户故事：** 作为一名 Java 后端开发者，我希望知识库覆盖 Docker 和 Kubernetes 的核心知识，以便我能掌握容器化部署和编排，应对工作和面试需求。

#### 验收标准

1. THE Knowledge_Base SHALL 在 Docker 模块中覆盖以下主题：Docker 核心概念（镜像/容器/仓库）、Dockerfile 编写最佳实践（多阶段构建/镜像瘦身）、Docker 网络模式（bridge/host/none/overlay）、Docker 数据卷与持久化、Docker Compose 编排（多服务编排/依赖管理/环境变量）、Java 应用 Docker 化（JVM 参数在容器中的调优/内存限制/cgroup 感知）、Docker 镜像私有仓库（Harbor）
2. THE Knowledge_Base SHALL 在 Kubernetes 模块中覆盖以下主题：K8s 架构与核心组件（Master/Node/etcd/API Server/Scheduler/Controller Manager）、核心资源对象（Pod/Deployment/Service/ConfigMap/Secret/Ingress）、Pod 生命周期与健康检查（livenessProbe/readinessProbe/startupProbe）、Service 类型与服务发现（ClusterIP/NodePort/LoadBalancer）、Ingress 与流量管理、HPA 自动扩缩容、K8s 部署策略（滚动更新/蓝绿部署/金丝雀发布）、Helm 包管理、Java 应用在 K8s 上的部署实践（优雅停机/配置管理/日志收集）
3. WHEN 用户学习 Docker 与 K8s 模块时，THE Code_Example SHALL 提供 Java 应用的 Dockerfile、Docker Compose 配置和 K8s YAML 部署清单示例
4. THE Knowledge_Base SHALL 提供 Docker 与 K8s 常用命令速查表
5. 创建 `docs/docker-k8s/interview.md` 面试指南

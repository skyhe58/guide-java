# Java 个人知识库

> 一个面向 Java 开发者的个人知识库系统，涵盖 Java 技术体系的核心知识点，服务于日常学习、知识复习和面试准备三大场景。

## 项目简介

本项目采用**混合方案**构建：Markdown 知识文档 + Maven 多模块代码项目 + VitePress 静态站点，三者通过 Git 统一管理。

- 📝 **知识文档**：使用 Markdown 编写，通过 VitePress 生成可搜索的静态站点
- 💻 **代码示例**：独立的 Maven 多模块项目，每个技术领域一个子模块，可独立编译运行
- 🔗 **文档与代码关联**：文档中通过链接引用代码，保持文档简洁的同时代码可实际执行
- 🚀 **自动部署**：GitHub Actions 自动构建并部署到 GitHub Pages

### 知识体系覆盖

涵盖 Java 基础、Java 进阶、JVM、并发编程、设计模式、网络与协议、Spring Boot、Spring Cloud、Redis、消息队列、Elasticsearch、数据库（MySQL）、Nginx、配置中心、注册中心、分布式系统理论、Linux 运维、数据结构与算法、Docker 与 Kubernetes、AI 应用等 20+ 技术领域。此外，项目还包含一个 **Spring Cloud 实战项目**（4 个微服务模块），一键启动即可体验 Spring Cloud 全家桶。

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Node.js 18+（用于 VitePress 站点）
- pnpm 8+
- Docker & Docker Compose（可选，用于启动中间件依赖）

### 1. 克隆项目

```bash
git clone https://github.com/your-username/java-knowledge-base.git
cd java-knowledge-base
```

### 2. 编译代码示例

```bash
cd code-examples
mvn compile
```

### 3. 启动文档站点（本地预览）

```bash
cd docs
pnpm install
pnpm run dev
```

### 4. 启动中间件依赖（可选）

#### 首次启动（会自动拉取镜像，耗时较长）

```bash
# 启动基础中间件（Redis、MySQL）
docker compose -f docker/docker-compose.yml up -d

# 按需启动其他中间件
docker compose -f docker/docker-compose.mq.yml up -d       # RabbitMQ、Kafka
docker compose -f docker/docker-compose.es.yml up -d       # Elasticsearch
docker compose -f docker/docker-compose.consul.yml up -d   # Consul
docker compose -f docker/docker-compose.apollo.yml up -d   # Apollo
docker compose -f docker/docker-compose.nginx.yml up -d    # Nginx
```

#### 后续启动（镜像已存在，秒级启动）

```bash
# 启动全部已创建的容器
docker compose -f docker/docker-compose.yml start

# 启动指定服务
docker compose -f docker/docker-compose.yml start redis
docker compose -f docker/docker-compose.yml start mysql
```

#### 常用管理命令

```bash
# 查看运行中的容器
docker compose -f docker/docker-compose.yml ps

# 停止服务（不删除容器和数据）
docker compose -f docker/docker-compose.yml stop

# 停止并删除容器（数据卷保留）
docker compose -f docker/docker-compose.yml down

# 停止并删除容器 + 数据卷（慎用，会丢数据）
docker compose -f docker/docker-compose.yml down -v

# 查看服务日志
docker compose -f docker/docker-compose.yml logs -f redis
```

### 5. Spring Cloud 实战项目（可选）

一键启动体验 Spring Cloud 全家桶（注册发现、Feign 调用、熔断降级、网关、消息队列、缓存、数据库、搜索、文件存储等）：

```bash
# 启动全部中间件
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.consul.yml up -d
docker compose -f docker/docker-compose.mq.yml up -d
docker compose -f docker/docker-compose.es.yml up -d

# 启动业务服务（端口 8090）
cd code-examples/02-framework/springcloud-examples
mvn spring-boot:run

# 可选：启动网关（端口 8080）
cd code-examples/02-framework/springcloud-gateway
mvn spring-boot:run

# 可选：启动分库分表服务（端口 8091）
cd code-examples/02-framework/springcloud-sharding
mvn spring-boot:run

# 可选：启动用户服务（端口 8092）
cd code-examples/02-framework/springcloud-user-service
mvn spring-boot:run

# 验证
curl http://localhost:8090/demo/registry/services
curl http://localhost:8090/demo/cache/set?key=test&value=hello
curl http://localhost:8090/actuator/health
```

支持 Profile 切换中间件实现：
```bash
# 切换注册中心为 Nacos
mvn spring-boot:run -Dspring-boot.run.profiles=nacos

# 切换注册中心为 ZooKeeper
mvn spring-boot:run -Dspring-boot.run.profiles=zk
```

## 目录结构

```
java-knowledge-base/
├── README.md                        # 项目说明（本文件）
├── docs/                            # Markdown 知识文档（VitePress 站点）
│   ├── .vitepress/                  # VitePress 配置
│   │   ├── config.mts               # 主配置
│   │   └── sidebar.mts              # 侧边栏配置
│   ├── index.md                     # 首页
│   ├── guide/                       # 使用指南
│   ├── 1-java-core/                 # Java 核心模块
│   │   ├── 1.1-java-basics/        #   Java 基础
│   │   ├── 1.2-java-advanced/      #   Java 进阶
│   │   ├── 1.3-concurrent/         #   并发编程
│   │   ├── 1.4-jvm/                #   JVM
│   │   ├── 1.5-design-patterns/    #   设计模式
│   │   └── 1.6-algorithm/          #   数据结构与算法
│   ├── 2-framework/                 # 框架层模块
│   │   ├── 2.1-network/            #   网络与协议
│   │   ├── 2.2-springboot/         #   Spring Boot
│   │   └── 2.3-springcloud/        #   Spring Cloud
│   ├── 3-data-store/                # 数据存储模块
│   │   ├── 3.1-database/           #   MySQL 数据库
│   │   ├── 3.2-redis/              #   Redis
│   │   ├── 3.3-elasticsearch/      #   Elasticsearch
│   │   ├── 3.4-mongodb/            #   MongoDB
│   │   └── 3.5-minio/              #   MinIO
│   ├── 4-middleware/                # 中间件模块
│   │   ├── 4.1-mq-rabbitmq/        #   RabbitMQ
│   │   ├── 4.2-mq-kafka/           #   Kafka
│   │   ├── 4.3-mq-mqtt/            #   MQTT
│   │   ├── 4.4-config-center/      #   配置中心
│   │   ├── 4.5-registry/           #   注册中心
│   │   └── 4.6-nginx/              #   Nginx
│   ├── 5-distributed/               # 分布式模块
│   │   └── 5.1-distributed/        #   分布式系统理论
│   ├── 6-devops/                    # DevOps 模块
│   │   ├── 6.1-docker-k8s/         #   Docker 与 K8s
│   │   ├── 6.2-cicd/               #   CI/CD
│   │   ├── 6.3-monitoring/         #   监控体系
│   │   └── 6.4-linux/              #   Linux 运维
│   ├── 7-ai/                        # AI 模块
│   │   └── 7.1-ai/                 #   AI 应用
│   ├── 8-architecture/              # 架构设计场景
│   ├── learning-paths/              # 学习路径
│   ├── interview/                   # 面试汇总
│   └── templates/                   # 文档模板
│       └── entry-template.md        # 知识条目模板
├── code-examples/                   # Maven 多模块代码项目（分组结构）
│   ├── pom.xml                      # 根 POM（Java 21, Spring Boot 3.2.5）
│   ├── 01-java-core/               # Java 核心分组
│   │   ├── java-basics/             #   Java 基础示例
│   │   ├── java-advanced/           #   Java 进阶示例
│   │   ├── concurrent-programming/  #   并发编程示例
│   │   ├── jvm-deep-dive/           #   JVM 示例
│   │   └── design-patterns/         #   设计模式示例
│   ├── 02-framework/                # 框架层分组
│   │   ├── springboot-examples/     #   Spring Boot 示例
│   │   ├── springcloud-examples/    #   Spring Cloud 示例
│   │   ├── springcloud-gateway/         #   Spring Cloud Gateway 网关（WebFlux）
│   │   ├── springcloud-sharding/        #   分库分表服务（ShardingSphere）
│   │   ├── springcloud-user-service/    #   用户微服务（Feign 被调用方）
│   │   └── network-programming/     #   网络编程示例
│   ├── 03-data-store/               # 数据存储分组
│   │   ├── database-examples/       #   MySQL（索引/事务/分库分表/Binlog）
│   │   ├── redis-examples/          #   Redis（数据结构/缓存/分布式锁）
│   │   ├── elasticsearch-examples/  #   Elasticsearch（倒排索引/DSL/聚合）
│   │   ├── mongodb-examples/        #   MongoDB（文档模型/聚合管道）
│   │   └── minio-examples/          #   MinIO（对象存储/分片上传）
│   ├── 04-middleware/               # 中间件分组
│   │   ├── mq-rabbitmq-examples/    #   RabbitMQ 消息队列
│   │   ├── mq-kafka-examples/       #   Kafka 消息队列
│   │   ├── mq-mqtt-examples/        #   MQTT 物联网消息协议
│   │   ├── config-center-examples/  #   配置中心（Apollo/Nacos Config）
│   │   ├── registry-examples/       #   注册中心（Consul/Nacos/Zookeeper/Eureka）
│   │   └── nginx-examples/          #   Nginx 配置示例（无 POM）
│   ├── 05-distributed/              # 分布式分组
│   │   └── distributed-examples/    #   分布式示例
│   ├── 06-devops/                   # DevOps 运维分组
│   │   ├── docker-k8s-examples/     #   Docker/K8s（容器化/编排/部署策略）
│   │   ├── cicd-examples/           #   CI/CD（Jenkins/GitHub Actions/GitLab CI）
│   │   └── monitoring-examples/     #   监控（Prometheus/Grafana/Micrometer）
│   └── 07-ai/                       # AI 应用分组
│       └── ai-examples/             #   AI 应用示例
├── .github/                         # GitHub 配置
│   └── workflows/                   # GitHub Actions 工作流
│       └── deploy.yml               # 自动构建 + 部署
└── docker/                          # Docker Compose 配置
    ├── docker-compose.yml           # 基础中间件（Redis、MySQL）
    ├── docker-compose.mq.yml        # 消息队列（RabbitMQ、Kafka）
    ├── docker-compose.es.yml        # Elasticsearch
    ├── docker-compose.consul.yml    # Consul
    ├── docker-compose.apollo.yml    # Apollo 配置中心
    └── docker-compose.nginx.yml     # Nginx
```

## 学习架构（五层递进）

知识模块按照认知规律组织，遵循从基础到进阶的五层学习架构：

| 层级 | 阶段 | 模块 | 说明 |
|------|------|------|------|
| 第一层：语言基础 | 阶段 1 | Java 基础、Java 进阶、并发编程、JVM、设计模式、算法 | 打牢 Java 语言根基 |
| 第二层：框架应用 | 阶段 2 | 网络与协议、Spring Boot、Spring Cloud 全家桶 | 掌握主流框架 |
| 第三层：数据存储 | 阶段 3 | MySQL、Redis、Elasticsearch、MongoDB、MinIO | 数据存储技术 |
| 第四层：中间件 | 阶段 4 | RabbitMQ、Kafka、MQTT、配置中心、注册中心、Nginx | 中间件技术 |
| 第五层：综合进阶 | 阶段 5~8 | 分布式理论、DevOps、AI、架构设计 | 综合运用 |

## 内容完成度追踪

> 模块顺序与 project-plan.md 的阶段编号对应

| 阶段 | 模块 | 状态 | 文档 | 代码示例 | 面试指南 |
|------|------|------|------|----------|----------|
| 1.1 | Java 基础 | ✅ 已完成 | 13/13 | 11/11 | ✅ |
| 1.2 | Java 进阶 | ✅ 已完成 | 8/8 | 8/8 | ✅ |
| 1.3 | 并发编程 | ✅ 已完成 | 11/11 | 11/11 | ✅ |
| 1.4 | JVM | ✅ 已完成 | 7/7 | 7/7 | ✅ |
| 1.5 | 设计模式 | ✅ 已完成 | 6/6 | 6/6 | ✅ |
| 1.6 | 数据结构与算法 | ✅ 已完成 | 11/11 | 11/11 | ✅ |
| 2.1 | 网络与协议 | ✅ 已完成 | 8/8 | 8/8 | ✅ |
| 2.2 | Spring Boot | ✅ 已完成 | 14/14 | 14/14 | ✅ |
| 2.3 | Spring Cloud | ✅ 已完成 | 10/10 | 10/10 | ✅ |
| 2.3 | Spring Cloud 实战项目 | ✅ 已完成 | 18 篇文档已更新 | 4 个微服务模块 | — |
| 3.1 | 数据库/MySQL | ✅ 已完成 | 11/11 | 11/11 | ✅ |
| 3.2 | Redis | ✅ 已完成 | 7/7 | 7/7 | ✅ |
| 3.3 | Elasticsearch | ✅ 已完成 | 7/7 | 7/7 | ✅ |
| 3.4 | MongoDB | ✅ 已完成 | 6/6 | 6/6 | ✅ |
| 3.5 | MinIO | ✅ 已完成 | 4/4 | 4/4 | ✅ |
| 4.1 | 消息队列（RabbitMQ） | ✅ 已完成 | 5/5 | 5/5 | ✅ |
| 4.2 | 消息队列（Kafka） | ✅ 已完成 | 5/5 | 5/5 | ✅ |
| 4.3 | 消息队列（MQTT） | ✅ 已完成 | 4/4 | 4/4 | ✅ |
| 4.4 | 配置中心 | ✅ 已完成 | 5/5 | 5/5 | ✅ |
| 4.5 | 注册中心 | ✅ 已完成 | 6/6 | 6/6 | ✅ |
| 4.6 | Nginx | ✅ 已完成 | 8/8 | 8/8 | ✅ |
| 5.1 | 分布式系统理论 | ✅ 已完成 | 7/7 | 7/7 | ✅ |
| 6.1 | Docker 与 K8s | ✅ 已完成 | 13/13 | 13/13 | ✅ |
| 6.2 | CI/CD | ✅ 已完成 | 5/5 | 5/5 | ✅ |
| 6.3 | 监控体系 | ✅ 已完成 | 5/5 | 5/5 | ✅ |
| 6.4 | Linux 运维 | ✅ 已完成 | 6/6 | — | ✅ |
| 7.1 | AI 应用 | ✅ 已完成 | 8/8 | 8/8 | ✅ |
| 8.1 | 架构设计场景 | ✅ 已完成 | 9/9 | 9/9 | ✅ |
| 9.1 | 学习路径 | ✅ 已完成 | 5/5 | — | — |

> 状态说明：🔲 未开始 | 🔶 进行中 | ✅ 已完成

## 文档与代码对应规则

- 文档位置：`docs/{分组名}/{模块名}/{知识点}.md`（如 `docs/1-java-core/1.1-java-basics/01-data-types.md`）
- 代码位置：`code-examples/{分组名}/{子模块名}/`
- 文档中通过链接引用代码示例，每个知识点都有对应的可运行代码

## 许可证

本项目仅供个人学习使用。

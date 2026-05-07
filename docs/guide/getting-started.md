---
title: 快速开始
---

# 快速开始

## 环境要求

| 工具 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 17+ | 运行代码示例 |
| Maven | 3.8+ | 编译代码项目 |
| Node.js | 18+ | 构建文档站点 |
| pnpm | 9+ | 包管理器 |
| Docker | 20+ | 启动中间件环境（可选） |

## 克隆项目

```bash
git clone https://github.com/your-repo/java-knowledge-base.git
cd java-knowledge-base
```

## 浏览文档站点

```bash
# 安装依赖
cd docs
pnpm install

# 启动本地开发服务器
pnpm run dev
```

浏览器访问 `http://localhost:5173` 即可查看知识库站点。

## 运行代码示例

```bash
# 编译所有子模块
cd code-examples
mvn compile

# 运行某个示例的测试
mvn test -pl java-basics

# 运行某个具体的 main 方法
mvn exec:java -pl java-basics -Dexec.mainClass="com.example.basics.collections.HashMapDemo"
```

## 启动中间件（可选）

部分代码示例依赖外部服务（Redis、MySQL、Kafka 等），可通过 Docker Compose 一键启动：

```bash
# 基础中间件（Redis + MySQL）
docker compose -f docker/docker-compose.yml up -d

# 消息队列（RabbitMQ + Kafka）
docker compose -f docker/docker-compose.mq.yml up -d

# Elasticsearch
docker compose -f docker/docker-compose.es.yml up -d
```

## 项目结构

```
java-knowledge-base/
├── docs/                            # Markdown 知识文档 + VitePress 站点
│   ├── .vitepress/                  # VitePress 配置
│   ├── 1-java-core/                 # Java 核心模块
│   │   ├── 1.1-java-basics/        #   Java 基础
│   │   ├── 1.2-java-advanced/      #   Java 进阶
│   │   ├── 1.3-concurrent/         #   并发编程
│   │   ├── 1.4-jvm/                #   JVM
│   │   ├── 1.5-design-patterns/    #   设计模式
│   │   └── 1.6-algorithm/          #   数据结构与算法
│   ├── guide/                       # 使用指南
│   ├── learning-paths/              # 学习路径
│   ├── interview/                   # 面试汇总
│   └── templates/                   # 文档模板
├── code-examples/                   # Maven 多模块代码项目
│   ├── pom.xml                      # 父 POM
│   ├── 01-java-core/               # Java 核心代码示例
│   ├── 02-framework/               # 框架层代码示例
│   └── ...                          # 其他分组
├── docker/                          # Docker Compose 配置
└── .github/                         # GitHub Actions CI/CD
```

## 下一步

- 📖 阅读 [使用指南](/guide/how-to-use) 了解知识库的组织方式
- 🟢 从 [Java 基础](/1-java-core/1.1-java-basics/01-data-types) 开始学习
- 🎯 查看 [学习路径](/learning-paths/beginner) 选择适合你的路线

---
title: "分布式缓存方案设计"
module: "architecture"
difficulty: "advanced"
interviewFrequency: "high"
tags:
  - "缓存"
  - "Redis"
  - "架构设计"
codeExample: ""
relatedEntries:
  - "/3-data-store/3.2-redis/04-cache-problems"
  - "/8-architecture/08-cache-db-consistency"
prerequisites:
  - "/3-data-store/3.2-redis/01-data-structures"
estimatedTime: "45min"
---

# 分布式缓存方案设计

## 问题分析

分布式缓存是提升系统性能的关键手段，但引入缓存也带来了一致性、可用性等挑战。

## 缓存策略对比

| 策略 | 读流程 | 写流程 | 一致性 | 适用场景 |
|------|--------|--------|--------|----------|
| Cache Aside | 先读缓存，未命中读 DB 写缓存 | 先更新 DB，再删缓存 | 最终一致 | 通用场景（推荐） |
| Read/Write Through | 缓存层代理读写 | 缓存层同步写 DB | 强一致 | 缓存中间件支持 |
| Write Behind | 同 Read Through | 缓存层异步写 DB | 弱一致 | 高写入场景 |

## 推荐方案详解

### Cache Aside 模式

```mermaid
sequenceDiagram
    participant App as 应用
    participant Cache as Redis
    participant DB as MySQL

    Note over App: 读流程
    App->>Cache: 1. 查询缓存
    alt 缓存命中
        Cache-->>App: 返回数据
    else 缓存未命中
        App->>DB: 2. 查询数据库
        DB-->>App: 返回数据
        App->>Cache: 3. 写入缓存（设置 TTL）
    end

    Note over App: 写流程
    App->>DB: 1. 更新数据库
    App->>Cache: 2. 删除缓存
```

### 缓存三大问题

```mermaid
graph TD
    A["缓存问题"] --> B["缓存穿透"]
    A --> C["缓存击穿"]
    A --> D["缓存雪崩"]

    B --> B1["查询不存在的数据"]
    B --> B2["解决: 布隆过滤器 + 空值缓存"]

    C --> C1["热点 Key 过期"]
    C --> C2["解决: 互斥锁 + 逻辑过期"]

    D --> D1["大量 Key 同时过期"]
    D --> D2["解决: 随机 TTL + 多级缓存"]

    style B fill:#fce4ec
    style C fill:#fff3e0
    style D fill:#e8f5e9
```

### 多级缓存架构

```mermaid
graph LR
    A["请求"] --> B["本地缓存<br/>Caffeine"]
    B -->|未命中| C["分布式缓存<br/>Redis"]
    C -->|未命中| D["数据库<br/>MySQL"]

    style B fill:#e8f5e9
    style C fill:#fff3e0
```

## 常见追问

### Q: 为什么是删除缓存而不是更新缓存？
删除缓存更简单且安全。更新缓存在并发场景下可能导致数据不一致（两个线程同时更新，后更新 DB 的先更新了缓存）。删除缓存让下次读取时重新加载最新数据。

### Q: 缓存穿透如何解决？
布隆过滤器拦截不存在的 Key；缓存空值（TTL 较短，如 5 分钟）；接口层参数校验。

### Q: 热点 Key 如何处理？
互斥锁（只允许一个线程重建缓存）；逻辑过期（不设置 TTL，由后台线程异步更新）；本地缓存（Caffeine）减少 Redis 压力。

## 参考资料

- [缓存更新策略](https://coolshell.cn/articles/17416.html)

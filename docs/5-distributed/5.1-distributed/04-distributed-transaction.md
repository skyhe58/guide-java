---
title: "分布式事务"
module: "distributed"
difficulty: "advanced"
interviewFrequency: "high"
tags:
  - "分布式系统"
  - "分布式事务"
  - "2PC"
  - "TCC"
  - "Saga"
  - "Seata"
  - "面试高频"
codeExample: "05-distributed/distributed-examples/src/main/java/com/example/distributed/transaction/DistributedTransactionDemo.java"
relatedEntries:
  - "/2-framework/2.3-springcloud/08-transaction"
  - "/5-distributed/5.1-distributed/01-cap-base"
prerequisites:
  - "/3-data-store/3.1-database/02-transaction"
  - "/5-distributed/5.1-distributed/01-cap-base"
estimatedTime: "60min"
---

# 分布式事务

## 概念说明

分布式事务解决的是**跨多个服务/数据库的数据一致性问题**。在微服务架构中，一个业务操作可能涉及多个服务（如下单 = 扣库存 + 创建订单 + 扣余额），每个服务有自己的数据库，无法使用本地事务保证一致性。

## 核心原理

### 一、2PC（两阶段提交）

2PC 是最经典的分布式事务协议，由协调者（Coordinator）和参与者（Participant）组成。

```mermaid
sequenceDiagram
    participant TC as 协调者（TC）
    participant P1 as 参与者1（订单服务）
    participant P2 as 参与者2（库存服务）

    Note over TC,P2: 阶段一：Prepare（准备）

    TC->>P1: 准备提交？
    P1->>P1: 执行事务，写 Undo/Redo 日志
    P1-->>TC: YES（准备就绪）

    TC->>P2: 准备提交？
    P2->>P2: 执行事务，写 Undo/Redo 日志
    P2-->>TC: YES（准备就绪）

    Note over TC,P2: 阶段二：Commit（提交）

    TC->>P1: 提交
    P1->>P1: 正式提交事务
    P1-->>TC: ACK

    TC->>P2: 提交
    P2->>P2: 正式提交事务
    P2-->>TC: ACK
```

**2PC 的问题**：

| 问题 | 说明 |
|------|------|
| 同步阻塞 | 参与者在 Prepare 后必须等待协调者指令，期间资源被锁定 |
| 单点故障 | 协调者宕机，参与者一直阻塞 |
| 数据不一致 | 阶段二如果部分参与者没收到 Commit，数据不一致 |
| 性能差 | 两轮网络通信 + 资源锁定时间长 |

### 二、3PC（三阶段提交）

3PC 在 2PC 基础上增加了 CanCommit 阶段和超时机制：

```mermaid
graph LR
    A["CanCommit<br/>（询问能否提交）"] --> B["PreCommit<br/>（预提交）"]
    B --> C["DoCommit<br/>（正式提交）"]

    A1["超时 → 中止"] -.-> A
    B1["超时 → 中止"] -.-> B
    C1["超时 → 提交"] -.-> C
```

**3PC vs 2PC**：3PC 引入超时机制减少阻塞，但仍无法完全解决数据不一致问题，实际应用较少。

### 三、TCC（Try-Confirm-Cancel）

TCC 是一种业务层面的补偿事务方案，将事务拆分为三个阶段：

```mermaid
sequenceDiagram
    participant App as 业务应用
    participant Order as 订单服务
    participant Stock as 库存服务
    participant Account as 账户服务

    Note over App,Account: 阶段一：Try（资源预留）

    App->>Order: Try: 创建订单（待确认状态）
    App->>Stock: Try: 冻结库存（stock-1, frozen+1）
    App->>Account: Try: 冻结余额（balance-100, frozen+100）

    alt 所有 Try 成功
        Note over App,Account: 阶段二：Confirm（确认提交）
        App->>Order: Confirm: 订单状态 → 已确认
        App->>Stock: Confirm: frozen-1（释放冻结）
        App->>Account: Confirm: frozen-100（释放冻结）
    else 任一 Try 失败
        Note over App,Account: 阶段三：Cancel（取消回滚）
        App->>Order: Cancel: 删除订单
        App->>Stock: Cancel: stock+1, frozen-1（恢复库存）
        App->>Account: Cancel: balance+100, frozen-100（恢复余额）
    end
```

**TCC 的关键设计**：

| 阶段 | 职责 | 要求 |
|------|------|------|
| Try | 资源检查和预留 | 不做真正的业务操作，只冻结资源 |
| Confirm | 确认提交 | 必须幂等，可能被重复调用 |
| Cancel | 取消回滚 | 必须幂等，释放 Try 阶段预留的资源 |

**TCC 的优缺点**：
- ✅ 不依赖数据库事务，性能好
- ✅ 每个阶段都是本地事务，不会长时间锁资源
- ❌ 业务侵入性强，每个服务都要实现 Try/Confirm/Cancel
- ❌ 开发成本高，需要考虑幂等、空回滚、悬挂等问题

### 四、Saga 模式

Saga 将长事务拆分为多个本地事务，每个本地事务有对应的补偿事务。

#### 编排模式（Choreography）

```mermaid
graph LR
    A["订单服务<br/>创建订单"] -->|事件| B["库存服务<br/>扣减库存"]
    B -->|事件| C["账户服务<br/>扣减余额"]
    C -->|失败| C1["账户服务<br/>恢复余额"]
    C1 -->|补偿事件| B1["库存服务<br/>恢复库存"]
    B1 -->|补偿事件| A1["订单服务<br/>取消订单"]

    style A fill:#e8f5e9
    style B fill:#e8f5e9
    style C fill:#fce4ec
    style C1 fill:#fff3e0
    style B1 fill:#fff3e0
    style A1 fill:#fff3e0
```

#### 协调模式（Orchestration）

```mermaid
graph TD
    O["Saga 协调器"] --> A["订单服务：创建订单"]
    O --> B["库存服务：扣减库存"]
    O --> C["账户服务：扣减余额"]

    A -.->|失败补偿| A1["订单服务：取消订单"]
    B -.->|失败补偿| B1["库存服务：恢复库存"]
    C -.->|失败补偿| C1["账户服务：恢复余额"]

    style O fill:#e1f5fe
```

| 模式 | 优点 | 缺点 |
|------|------|------|
| 编排模式 | 去中心化，服务间松耦合 | 流程分散，难以追踪和调试 |
| 协调模式 | 流程集中管理，易于理解 | 协调器是单点，逻辑集中 |

### 五、消息最终一致性

通过消息队列实现最终一致性，是最常用的分布式事务方案之一。

```mermaid
sequenceDiagram
    participant Order as 订单服务
    participant DB as 订单数据库
    participant MQ as 消息队列
    participant Stock as 库存服务

    Order->>DB: 1. 开启本地事务
    Order->>DB: 2. 创建订单
    Order->>DB: 3. 写入消息表（同一事务）
    Order->>DB: 4. 提交事务

    Note over Order,MQ: 定时任务扫描消息表

    Order->>MQ: 5. 发送消息（扣减库存）
    MQ-->>Stock: 6. 消费消息
    Stock->>Stock: 7. 扣减库存（幂等处理）
    Stock-->>MQ: 8. ACK 确认

    Order->>DB: 9. 更新消息状态为已发送
```

**关键设计**：
- 本地消息表：订单和消息在同一个事务中写入，保证原子性
- 定时任务：扫描未发送的消息，重试发送
- 消费端幂等：消费者必须做幂等处理，防止重复消费

### 六、Seata 框架

Seata 是阿里开源的分布式事务框架，支持 AT、TCC、Saga、XA 四种模式。

#### AT 模式（推荐）

```mermaid
sequenceDiagram
    participant TM as TM（事务管理器）
    participant TC as TC（事务协调器）
    participant RM1 as RM1（订单服务）
    participant RM2 as RM2（库存服务）

    TM->>TC: 1. 开启全局事务（获取 XID）

    TM->>RM1: 2. 调用订单服务
    RM1->>RM1: 执行 SQL，记录 Before/After Image
    RM1->>TC: 注册分支事务

    TM->>RM2: 3. 调用库存服务
    RM2->>RM2: 执行 SQL，记录 Before/After Image
    RM2->>TC: 注册分支事务

    alt 所有分支成功
        TM->>TC: 4. 全局提交
        TC->>RM1: 异步删除 Undo Log
        TC->>RM2: 异步删除 Undo Log
    else 任一分支失败
        TM->>TC: 4. 全局回滚
        TC->>RM1: 根据 Before Image 回滚
        TC->>RM2: 根据 Before Image 回滚
    end
```

**AT 模式特点**：
- 对业务无侵入，只需加 `@GlobalTransactional` 注解
- 通过 Undo Log 实现自动回滚
- 性能优于 2PC（一阶段直接提交本地事务）

### 七、方案对比总结

| 方案 | 一致性 | 性能 | 业务侵入 | 适用场景 |
|------|--------|------|----------|----------|
| 2PC/XA | 强一致 | ⭐ | 无 | 传统数据库分布式事务 |
| TCC | 最终一致 | ⭐⭐⭐⭐ | 强 | 资金类高一致性要求 |
| Saga | 最终一致 | ⭐⭐⭐ | 中 | 长事务、跨多服务 |
| 消息最终一致 | 最终一致 | ⭐⭐⭐⭐⭐ | 弱 | 异步场景、最常用 |
| Seata AT | 最终一致 | ⭐⭐⭐⭐ | 无 | Spring Cloud 微服务 |

```mermaid
graph TD
    A{"选择分布式事务方案"} --> B{"一致性要求？"}
    B -->|强一致| C{"性能要求？"}
    B -->|最终一致| D{"业务复杂度？"}
    C -->|高| E["TCC"]
    C -->|低| F["2PC/XA"]
    D -->|简单| G["消息最终一致性（推荐）"]
    D -->|复杂| H{"是否用 Spring Cloud？"}
    H -->|是| I["Seata AT（推荐）"]
    H -->|否| J["Saga"]

    style G fill:#e8f5e9
    style I fill:#e8f5e9
```

## 代码示例

> 💻 完整可运行代码：[DistributedTransactionDemo.java](../../../code-examples/05-distributed/distributed-examples/src/main/java/com/example/distributed/transaction/DistributedTransactionDemo.java)

## 常见面试题

### Q1: 分布式事务有哪些方案？各自的优缺点？

**难度**：⭐⭐⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 列举主要方案：2PC、TCC、Saga、消息最终一致性、Seata
2. 从一致性、性能、侵入性三个维度对比
3. 给出选型建议

**标准答案**：

分布式事务主要有五种方案：1）2PC 两阶段提交，强一致但性能差、有阻塞问题；2）TCC 补偿事务，性能好但业务侵入强，需要实现 Try/Confirm/Cancel 三个接口；3）Saga 模式，适合长事务，每个步骤有对应的补偿操作；4）消息最终一致性，通过本地消息表 + 消息队列实现，性能最好，是最常用的方案；5）Seata AT 模式，对业务无侵入，通过 Undo Log 自动回滚。选型建议：一般场景用消息最终一致性，Spring Cloud 微服务用 Seata AT，资金类场景用 TCC。

**深入追问**：

- TCC 的空回滚和悬挂问题是什么？如何解决？
- Seata AT 模式的原理是什么？和 XA 有什么区别？
- 消息最终一致性如何保证消息一定能发出去？（本地消息表 + 定时重试）

**易错点**：

- 把 2PC 和 TCC 混淆（2PC 是协议层面，TCC 是业务层面）
- 忘记提到消息最终一致性方案（这是实际最常用的）

### Q2: 如何保证消息最终一致性？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 说明本地消息表方案
2. 描述完整流程
3. 强调幂等性

**标准答案**：

消息最终一致性通过本地消息表实现：业务操作和消息写入在同一个本地事务中完成，保证原子性。然后通过定时任务扫描消息表，将未发送的消息发送到 MQ。消费端消费消息后执行业务操作，必须做幂等处理（通过唯一 ID 去重）。如果消费失败，MQ 会重试投递。整个过程保证了最终一致性。也可以使用 RocketMQ 的事务消息，原理类似但不需要自己维护消息表。

## 参考资料

- [Seata 官方文档](https://seata.io/zh-cn/)
- [分布式事务方案对比](https://www.infoq.cn/article/2018/08/distributed-transaction)

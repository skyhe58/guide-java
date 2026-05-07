---
title: "CompletableFuture 异步编程"
module: "concurrent"
difficulty: "intermediate"
interviewFrequency: "medium"
tags:
  - "并发编程"
  - "CompletableFuture"
  - "异步编程"
  - "面试高频"
codeExample: "01-java-core/concurrent-programming/src/main/java/com/example/concurrent/future/"
relatedEntries:
  - "/1-java-core/1.3-concurrent/05-thread-pool"
  - "/1-java-core/1.3-concurrent/06-concurrent-tools"
prerequisites:
  - "/1-java-core/1.3-concurrent/05-thread-pool"
  - "/1-java-core/1.1-java-basics/11-lambda-stream"
estimatedTime: "40min"
---

# CompletableFuture 异步编程

## 概念说明

`CompletableFuture` 是 JDK 8 引入的异步编程工具，它实现了 `Future` 和 `CompletionStage` 接口，支持链式调用、组合多个异步任务、异常处理等功能。相比传统的 Future，CompletableFuture 不需要阻塞等待结果，可以通过回调方式处理。

## 核心原理

### 一、创建异步任务

| 方法 | 返回值 | 线程池 |
|------|--------|--------|
| `supplyAsync(Supplier)` | 有返回值 | ForkJoinPool.commonPool() |
| `supplyAsync(Supplier, Executor)` | 有返回值 | 自定义线程池 |
| `runAsync(Runnable)` | 无返回值 | ForkJoinPool.commonPool() |
| `runAsync(Runnable, Executor)` | 无返回值 | 自定义线程池 |

> ⚠️ **生产建议**：不要使用默认的 ForkJoinPool.commonPool()，应该传入自定义线程池，便于监控和隔离。

### 二、核心 API 分类

```mermaid
flowchart TD
    A["CompletableFuture API"] --> B["转换类"]
    A --> C["组合类"]
    A --> D["消费类"]
    A --> E["异常处理"]
    A --> F["多任务组合"]

    B --> B1["thenApply — 转换结果"]
    B --> B2["thenCompose — 扁平化（类似 flatMap）"]

    C --> C1["thenCombine — 合并两个结果"]
    C --> C2["thenAcceptBoth — 消费两个结果"]

    D --> D1["thenAccept — 消费结果"]
    D --> D2["thenRun — 不关心结果，执行动作"]

    E --> E1["exceptionally — 异常恢复"]
    E --> E2["handle — 处理结果或异常"]
    E --> E3["whenComplete — 完成时回调"]

    F --> F1["allOf — 等待所有完成"]
    F --> F2["anyOf — 任一完成即返回"]
```

### 三、thenApply vs thenCompose

```java
// thenApply：同步转换，类似 Stream.map()
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> getUserId())
    .thenApply(id -> "User-" + id);  // 返回 CompletableFuture<String>

// thenCompose：异步转换，类似 Stream.flatMap()
CompletableFuture<User> future = CompletableFuture
    .supplyAsync(() -> getUserId())
    .thenCompose(id -> queryUserAsync(id));  // 返回 CompletableFuture<User>
```

### 四、异常处理

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (error) throw new RuntimeException("出错了");
        return "success";
    })
    .exceptionally(ex -> "默认值")        // 异常时返回默认值
    .handle((result, ex) -> {             // 统一处理结果和异常
        if (ex != null) return "error";
        return result;
    })
    .whenComplete((result, ex) -> {       // 完成时回调（不改变结果）
        log.info("完成: result={}, error={}", result, ex);
    });
```

### 五、实战场景：多接口并行调用

```mermaid
sequenceDiagram
    participant Main as 主线程
    participant F1 as 用户服务
    participant F2 as 订单服务
    participant F3 as 商品服务

    Main->>F1: supplyAsync(查询用户)
    Main->>F2: supplyAsync(查询订单)
    Main->>F3: supplyAsync(查询商品)

    F1-->>Main: 用户信息（200ms）
    F2-->>Main: 订单列表（300ms）
    F3-->>Main: 商品详情（150ms）

    Main->>Main: allOf().join()
    Note over Main: 总耗时 ≈ 300ms（取最长）<br/>而非 650ms（串行）
```

## 代码示例

```java
// 多任务并行 + 结果合并
ExecutorService executor = Executors.newFixedThreadPool(3);

CompletableFuture<User> userFuture = CompletableFuture
    .supplyAsync(() -> userService.getUser(userId), executor);
CompletableFuture<List<Order>> orderFuture = CompletableFuture
    .supplyAsync(() -> orderService.getOrders(userId), executor);

CompletableFuture<UserDetail> result = userFuture
    .thenCombine(orderFuture, (user, orders) -> new UserDetail(user, orders));
```

> 💻 完整可运行代码：[CompletableFutureDemo.java](../../../code-examples/01-java-core/concurrent-programming/src/main/java/com/example/concurrent/future/CompletableFutureDemo.java)

## 常见面试题

### Q1: CompletableFuture 和 Future 的区别？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

Future 只能通过 get() 阻塞获取结果或 isDone() 轮询，不支持回调和链式调用。CompletableFuture 支持链式调用（thenApply/thenCompose）、多任务组合（allOf/anyOf）、异常处理（exceptionally/handle）、回调通知（whenComplete），是真正的异步编程工具。

**深入追问**：

- CompletableFuture 默认使用什么线程池？（ForkJoinPool.commonPool()）
- 为什么生产环境不建议用默认线程池？（共享线程池，可能被其他任务影响）

### Q2: thenApply 和 thenCompose 的区别？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

thenApply 接收一个同步函数，将结果转换为另一个值，类似 Stream 的 map()；thenCompose 接收一个返回 CompletableFuture 的函数，将结果扁平化，类似 Stream 的 flatMap()。如果转换函数本身是异步的，应该用 thenCompose 避免嵌套。

### Q3: CompletableFuture 如何处理异常？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

三种方式：exceptionally() 只处理异常，返回默认值；handle() 同时处理正常结果和异常；whenComplete() 完成时回调，不改变结果。推荐使用 handle() 统一处理，或者 exceptionally() 做异常兜底。

## 参考资料

- [CompletableFuture - JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/1-java-core/1.3-concurrent/CompletableFuture.html)

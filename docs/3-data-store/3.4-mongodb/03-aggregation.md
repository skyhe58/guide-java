---
title: "MongoDB 聚合管道与 MapReduce"
module: "mongodb"
difficulty: "advanced"
interviewFrequency: "medium"
tags:
  - "MongoDB"
  - "聚合管道"
  - "数据分析"
codeExample: "03-data-store/mongodb-examples/src/main/java/com/example/mongodb/MongoDBDemo.java"
relatedEntries:
  - "/3-data-store/3.4-mongodb/02-crud"
prerequisites:
  - "/3-data-store/3.4-mongodb/02-crud"
estimatedTime: "50min"
---

# MongoDB 聚合管道与 MapReduce

## 概念说明

MongoDB 的**聚合管道（Aggregation Pipeline）**是一种数据处理框架，将文档通过多个阶段（Stage）依次处理，每个阶段对输入文档进行转换并输出到下一个阶段。类似于 Unix 管道，功能强大且灵活。

## 核心原理

### 聚合管道工作流程

```mermaid
graph LR
    A["原始集合"] --> B["$match<br/>过滤"]
    B --> C["$group<br/>分组"]
    C --> D["$sort<br/>排序"]
    D --> E["$project<br/>投影"]
    E --> F["结果"]

    style A fill:#e1f5fe
    style B fill:#e8f5e9
    style C fill:#fff3e0
    style D fill:#fce4ec
    style E fill:#f3e5f5
    style F fill:#ede7f6
```

### 常用聚合阶段

| 阶段 | 说明 | 类比 SQL |
|------|------|----------|
| `$match` | 过滤文档 | WHERE |
| `$group` | 分组聚合 | GROUP BY |
| `$sort` | 排序 | ORDER BY |
| `$project` | 字段投影/重命名 | SELECT |
| `$limit` / `$skip` | 分页 | LIMIT / OFFSET |
| `$lookup` | 关联查询 | LEFT JOIN |
| `$unwind` | 展开数组 | — |
| `$addFields` | 添加计算字段 | AS |
| `$bucket` | 分桶统计 | — |
| `$facet` | 多管道并行 | — |

### 聚合示例

```javascript
// 统计每个城市的用户数和平均年龄
db.users.aggregate([
  { $match: { status: "active" } },
  { $group: {
      _id: "$address.city",
      count: { $sum: 1 },
      avgAge: { $avg: "$age" },
      maxAge: { $max: "$age" }
  }},
  { $sort: { count: -1 } },
  { $limit: 10 }
])

// $lookup 关联查询（类似 LEFT JOIN）
db.orders.aggregate([
  { $lookup: {
      from: "users",
      localField: "userId",
      foreignField: "_id",
      as: "userInfo"
  }},
  { $unwind: "$userInfo" },
  { $project: {
      orderNo: 1,
      totalAmount: 1,
      "userInfo.name": 1
  }}
])

// $facet 多维度统计
db.products.aggregate([
  { $facet: {
      "byCategory": [
        { $group: { _id: "$category", count: { $sum: 1 } } }
      ],
      "priceRange": [
        { $bucket: {
            groupBy: "$price",
            boundaries: [0, 50, 100, 500],
            default: "500+",
            output: { count: { $sum: 1 } }
        }}
      ]
  }}
])
```

### 聚合管道 vs MapReduce

| 维度 | 聚合管道 | MapReduce |
|------|----------|-----------|
| 性能 | 高（C++ 原生实现） | 低（JavaScript 引擎） |
| 灵活性 | 阶段组合 | 完全自定义 |
| 使用场景 | 大多数聚合需求 | 极复杂的自定义逻辑 |
| 推荐度 | ⭐⭐⭐⭐⭐ | ⭐⭐（已不推荐） |

> MongoDB 5.0 开始已弃用 MapReduce，推荐使用聚合管道替代。

## 代码示例

```java
// 聚合管道概念演示
public static void aggregationDemo() {
    System.out.println("=== 聚合管道阶段 ===");
    System.out.println("$match  → 过滤（WHERE）");
    System.out.println("$group  → 分组（GROUP BY）");
    System.out.println("$sort   → 排序（ORDER BY）");
    System.out.println("$lookup → 关联（LEFT JOIN）");
}
```

> 💻 完整可运行代码：[MongoDBDemo.java](../../../code-examples/03-data-store/mongodb-examples/src/main/java/com/example/mongodb/MongoDBDemo.java)

## 常见面试题

### Q1: MongoDB 的聚合管道和 SQL 的 GROUP BY 有什么区别？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

MongoDB 聚合管道是多阶段流水线处理，比 SQL 的 GROUP BY 更灵活。聚合管道可以在分组前后进行过滤、投影、排序等操作，支持嵌套文档和数组的处理（如 $unwind），还支持 $facet 多维度并行聚合。SQL 的 GROUP BY 是单次分组操作，复杂分析需要子查询或窗口函数。

### Q2: $lookup 的性能如何？有什么优化建议？

**难度**：⭐⭐⭐ | **频率**：🔥🔥

**标准答案**：

$lookup 类似 LEFT JOIN，性能取决于被关联集合的大小和索引。优化建议：为 foreignField 创建索引；在 $lookup 前用 $match 减少文档数量；考虑使用嵌入模式替代频繁的 $lookup；对于大数据量场景，可以使用 pipeline 形式的 $lookup 在关联时添加过滤条件。

### Q3: 聚合管道的执行顺序有什么优化技巧？

**难度**：⭐⭐⭐ | **频率**：🔥🔥

**标准答案**：

将 $match 和 $limit 尽量放在管道前面，减少后续阶段处理的数据量。$match 放在 $group 前可以利用索引。MongoDB 优化器会自动将 $match 前移（如果不影响结果），但显式放在前面更可靠。避免在管道中间使用 $unwind 展开大数组，会导致文档数量膨胀。

## 参考资料

- [MongoDB Aggregation Pipeline](https://www.mongodb.com/docs/manual/core/aggregation-pipeline/)

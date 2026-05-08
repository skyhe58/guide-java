---
title: "SQL 优化"
module: "database"
difficulty: "advanced"
interviewFrequency: "high"
tags:
  - "MySQL"
  - "SQL 优化"
  - "EXPLAIN"
  - "慢查询"
  - "面试高频"
codeExample: "03-data-store/database-examples/src/main/java/com/example/database/optimization/OptimizationDemo.java"
relatedEntries:
  - "/3-data-store/3.1-database/01-index-theory"
  - "/3-data-store/3.1-database/10-pool"
prerequisites:
  - "/3-data-store/3.1-database/01-index-theory"
estimatedTime: "60min"
---

# SQL 优化

## 概念说明

SQL 优化是 MySQL 性能调优的核心环节，也是面试中的高频考点。掌握 EXPLAIN 执行计划的各字段含义、慢查询分析方法和索引优化策略，是每个后端开发者的必备技能。

> 面试核心：EXPLAIN 的 type 字段有哪些值？什么是 Using filesort 和 Using temporary？

## 核心原理

### 一、EXPLAIN 执行计划详解

```sql
EXPLAIN SELECT * FROM user WHERE name = '张三' AND age > 20;
```

| 字段 | 含义 | 重点关注 |
|------|------|----------|
| `id` | 查询序号 | 值越大优先级越高 |
| `select_type` | 查询类型 | SIMPLE/PRIMARY/SUBQUERY/DERIVED |
| `table` | 访问的表 | - |
| `partitions` | 匹配的分区 | - |
| **`type`** | **访问类型** | **最重要！性能从好到差排列** |
| `possible_keys` | 可能使用的索引 | - |
| **`key`** | **实际使用的索引** | NULL 表示未使用索引 |
| `key_len` | 索引使用的字节数 | 越短越好 |
| **`rows`** | **预估扫描行数** | 越少越好 |
| `filtered` | 过滤比例 | 百分比，越高越好 |
| **`Extra`** | **额外信息** | 关注 Using index/filesort/temporary |

### 二、type 字段详解（性能从好到差）

| type | 含义 | 性能 | 示例 |
|------|------|------|------|
| `system` | 表只有一行 | ⭐⭐⭐⭐⭐ | 系统表 |
| `const` | 主键/唯一索引等值查询 | ⭐⭐⭐⭐⭐ | `WHERE id = 1` |
| `eq_ref` | 关联查询，主键/唯一索引 | ⭐⭐⭐⭐ | JOIN 使用主键 |
| `ref` | 非唯一索引等值查询 | ⭐⭐⭐⭐ | `WHERE name = '张三'` |
| `range` | 索引范围查询 | ⭐⭐⭐ | `WHERE age > 20` |
| `index` | 全索引扫描 | ⭐⭐ | 覆盖索引但无 WHERE |
| **`ALL`** | **全表扫描** | ⭐ | **必须优化！** |

> 优化目标：至少达到 `range` 级别，最好达到 `ref` 级别。

### 三、Extra 字段详解

| Extra | 含义 | 是否需要优化 |
|-------|------|-------------|
| `Using index` | 覆盖索引，无需回表 | ✅ 好 |
| `Using where` | Server 层过滤 | ⚠️ 一般 |
| `Using index condition` | 索引下推（ICP） | ✅ 好 |
| **`Using filesort`** | **额外排序（未用索引排序）** | ❌ **需要优化** |
| **`Using temporary`** | **使用临时表** | ❌ **需要优化** |
| `Using join buffer` | 关联查询使用缓冲区 | ⚠️ 关注 |

### 四、慢查询分析

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;  -- 超过 1 秒记录
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 查看慢查询状态
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 使用 mysqldumpslow 分析慢查询日志
-- mysqldumpslow -s t -t 10 /var/log/mysql/slow.log
```

### 五、索引优化策略

#### 索引失效的常见场景

| 场景 | 示例 | 原因 |
|------|------|------|
| 对索引列使用函数 | `WHERE YEAR(create_time) = 2024` | 函数破坏索引有序性 |
| 隐式类型转换 | `WHERE phone = 13800138000`（phone 是 varchar） | 相当于对列加了函数 |
| 左模糊查询 | `WHERE name LIKE '%张'` | 无法利用 B+树有序性 |
| OR 条件 | `WHERE id = 1 OR name = '张三'`（name 无索引） | 有一个条件无索引则全表扫描 |
| 不等于 | `WHERE status != 1` | 优化器可能选择全表扫描 |
| IS NOT NULL | `WHERE name IS NOT NULL` | 取决于数据分布 |
| 联合索引不满足最左前缀 | `WHERE b = 1`（索引是 a,b,c） | 跳过最左列 |

#### 索引设计原则

1. **选择区分度高的列**：`COUNT(DISTINCT col) / COUNT(*)` 越接近 1 越好
2. **联合索引列顺序**：区分度高的列放前面；等值查询的列放前面，范围查询的列放后面
3. **覆盖索引优先**：尽量让查询字段都在索引中
4. **避免冗余索引**：(a, b) 已包含 (a) 的功能
5. **控制索引数量**：单表索引不超过 5-6 个

## 代码示例

```sql
-- 索引失效演示
-- 1. 函数导致索引失效
EXPLAIN SELECT * FROM user WHERE YEAR(create_time) = 2024;
-- 优化：改为范围查询
EXPLAIN SELECT * FROM user WHERE create_time >= '2024-01-01' AND create_time < '2025-01-01';

-- 2. 隐式类型转换
EXPLAIN SELECT * FROM user WHERE phone = 13800138000;  -- phone 是 varchar
-- 优化：使用字符串
EXPLAIN SELECT * FROM user WHERE phone = '13800138000';

-- 3. 覆盖索引优化
EXPLAIN SELECT name, age FROM user WHERE name = '张三';  -- Using index
EXPLAIN SELECT * FROM user WHERE name = '张三';           -- 需要回表
```

> 💻 完整可运行代码：[OptimizationDemo.java](https://github.com/skyhe58/guide-java/tree/main/code-examples/03-data-store/database-examples/src/main/java/com/example/database/optimization/OptimizationDemo.java)
> <!-- 本地路径：code-examples/03-data-store/database-examples/src/main/java/com/example/database/optimization/OptimizationDemo.java -->
>
> ⚠️ 需要 MySQL 环境：`docker compose -f docker/docker-compose.yml up -d mysql`

## 常见面试题

### Q1: EXPLAIN 执行计划中各字段的含义？重点关注哪些？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 重点说 type、key、rows、Extra 四个字段
2. type 从好到差排列
3. Extra 中需要优化的标志

**标准答案**：

EXPLAIN 最重要的四个字段：`type`（访问类型，从好到差：const > eq_ref > ref > range > index > ALL）、`key`（实际使用的索引）、`rows`（预估扫描行数）、`Extra`（额外信息）。

type 至少要达到 range 级别，ALL 表示全表扫描必须优化。Extra 中 `Using index` 表示覆盖索引（好），`Using filesort` 表示额外排序（需优化），`Using temporary` 表示使用临时表（需优化）。

**深入追问**：

- type 为 index 和 ALL 有什么区别？
- key_len 怎么计算？
- Using index condition 是什么意思？

### Q2: 索引在什么情况下会失效？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 列举常见的索引失效场景
2. 解释每种场景的原因
3. 给出优化方案

**标准答案**：

常见索引失效场景：1）对索引列使用函数或计算；2）隐式类型转换（varchar 列用数字查询）；3）左模糊查询 `LIKE '%xxx'`；4）联合索引不满足最左前缀原则；5）OR 条件中有一个列无索引；6）优化器判断全表扫描更快时（数据量小或区分度低）。

核心原因：这些操作破坏了 B+树索引的有序性，导致无法利用索引快速定位。

**深入追问**：

- 如何判断一个索引的区分度？
- `!=` 和 `NOT IN` 一定不走索引吗？
- 如何优化 `LIKE '%xxx%'` 的查询？

### Q3: 如何优化一个慢 SQL？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 开启慢查询日志定位慢 SQL
2. 用 EXPLAIN 分析执行计划
3. 针对性优化（加索引、改写 SQL、分页优化等）

**标准答案**：

优化步骤：1）开启慢查询日志，定位慢 SQL；2）用 EXPLAIN 分析执行计划，关注 type、key、rows、Extra；3）针对性优化：添加合适的索引、避免索引失效、使用覆盖索引减少回表、优化 JOIN 查询（小表驱动大表）、深分页优化（延迟关联）、避免 SELECT *。

深分页优化示例：
```sql
-- 慢：OFFSET 很大时需要扫描大量行
SELECT * FROM user LIMIT 1000000, 10;
-- 快：延迟关联
SELECT * FROM user u INNER JOIN (SELECT id FROM user LIMIT 1000000, 10) t ON u.id = t.id;
```

**深入追问**：

- 深分页还有什么优化方案？
- JOIN 查询怎么优化？
- 如何用 FORCE INDEX 强制使用索引？

## 参考资料

- [MySQL 官方文档 - EXPLAIN](https://dev.mysql.com/doc/refman/8.0/en/explain-output.html)
- [MySQL 官方文档 - 优化](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)

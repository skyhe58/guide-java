---
title: "数据结构与算法"
module: "algorithm"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "数据结构"
  - "算法"
  - "LeetCode"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/"
relatedEntries:
  - "/1-java-core/1.1-java-basics/05-collections"
  - "/1-java-core/1.2-java-advanced/01-collections-source"
prerequisites:
  - "/1-java-core/1.1-java-basics/05-collections"
  - "/1-java-core/1.1-java-basics/04-oop"
estimatedTime: "120min"
---

# 数据结构与算法

## 模块概述

数据结构与算法是程序员的内功，也是技术面试中必考的环节。本模块系统覆盖面试高频数据结构和算法思想，优先覆盖 **LeetCode Hot 100** 中的 Java 后端高频题。

## 知识分类

### 数据结构篇

| 专题 | 核心知识点 | 高频题目 | 文档链接 |
|------|-----------|---------|---------|
| 链表 | 反转、合并、环检测、快慢指针 | LC 206/21/141/142/19/24 | [linked-list](./linked-list) |
| 栈与队列 | 单调栈、栈队互转、滑动窗口 | LC 20/155/232/739/239 | [stack-queue](./stack-queue) |
| 哈希表 | 哈希映射、去重、LRU 缓存 | LC 1/49/128/146 | [hash-table](./hash-table) |
| 二叉树 | 遍历、BST、公共祖先 | LC 144/94/145/102/104/226/98/236 | [binary-tree](./binary-tree) |
| 堆 | TopK、优先队列、多路归并 | LC 215/23/347 | [heap](./heap) |

### 算法思想篇

| 专题 | 核心知识点 | 高频题目 | 文档链接 |
|------|-----------|---------|---------|
| 排序算法 | 快排、归并、堆排序、稳定性 | — | [sorting](./sorting) |
| 二分查找 | 基础二分、旋转数组、边界查找 | LC 704/33/34/35 | [binary-search](./binary-search) |
| 双指针与滑动窗口 | 对撞指针、快慢指针、窗口收缩 | LC 15/11/3/76 | [two-pointers](./two-pointers) |
| 动态规划 | 状态定义、转移方程、空间优化 | LC 70/300/72/1143/322 | [dynamic-programming](./dynamic-programming) |
| 回溯算法 | 决策树、剪枝、排列组合 | LC 46/78/39/51/17 | [backtracking](./backtracking) |

## 推荐学习顺序

```mermaid
graph LR
    A[链表] --> B[栈与队列]
    B --> C[哈希表]
    C --> D[二叉树]
    D --> E[堆]
    E --> F[排序算法]
    F --> G[二分查找]
    G --> H[双指针与滑动窗口]
    H --> I[动态规划]
    I --> J[回溯算法]
```

**建议**：
1. 先掌握基础数据结构（链表→栈队列→哈希表→树→堆）
2. 再学习算法思想（排序→二分→双指针→DP→回溯）
3. 每个专题先理解思路，再刷对应题目
4. 每道题至少手写两遍，间隔 3 天以上

## LeetCode Hot 100 对照表

| 题号 | 题目 | 难度 | 所属专题 | 面试频率 |
|------|------|------|---------|---------|
| 1 | 两数之和 | 🟢 Easy | 哈希表 | 🔥🔥🔥 |
| 3 | 无重复字符的最长子串 | 🟡 Medium | 滑动窗口 | 🔥🔥🔥 |
| 11 | 盛最多水的容器 | 🟡 Medium | 双指针 | 🔥🔥 |
| 15 | 三数之和 | 🟡 Medium | 双指针 | 🔥🔥🔥 |
| 19 | 删除链表倒数第 N 个节点 | 🟡 Medium | 链表 | 🔥🔥 |
| 20 | 有效的括号 | 🟢 Easy | 栈 | 🔥🔥🔥 |
| 21 | 合并两个有序链表 | 🟢 Easy | 链表 | 🔥🔥🔥 |
| 23 | 合并 K 个升序链表 | 🔴 Hard | 堆 | 🔥🔥 |
| 24 | 两两交换链表中的节点 | 🟡 Medium | 链表 | 🔥🔥 |
| 33 | 搜索旋转排序数组 | 🟡 Medium | 二分查找 | 🔥🔥🔥 |
| 34 | 查找第一个和最后一个位置 | 🟡 Medium | 二分查找 | 🔥🔥 |
| 39 | 组合总和 | 🟡 Medium | 回溯 | 🔥🔥 |
| 46 | 全排列 | 🟡 Medium | 回溯 | 🔥🔥🔥 |
| 49 | 字母异位词分组 | 🟡 Medium | 哈希表 | 🔥🔥 |
| 51 | N 皇后 | 🔴 Hard | 回溯 | 🔥🔥 |
| 70 | 爬楼梯 | 🟢 Easy | 动态规划 | 🔥🔥🔥 |
| 72 | 编辑距离 | 🟡 Medium | 动态规划 | 🔥🔥 |
| 76 | 最小覆盖子串 | 🔴 Hard | 滑动窗口 | 🔥🔥 |
| 78 | 子集 | 🟡 Medium | 回溯 | 🔥🔥 |
| 94 | 二叉树的中序遍历 | 🟢 Easy | 二叉树 | 🔥🔥🔥 |
| 98 | 验证二叉搜索树 | 🟡 Medium | 二叉树 | 🔥🔥 |
| 102 | 二叉树的层序遍历 | 🟡 Medium | 二叉树 | 🔥🔥🔥 |
| 104 | 二叉树的最大深度 | 🟢 Easy | 二叉树 | 🔥🔥🔥 |
| 128 | 最长连续序列 | 🟡 Medium | 哈希表 | 🔥🔥 |
| 141 | 环形链表 | 🟢 Easy | 链表 | 🔥🔥🔥 |
| 142 | 环形链表 II | 🟡 Medium | 链表 | 🔥🔥 |
| 146 | LRU 缓存 | 🟡 Medium | 哈希表 | 🔥🔥🔥 |
| 155 | 最小栈 | 🟡 Medium | 栈 | 🔥🔥 |
| 206 | 反转链表 | 🟢 Easy | 链表 | 🔥🔥🔥 |
| 215 | 数组中的第 K 个最大元素 | 🟡 Medium | 堆 | 🔥🔥🔥 |
| 226 | 翻转二叉树 | 🟢 Easy | 二叉树 | 🔥🔥 |
| 232 | 用栈实现队列 | 🟢 Easy | 栈 | 🔥🔥 |
| 236 | 二叉树的最近公共祖先 | 🟡 Medium | 二叉树 | 🔥🔥🔥 |
| 239 | 滑动窗口最大值 | 🔴 Hard | 栈/队列 | 🔥🔥 |
| 300 | 最长递增子序列 | 🟡 Medium | 动态规划 | 🔥🔥🔥 |
| 322 | 零钱兑换 | 🟡 Medium | 动态规划 | 🔥🔥 |
| 347 | 前 K 个高频元素 | 🟡 Medium | 堆 | 🔥🔥 |
| 704 | 二分查找 | 🟢 Easy | 二分查找 | 🔥🔥🔥 |
| 739 | 每日温度 | 🟡 Medium | 栈 | 🔥🔥 |
| 1143 | 最长公共子序列 | 🟡 Medium | 动态规划 | 🔥🔥 |

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/](../../../code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/)

## 参考资料

- [LeetCode 热题 100](https://leetcode.cn/studyplan/top-100-liked/)
- [代码随想录](https://programmercarl.com/)
- [labuladong 的算法笔记](https://labuladong.online/algo/)

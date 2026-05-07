---
title: "算法面试指南"
module: "algorithm"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "面试"
  - "算法"
  - "LeetCode"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/"
relatedEntries:
  - "/1-java-core/1.6-algorithm/index"
prerequisites:
  - "/1-java-core/1.6-algorithm/index"
estimatedTime: "30min"
---

# 算法面试指南

## 面试高频题按频率排序

### 🔥🔥🔥 必刷题（几乎每次面试都会考）

| 题号 | 题目 | 专题 | 难度 |
|------|------|------|------|
| 206 | 反转链表 | 链表 | Easy |
| 1 | 两数之和 | 哈希表 | Easy |
| 146 | LRU 缓存 | 哈希表 | Medium |
| 15 | 三数之和 | 双指针 | Medium |
| 3 | 无重复字符的最长子串 | 滑动窗口 | Medium |
| 20 | 有效的括号 | 栈 | Easy |
| 21 | 合并两个有序链表 | 链表 | Easy |
| 141 | 环形链表 | 链表 | Easy |
| 33 | 搜索旋转排序数组 | 二分查找 | Medium |
| 102 | 二叉树的层序遍历 | 二叉树 | Medium |
| 236 | 最近公共祖先 | 二叉树 | Medium |
| 215 | 第 K 大元素 | 堆 | Medium |
| 46 | 全排列 | 回溯 | Medium |
| 70 | 爬楼梯 | DP | Easy |
| 300 | 最长递增子序列 | DP | Medium |

### 🔥🔥 高频题（经常出现）

| 题号 | 题目 | 专题 | 难度 |
|------|------|------|------|
| 142 | 环形链表 II | 链表 | Medium |
| 19 | 删除倒数第 N 个节点 | 链表 | Medium |
| 155 | 最小栈 | 栈 | Medium |
| 739 | 每日温度 | 单调栈 | Medium |
| 49 | 字母异位词分组 | 哈希表 | Medium |
| 128 | 最长连续序列 | 哈希表 | Medium |
| 98 | 验证 BST | 二叉树 | Medium |
| 104 | 最大深度 | 二叉树 | Easy |
| 226 | 翻转二叉树 | 二叉树 | Easy |
| 23 | 合并 K 个链表 | 堆 | Hard |
| 347 | 前 K 个高频元素 | 堆 | Medium |
| 34 | 查找第一个和最后一个位置 | 二分 | Medium |
| 11 | 盛最多水的容器 | 双指针 | Medium |
| 76 | 最小覆盖子串 | 滑动窗口 | Hard |
| 72 | 编辑距离 | DP | Medium |
| 322 | 零钱兑换 | DP | Medium |
| 78 | 子集 | 回溯 | Medium |
| 39 | 组合总和 | 回溯 | Medium |
| 51 | N 皇后 | 回溯 | Hard |

## 解题模板速查

### 链表模板
```java
// 虚拟头节点
ListNode dummy = new ListNode(0, head);
// 快慢指针
ListNode slow = head, fast = head;
```

### 二分查找模板
```java
int left = 0, right = nums.length - 1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (nums[mid] == target) return mid;
    else if (nums[mid] < target) left = mid + 1;
    else right = mid - 1;
}
```

### 滑动窗口模板
```java
int left = 0;
for (int right = 0; right < n; right++) {
    // 扩大窗口
    while (需要收缩) { left++; }
    // 更新结果
}
```

### 回溯模板
```java
void backtrack(路径, 选择列表) {
    if (满足结束条件) { result.add(路径); return; }
    for (选择 : 选择列表) {
        做选择;
        backtrack(路径, 选择列表);
        撤销选择;
    }
}
```

### BFS 层序遍历模板
```java
Queue<TreeNode> queue = new LinkedList<>();
queue.offer(root);
while (!queue.isEmpty()) {
    int size = queue.size();
    for (int i = 0; i < size; i++) {
        TreeNode node = queue.poll();
        // 处理节点
        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
}
```

## 面试技巧

### 1. 解题步骤

1. **理解题意**：确认输入输出、边界条件、是否有重复元素
2. **举例验证**：用小规模例子验证理解
3. **说出思路**：先说暴力解法，再优化
4. **编写代码**：边写边解释
5. **测试验证**：用例子走一遍代码
6. **复杂度分析**：时间和空间复杂度

### 2. 常见优化方向

| 暴力解法 | 优化方向 | 典型题目 |
|---------|---------|---------|
| O(n²) 两层循环 | 哈希表 O(n) | 两数之和 |
| O(n²) 两层循环 | 排序 + 双指针 O(n log n) | 三数之和 |
| O(n²) 暴力搜索 | 二分查找 O(n log n) | 旋转数组搜索 |
| O(2^n) 递归 | 动态规划 O(n²) 或 O(n) | 爬楼梯、LIS |
| O(n log n) 排序 | 堆 O(n log k) | TopK 问题 |

### 3. 面试中的沟通要点

- **先说思路再写代码**：面试官更看重思维过程
- **主动分析复杂度**：不等面试官问
- **遇到不会的题**：说出已知的思路，尝试暴力解法
- **代码写完后主动测试**：用边界用例走一遍

## 按公司类型的重点

### 大厂（字节/阿里/腾讯/美团）
- 重点：链表、二叉树、DP、滑动窗口
- 难度：Medium 为主，偶尔 Hard
- 特点：要求手写代码，注重代码质量和边界处理

### 中厂
- 重点：哈希表、排序、二分查找、基础 DP
- 难度：Easy-Medium
- 特点：更注重基础扎实度

### 外企
- 重点：系统设计 + 算法并重
- 难度：Medium
- 特点：注重代码风格、测试用例设计

## 参考资料

- [LeetCode 热题 100](https://leetcode.cn/studyplan/top-100-liked/)
- [代码随想录](https://programmercarl.com/)

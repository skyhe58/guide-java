---
title: "二分查找"
module: "algorithm"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "算法"
  - "二分查找"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/search/"
relatedEntries:
  - "/1-java-core/1.6-algorithm/06-sorting"
  - "/1-java-core/1.6-algorithm/08-two-pointers"
prerequisites:
  - "/1-java-core/1.6-algorithm/06-sorting"
estimatedTime: "40min"
---

# 二分查找

## 概念说明

二分查找是在有序数组中查找目标值的高效算法，时间复杂度 O(log n)。面试中的难点在于**边界处理**和**变体题目**（旋转数组、查找边界）。

## 核心题目

### 一、基础二分查找（LeetCode 704）🟢 Easy | 🔥🔥🔥

```java
/**
 * 基础二分查找
 * 时间复杂度: O(log n)，空间复杂度: O(1)
 */
public int search(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) { // 注意：<=
        int mid = left + (right - left) / 2; // 防止溢出
        if (nums[mid] == target) return mid;
        else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

> ⚠️ 二分查找的三个易错点：`left <= right`、`mid` 的计算方式、`left = mid + 1` 而非 `left = mid`。

---

### 二、搜索旋转排序数组（LeetCode 33）🟡 Medium | 🔥🔥🔥

**题目描述**：在旋转排序数组（如 `[4,5,6,7,0,1,2]`）中搜索目标值。

**解题思路**：二分后，至少有一半是有序的，判断 target 在有序的那一半还是无序的那一半。

```java
/**
 * 搜索旋转排序数组
 * 时间复杂度: O(log n)
 */
public int searchRotated(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) return mid;

        if (nums[left] <= nums[mid]) {
            // 左半部分有序
            if (nums[left] <= target && target < nums[mid]) {
                right = mid - 1; // target 在左半部分
            } else {
                left = mid + 1;
            }
        } else {
            // 右半部分有序
            if (nums[mid] < target && target <= nums[right]) {
                left = mid + 1; // target 在右半部分
            } else {
                right = mid - 1;
            }
        }
    }
    return -1;
}
```

---

### 三、查找第一个和最后一个位置（LeetCode 34）🟡 Medium | 🔥🔥

**题目描述**：在排序数组中查找目标值的起始和结束位置。

```java
/**
 * 查找第一个和最后一个位置
 */
public int[] searchRange(int[] nums, int target) {
    return new int[]{findFirst(nums, target), findLast(nums, target)};
}

private int findFirst(int[] nums, int target) {
    int left = 0, right = nums.length - 1, result = -1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) {
            result = mid;
            right = mid - 1; // 继续向左找
        } else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return result;
}

private int findLast(int[] nums, int target) {
    int left = 0, right = nums.length - 1, result = -1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) {
            result = mid;
            left = mid + 1; // 继续向右找
        } else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return result;
}
```

---

### 四、搜索插入位置（LeetCode 35）🟢 Easy | 🔥🔥

```java
/**
 * 搜索插入位置 — 找到第一个 >= target 的位置
 */
public int searchInsert(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] >= target) right = mid - 1;
        else left = mid + 1;
    }
    return left; // left 就是插入位置
}
```

## 二分查找模板总结

| 场景 | 循环条件 | 返回值 |
|------|---------|--------|
| 精确查找 | `left <= right` | `mid`（找到时） |
| 查找左边界（第一个 ≥ target） | `left <= right` | `left` |
| 查找右边界（最后一个 ≤ target） | `left <= right` | `right` |

## 代码示例

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/search/](../../../code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/search/)

## 常见面试题

### Q1: 二分查找的边界条件怎么处理？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：关键在于三点：（1）循环条件用 `left <= right`（闭区间）；（2）`mid = left + (right - left) / 2` 防止整数溢出；（3）更新时 `left = mid + 1` 和 `right = mid - 1`，避免死循环。

**深入追问**：
- 如何在旋转排序数组中找最小值？（LC 153）
- 如何在有重复元素的旋转数组中搜索？（LC 81）

## 参考资料

- [LeetCode 704. 二分查找](https://leetcode.cn/problems/binary-search/)
- [LeetCode 33. 搜索旋转排序数组](https://leetcode.cn/problems/search-in-rotated-sorted-array/)

---
title: "堆"
module: "algorithm"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "数据结构"
  - "堆"
  - "优先队列"
  - "TopK"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/05-heap/"
relatedEntries:
  - "/1-java-core/1.6-algorithm/06-sorting"
  - "/1-java-core/1.6-algorithm/04-binary-tree"
prerequisites:
  - "/1-java-core/1.6-algorithm/04-binary-tree"
estimatedTime: "40min"
---

# 堆

## 概念说明

堆是一种特殊的完全二叉树，分为最大堆（父 ≥ 子）和最小堆（父 ≤ 子）。Java 中用 `PriorityQueue` 实现（默认最小堆）。堆的核心应用场景：**TopK 问题**和**多路归并**。

## 核心题目

### 一、数组中的第 K 个最大元素（LeetCode 215）🟡 Medium | 🔥🔥🔥

**方法一：最小堆（推荐）**

维护一个大小为 K 的最小堆，堆顶就是第 K 大元素。

```java
/**
 * 第 K 大元素 — 最小堆
 * 时间复杂度: O(n log k)，空间复杂度: O(k)
 */
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll(); // 弹出最小的，保留最大的 k 个
        }
    }
    return minHeap.peek();
}
```

**方法二：快速选择（平均 O(n)）**

```java
/**
 * 第 K 大元素 — 快速选择
 * 平均时间复杂度: O(n)，最坏 O(n²)
 */
public int findKthLargestQuickSelect(int[] nums, int k) {
    int target = nums.length - k; // 转化为第 target 小
    return quickSelect(nums, 0, nums.length - 1, target);
}

private int quickSelect(int[] nums, int left, int right, int target) {
    int pivot = partition(nums, left, right);
    if (pivot == target) return nums[pivot];
    else if (pivot < target) return quickSelect(nums, pivot + 1, right, target);
    else return quickSelect(nums, left, pivot - 1, target);
}

private int partition(int[] nums, int left, int right) {
    int pivot = nums[right];
    int i = left;
    for (int j = left; j < right; j++) {
        if (nums[j] <= pivot) {
            swap(nums, i++, j);
        }
    }
    swap(nums, i, right);
    return i;
}

private void swap(int[] nums, int i, int j) {
    int tmp = nums[i]; nums[i] = nums[j]; nums[j] = tmp;
}
```

| 方法 | 时间复杂度 | 空间复杂度 | 适用场景 |
|------|-----------|-----------|---------|
| 最小堆 | O(n log k) | O(k) | 数据流/k 较小 |
| 快速选择 | 平均 O(n) | O(1) | 静态数组 |

---

### 二、合并 K 个升序链表（LeetCode 23）🔴 Hard | 🔥🔥

**解题思路**：用最小堆维护 K 个链表的当前头节点，每次取最小的。

```java
/**
 * 合并 K 个升序链表 — 最小堆
 * 时间复杂度: O(N log k)，N 为总节点数，k 为链表数
 */
public ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> heap = new PriorityQueue<>(
        Comparator.comparingInt(a -> a.val)
    );
    for (ListNode head : lists) {
        if (head != null) heap.offer(head);
    }
    ListNode dummy = new ListNode(0);
    ListNode curr = dummy;
    while (!heap.isEmpty()) {
        ListNode min = heap.poll();
        curr.next = min;
        curr = curr.next;
        if (min.next != null) heap.offer(min.next);
    }
    return dummy.next;
}
```

---

### 三、前 K 个高频元素（LeetCode 347）🟡 Medium | 🔥🔥

```java
/**
 * 前 K 个高频元素 — 最小堆
 * 时间复杂度: O(n log k)
 */
public int[] topKFrequent(int[] nums, int k) {
    // 1. 统计频率
    Map<Integer, Integer> freq = new HashMap<>();
    for (int num : nums) freq.merge(num, 1, Integer::sum);

    // 2. 最小堆维护前 K 个高频
    PriorityQueue<Map.Entry<Integer, Integer>> heap =
        new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
    for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
        heap.offer(entry);
        if (heap.size() > k) heap.poll();
    }

    // 3. 提取结果
    return heap.stream().mapToInt(Map.Entry::getKey).toArray();
}
```

## 代码示例

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/05-heap/](../../../code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/05-heap/)

## 常见面试题

### Q1: TopK 问题有哪些解法？各自的时间复杂度？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：
1. 排序：O(n log n)，最简单但不是最优
2. 最小堆：O(n log k)，适合数据流场景
3. 快速选择：平均 O(n)，适合静态数组
4. 桶排序：O(n)，适合值域有限的场景

**深入追问**：
- 如果数据量非常大（内存放不下）怎么办？（分治 + 堆归并）
- PriorityQueue 的底层实现？（数组实现的完全二叉树，上浮下沉操作）

## 参考资料

- [LeetCode 215. 数组中的第K个最大元素](https://leetcode.cn/problems/kth-largest-element-in-an-array/)
- [LeetCode 23. 合并K个升序链表](https://leetcode.cn/problems/merge-k-sorted-lists/)

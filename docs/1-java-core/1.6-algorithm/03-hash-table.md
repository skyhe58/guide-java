---
title: "哈希表"
module: "algorithm"
difficulty: "beginner"
interviewFrequency: "high"
tags:
  - "数据结构"
  - "哈希表"
  - "LRU"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/hashtable/"
relatedEntries:
  - "/1-java-core/1.1-java-basics/05-collections"
  - "/1-java-core/1.2-java-advanced/01-collections-source"
prerequisites:
  - "/1-java-core/1.1-java-basics/05-collections"
estimatedTime: "50min"
---

# 哈希表

## 概念说明

哈希表通过 key 的哈希值实现 O(1) 的查找、插入和删除。面试中哈希表题目的核心思路是**空间换时间**，用额外的 HashMap/HashSet 来加速查找。

## 核心题目

### 一、两数之和（LeetCode 1）🟢 Easy | 🔥🔥🔥

**题目描述**：给定数组和目标值 target，找出和为 target 的两个数的下标。

```java
/**
 * 两数之和 — 哈希表一次遍历
 * 时间复杂度: O(n)，空间复杂度: O(n)
 */
public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>(); // 值 -> 下标
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[]{map.get(complement), i};
        }
        map.put(nums[i], i);
    }
    return new int[0];
}
```

> 💡 核心思路：遍历时查找 `target - nums[i]` 是否已在 map 中。

---

### 二、字母异位词分组（LeetCode 49）🟡 Medium | 🔥🔥

**题目描述**：给定字符串数组，将字母异位词组合在一起。

```java
/**
 * 字母异位词分组 — 排序作为 key
 * 时间复杂度: O(n * k * log k)，k 为字符串最大长度
 */
public List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> map = new HashMap<>();
    for (String s : strs) {
        char[] chars = s.toCharArray();
        Arrays.sort(chars);
        String key = new String(chars); // 排序后的字符串作为 key
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }
    return new ArrayList<>(map.values());
}
```

---

### 三、最长连续序列（LeetCode 128）🟡 Medium | 🔥🔥

**题目描述**：给定未排序数组，找出最长连续序列的长度，要求 O(n) 时间。

```java
/**
 * 最长连续序列 — HashSet
 * 时间复杂度: O(n)，空间复杂度: O(n)
 */
public int longestConsecutive(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int num : nums) set.add(num);

    int maxLen = 0;
    for (int num : set) {
        // 只从序列的起点开始计数（前一个数不在集合中）
        if (!set.contains(num - 1)) {
            int current = num;
            int len = 1;
            while (set.contains(current + 1)) {
                current++;
                len++;
            }
            maxLen = Math.max(maxLen, len);
        }
    }
    return maxLen;
}
```

> 💡 关键优化：只从序列起点（`num - 1` 不存在）开始遍历，避免重复计算。

---

### 四、LRU 缓存（LeetCode 146）🟡 Medium | 🔥🔥🔥

**题目描述**：设计 LRU（最近最少使用）缓存，支持 O(1) 的 `get` 和 `put`。

**解题思路**：**HashMap + 双向链表**

```mermaid
flowchart LR
    subgraph "LRU 缓存结构"
        HM["HashMap<key, Node>"]
        DL["双向链表: head ↔ node1 ↔ node2 ↔ ... ↔ tail"]
    end
    HM -->|O(1) 定位| DL
```

```java
/**
 * LRU 缓存 — HashMap + 双向链表
 */
public class LRUCache {
    private int capacity;
    private Map<Integer, Node> map;
    private Node head, tail; // 虚拟头尾节点

    static class Node {
        int key, value;
        Node prev, next;
        Node(int key, int value) { this.key = key; this.value = value; }
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        head = new Node(0, 0);
        tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node node = map.get(key);
        moveToHead(node); // 访问后移到头部（最近使用）
        return node.value;
    }

    public void put(int key, int value) {
        if (map.containsKey(key)) {
            Node node = map.get(key);
            node.value = value;
            moveToHead(node);
        } else {
            Node node = new Node(key, value);
            map.put(key, node);
            addToHead(node);
            if (map.size() > capacity) {
                Node removed = removeTail(); // 移除最久未使用的
                map.remove(removed.key);
            }
        }
    }

    private void addToHead(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private Node removeTail() {
        Node node = tail.prev;
        removeNode(node);
        return node;
    }
}
```

**复杂度**：get 和 put 均为 O(1)。

## 代码示例

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/hashtable/](../../../code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/hashtable/)

## 常见面试题

### Q1: 手写 LRU 缓存

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：
1. 说明数据结构选择：HashMap + 双向链表
2. HashMap 实现 O(1) 查找，双向链表维护访问顺序
3. get 时将节点移到头部，put 时超容量移除尾部

**深入追问**：
- 为什么用双向链表而不是单向链表？（删除节点需要 O(1)，单向链表删除需要知道前驱节点）
- Java 中 LinkedHashMap 如何实现 LRU？（构造函数传 `accessOrder=true`，重写 `removeEldestEntry`）
- LRU 和 LFU 的区别？（LRU 淘汰最久未使用，LFU 淘汰使用频率最低的）

## 参考资料

- [LeetCode 1. 两数之和](https://leetcode.cn/problems/two-sum/)
- [LeetCode 146. LRU 缓存](https://leetcode.cn/problems/lru-cache/)
- [LeetCode 128. 最长连续序列](https://leetcode.cn/problems/longest-consecutive-sequence/)

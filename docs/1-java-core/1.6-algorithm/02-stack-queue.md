---
title: "栈与队列"
module: "algorithm"
difficulty: "beginner"
interviewFrequency: "high"
tags:
  - "数据结构"
  - "栈"
  - "队列"
  - "单调栈"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/stack/"
relatedEntries:
  - "/1-java-core/1.6-algorithm/01-linked-list"
  - "/1-java-core/1.1-java-basics/05-collections"
prerequisites:
  - "/1-java-core/1.1-java-basics/05-collections"
estimatedTime: "50min"
---

# 栈与队列

## 概念说明

栈（LIFO）和队列（FIFO）是最基础的线性数据结构。面试中常考的技巧包括：**辅助栈**、**单调栈**、**栈队互转**和**滑动窗口（单调队列）**。

## 核心题目

### 一、有效的括号（LeetCode 20）🟢 Easy | 🔥🔥🔥

**题目描述**：给定一个只包含 `()`、`{}`、`[]` 的字符串，判断是否有效。

```java
/**
 * 有效的括号
 * 时间复杂度: O(n)，空间复杂度: O(n)
 */
public boolean isValid(String s) {
    Deque<Character> stack = new ArrayDeque<>();
    for (char c : s.toCharArray()) {
        if (c == '(') stack.push(')');
        else if (c == '{') stack.push('}');
        else if (c == '[') stack.push(']');
        else if (stack.isEmpty() || stack.pop() != c) return false;
    }
    return stack.isEmpty();
}
```

> 💡 技巧：遇到左括号时压入对应的右括号，匹配时直接比较即可。

---

### 二、最小栈（LeetCode 155）🟡 Medium | 🔥🔥

**题目描述**：设计一个支持 `push`、`pop`、`top` 和 `getMin` 操作的栈，所有操作 O(1)。

**解题思路**：使用辅助栈同步记录当前最小值。

```java
/**
 * 最小栈 — 辅助栈实现
 */
public class MinStack {
    private Deque<Integer> stack;
    private Deque<Integer> minStack; // 辅助栈，栈顶始终是当前最小值

    public MinStack() {
        stack = new ArrayDeque<>();
        minStack = new ArrayDeque<>();
    }

    public void push(int val) {
        stack.push(val);
        // 辅助栈为空或新值 <= 当前最小值时入栈
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        }
    }

    public void pop() {
        int val = stack.pop();
        if (val == minStack.peek()) {
            minStack.pop();
        }
    }

    public int top() { return stack.peek(); }
    public int getMin() { return minStack.peek(); }
}
```

---

### 三、用栈实现队列（LeetCode 232）🟢 Easy | 🔥🔥

**解题思路**：两个栈，一个负责入队，一个负责出队。出队栈为空时，将入队栈全部倒入。

```java
public class MyQueue {
    private Deque<Integer> inStack;
    private Deque<Integer> outStack;

    public MyQueue() {
        inStack = new ArrayDeque<>();
        outStack = new ArrayDeque<>();
    }

    public void push(int x) { inStack.push(x); }

    public int pop() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) outStack.push(inStack.pop());
        }
        return outStack.pop();
    }

    public int peek() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) outStack.push(inStack.pop());
        }
        return outStack.peek();
    }

    public boolean empty() { return inStack.isEmpty() && outStack.isEmpty(); }
}
```

**均摊时间复杂度**：每个元素最多被搬运两次（入栈一次、倒入出栈一次），所以均摊 O(1)。

---

### 四、每日温度（LeetCode 739）🟡 Medium | 🔥🔥

**题目描述**：给定每日温度数组，返回每天需要等几天才能遇到更高温度。

**解题思路**：**单调递减栈**，栈中存储下标。

```java
/**
 * 每日温度 — 单调栈
 * 时间复杂度: O(n)，空间复杂度: O(n)
 */
public int[] dailyTemperatures(int[] temperatures) {
    int n = temperatures.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // 存下标，维护单调递减
    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
            int idx = stack.pop();
            result[idx] = i - idx;
        }
        stack.push(i);
    }
    return result;
}
```

---

### 五、滑动窗口最大值（LeetCode 239）🔴 Hard | 🔥🔥

**题目描述**：给定数组和窗口大小 k，返回每个窗口的最大值。

**解题思路**：**单调递减双端队列**，队首始终是窗口最大值的下标。

```java
/**
 * 滑动窗口最大值 — 单调队列
 * 时间复杂度: O(n)，空间复杂度: O(k)
 */
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // 存下标，维护单调递减
    for (int i = 0; i < n; i++) {
        // 移除超出窗口范围的元素
        while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
            deque.pollFirst();
        }
        // 维护单调递减：移除所有比当前元素小的
        while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

## 代码示例

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/stack/](https://github.com/skyhe58/guide-java/tree/main/code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/stack/)
> <!-- 本地路径：code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/stack/ -->

## 常见面试题

### Q1: 什么是单调栈？适用于什么场景？

**难度**：⭐⭐⭐ | **频率**：🔥🔥

**标准答案**：单调栈是一种栈内元素保持单调递增或递减的栈。适用于"找到每个元素左/右边第一个比它大/小的元素"类问题。典型题目：每日温度、下一个更大元素、柱状图中最大的矩形。

**深入追问**：
- 单调栈和单调队列的区别？（栈只能从一端操作，队列可以两端操作）
- 如何用单调栈解决"接雨水"问题？

### Q2: 如何用两个栈实现队列？时间复杂度是多少？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：见上方 LC 232 解法。均摊时间复杂度 O(1)。

## 参考资料

- [LeetCode 20. 有效的括号](https://leetcode.cn/problems/valid-parentheses/)
- [LeetCode 739. 每日温度](https://leetcode.cn/problems/daily-temperatures/)
- [LeetCode 239. 滑动窗口最大值](https://leetcode.cn/problems/sliding-window-maximum/)

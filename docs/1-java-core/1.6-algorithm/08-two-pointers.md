---
title: "双指针与滑动窗口"
module: "algorithm"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "算法"
  - "双指针"
  - "滑动窗口"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/twopointers/"
relatedEntries:
  - "/1-java-core/1.6-algorithm/07-binary-search"
  - "/1-java-core/1.6-algorithm/03-hash-table"
prerequisites:
  - "/1-java-core/1.6-algorithm/06-sorting"
estimatedTime: "50min"
---

# 双指针与滑动窗口

## 概念说明

双指针是一种常用的算法技巧，通过两个指针在数组或字符串上移动来降低时间复杂度。常见类型：**对撞指针**（从两端向中间）、**快慢指针**、**滑动窗口**（同向移动）。

## 核心题目

### 一、三数之和（LeetCode 15）🟡 Medium | 🔥🔥🔥

**题目描述**：找出数组中所有和为 0 的三元组，不能重复。

**解题思路**：排序 + 固定一个数 + 双指针找另外两个。

```java
/**
 * 三数之和 — 排序 + 双指针
 * 时间复杂度: O(n²)，空间复杂度: O(1)（不计输出）
 */
public List<List<Integer>> threeSum(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);
    for (int i = 0; i < nums.length - 2; i++) {
        if (nums[i] > 0) break; // 最小值 > 0，不可能凑出 0
        if (i > 0 && nums[i] == nums[i - 1]) continue; // 跳过重复

        int left = i + 1, right = nums.length - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            if (sum == 0) {
                result.add(List.of(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;  // 跳过重复
                while (left < right && nums[right] == nums[right - 1]) right--;
                left++;
                right--;
            } else if (sum < 0) left++;
            else right--;
        }
    }
    return result;
}
```

---

### 二、盛最多水的容器（LeetCode 11）🟡 Medium | 🔥🔥

**解题思路**：对撞指针，每次移动较短的那一边（因为移动较长的不可能增大面积）。

```java
/**
 * 盛最多水的容器 — 对撞指针
 * 时间复杂度: O(n)，空间复杂度: O(1)
 */
public int maxArea(int[] height) {
    int left = 0, right = height.length - 1;
    int maxWater = 0;
    while (left < right) {
        int area = Math.min(height[left], height[right]) * (right - left);
        maxWater = Math.max(maxWater, area);
        if (height[left] < height[right]) left++;
        else right--;
    }
    return maxWater;
}
```

---

### 三、无重复字符的最长子串（LeetCode 3）🟡 Medium | 🔥🔥🔥

**解题思路**：**滑动窗口** + HashSet/HashMap 记录窗口内字符。

```java
/**
 * 无重复字符的最长子串 — 滑动窗口
 * 时间复杂度: O(n)，空间复杂度: O(min(n, 字符集大小))
 */
public int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> map = new HashMap<>(); // 字符 -> 最新下标
    int maxLen = 0, left = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        if (map.containsKey(c) && map.get(c) >= left) {
            left = map.get(c) + 1; // 窗口左边界跳到重复字符的下一个位置
        }
        map.put(c, right);
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}
```

---

### 四、最小覆盖子串（LeetCode 76）🔴 Hard | 🔥🔥

**题目描述**：给定字符串 s 和 t，找出 s 中包含 t 所有字符的最小子串。

**解题思路**：滑动窗口，先扩大右边界直到包含所有字符，再收缩左边界找最小窗口。

```java
/**
 * 最小覆盖子串 — 滑动窗口
 * 时间复杂度: O(n)，空间复杂度: O(字符集大小)
 */
public String minWindow(String s, String t) {
    Map<Character, Integer> need = new HashMap<>(); // t 中每个字符的需求量
    for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);

    int left = 0, minLen = Integer.MAX_VALUE, start = 0;
    int required = need.size(); // 需要满足的不同字符数
    int formed = 0;             // 已满足的不同字符数
    Map<Character, Integer> window = new HashMap<>();

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        window.merge(c, 1, Integer::sum);
        if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) {
            formed++;
        }
        // 收缩左边界
        while (formed == required) {
            if (right - left + 1 < minLen) {
                minLen = right - left + 1;
                start = left;
            }
            char leftChar = s.charAt(left);
            window.merge(leftChar, -1, Integer::sum);
            if (need.containsKey(leftChar) && window.get(leftChar) < need.get(leftChar)) {
                formed--;
            }
            left++;
        }
    }
    return minLen == Integer.MAX_VALUE ? "" : s.substring(start, start + minLen);
}
```

## 滑动窗口模板

```java
// 滑动窗口通用模板
int left = 0;
for (int right = 0; right < n; right++) {
    // 1. 扩大窗口：加入 right 元素
    window.add(arr[right]);

    // 2. 收缩窗口：当窗口不满足条件时
    while (窗口需要收缩) {
        window.remove(arr[left]);
        left++;
    }

    // 3. 更新结果
    result = Math.max(result, right - left + 1);
}
```

## 代码示例

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/twopointers/](../../../code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/twopointers/)

## 常见面试题

### Q1: 滑动窗口的适用场景和模板？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：滑动窗口适用于"连续子数组/子串"类问题。模板：右指针扩大窗口，当窗口满足/不满足条件时收缩左指针。关键是确定何时收缩以及如何更新结果。

**深入追问**：
- 滑动窗口和双指针的关系？（滑动窗口是双指针的一种特殊形式，两个指针同向移动）
- 如何判断一道题能否用滑动窗口？（连续子序列 + 单调性条件）

## 参考资料

- [LeetCode 15. 三数之和](https://leetcode.cn/problems/3sum/)
- [LeetCode 3. 无重复字符的最长子串](https://leetcode.cn/problems/longest-substring-without-repeating-characters/)
- [LeetCode 76. 最小覆盖子串](https://leetcode.cn/problems/minimum-window-substring/)

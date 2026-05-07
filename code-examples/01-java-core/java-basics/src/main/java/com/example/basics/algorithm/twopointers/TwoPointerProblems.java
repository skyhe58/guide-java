package com.example.basics.algorithm.twopointers;

import java.util.*;

/**
 * 双指针与滑动窗口经典算法题
 * 包含：三数之和、盛最多水的容器、无重复字符的最长子串
 *
 * @see <a href="https://leetcode.cn/problems/3sum/">LeetCode 15</a>
 * @see <a href="https://leetcode.cn/problems/container-with-most-water/">LeetCode 11</a>
 * @see <a href="https://leetcode.cn/problems/longest-substring-without-repeating-characters/">LeetCode 3</a>
 */
public class TwoPointerProblems {

    // ==================== LC 15: 三数之和 ====================

    /**
     * 三数之和 — 排序 + 双指针
     * 时间复杂度: O(n²)，空间复杂度: O(1)（不计输出）
     */
    public static List<List<Integer>> threeSum(int[] nums) {
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
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    left++;
                    right--;
                } else if (sum < 0) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        return result;
    }

    // ==================== LC 11: 盛最多水的容器 ====================

    /**
     * 盛最多水的容器 — 对撞指针
     * 每次移动较短的那一边
     * 时间复杂度: O(n)，空间复杂度: O(1)
     */
    public static int maxArea(int[] height) {
        int left = 0, right = height.length - 1;
        int maxWater = 0;
        while (left < right) {
            int area = Math.min(height[left], height[right]) * (right - left);
            maxWater = Math.max(maxWater, area);
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        return maxWater;
    }

    // ==================== LC 3: 无重复字符的最长子串 ====================

    /**
     * 无重复字符的最长子串 — 滑动窗口
     * 时间复杂度: O(n)，空间复杂度: O(min(n, 字符集大小))
     */
    public static int lengthOfLongestSubstring(String s) {
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

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 三数之和
        System.out.println("=== 三数之和 (LC 15) ===");
        int[] nums = {-1, 0, 1, 2, -1, -4};
        System.out.println("输入: " + Arrays.toString(nums));
        System.out.println("结果: " + threeSum(nums));
        // [[-1, -1, 2], [-1, 0, 1]]

        // 2. 盛最多水的容器
        System.out.println("\n=== 盛最多水的容器 (LC 11) ===");
        int[] height = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("高度: " + Arrays.toString(height));
        System.out.println("最大面积: " + maxArea(height)); // 49

        // 3. 无重复字符的最长子串
        System.out.println("\n=== 无重复字符的最长子串 (LC 3) ===");
        String[] testCases = {"abcabcbb", "bbbbb", "pwwkew"};
        for (String s : testCases) {
            System.out.println("\"" + s + "\" -> 最长子串长度: " + lengthOfLongestSubstring(s));
        }
        // "abcabcbb" -> 3, "bbbbb" -> 1, "pwwkew" -> 3
    }
}

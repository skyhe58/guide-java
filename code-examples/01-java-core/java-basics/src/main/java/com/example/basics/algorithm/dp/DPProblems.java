package com.example.basics.algorithm.dp;

import java.util.*;

/**
 * 动态规划经典算法题
 * 包含：爬楼梯、最长递增子序列、0-1 背包、编辑距离
 *
 * @see <a href="https://leetcode.cn/problems/climbing-stairs/">LeetCode 70</a>
 * @see <a href="https://leetcode.cn/problems/longest-increasing-subsequence/">LeetCode 300</a>
 * @see <a href="https://leetcode.cn/problems/edit-distance/">LeetCode 72</a>
 */
public class DPProblems {

    // ==================== LC 70: 爬楼梯 ====================

    /**
     * 爬楼梯 — 空间优化版
     * dp[i] = dp[i-1] + dp[i-2]
     * 时间: O(n)，空间: O(1)
     */
    public static int climbStairs(int n) {
        if (n <= 2) return n;
        int prev2 = 1, prev1 = 2;
        for (int i = 3; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        return prev1;
    }

    // ==================== LC 300: 最长递增子序列 ====================

    /**
     * 最长递增子序列 — DP
     * dp[i] = 以 nums[i] 结尾的 LIS 长度
     * 时间: O(n²)，空间: O(n)
     */
    public static int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        int maxLen = 1;
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }
        return maxLen;
    }

    /**
     * 最长递增子序列 — 贪心 + 二分（优化版）
     * 时间: O(n log n)
     */
    public static int lengthOfLISOptimized(int[] nums) {
        List<Integer> tails = new ArrayList<>();
        for (int num : nums) {
            int pos = Collections.binarySearch(tails, num);
            if (pos < 0) pos = -(pos + 1);
            if (pos == tails.size()) tails.add(num);
            else tails.set(pos, num);
        }
        return tails.size();
    }

    // ==================== 0-1 背包问题 ====================

    /**
     * 0-1 背包 — 一维空间优化
     * 每件物品只能选一次，逆序遍历容量
     * 时间: O(N*W)，空间: O(W)
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];
        for (int i = 0; i < weights.length; i++) {
            // 逆序遍历，保证每件物品只选一次
            for (int j = capacity; j >= weights[i]; j--) {
                dp[j] = Math.max(dp[j], dp[j - weights[i]] + values[i]);
            }
        }
        return dp[capacity];
    }

    // ==================== LC 72: 编辑距离 ====================

    /**
     * 编辑距离
     * dp[i][j] = word1 前 i 个字符转换为 word2 前 j 个字符的最少操作数
     * 时间: O(m*n)，空间: O(m*n)
     */
    public static int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        // 初始化：空串到另一个串的编辑距离
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // 字符相同，无需操作
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], // 替换
                                    Math.min(dp[i - 1][j],      // 删除
                                             dp[i][j - 1]));    // 插入
                }
            }
        }
        return dp[m][n];
    }

    // ==================== LC 1143: 最长公共子序列 ====================

    /**
     * 最长公共子序列
     * 时间: O(m*n)，空间: O(m*n)
     */
    public static int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    // ==================== LC 322: 零钱兑换 ====================

    /**
     * 零钱兑换 — 完全背包
     * dp[i] = 凑出金额 i 所需的最少硬币数
     * 时间: O(amount * n)，空间: O(amount)
     */
    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        return dp[amount] > amount ? -1 : dp[amount];
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 爬楼梯
        System.out.println("=== 爬楼梯 (LC 70) ===");
        for (int n = 1; n <= 5; n++) {
            System.out.println("n=" + n + " -> " + climbStairs(n) + " 种方法");
        }

        // 2. 最长递增子序列
        System.out.println("\n=== 最长递增子序列 (LC 300) ===");
        int[] nums = {10, 9, 2, 5, 3, 7, 101, 18};
        System.out.println("数组: " + Arrays.toString(nums));
        System.out.println("LIS 长度（DP）: " + lengthOfLIS(nums));           // 4
        System.out.println("LIS 长度（贪心+二分）: " + lengthOfLISOptimized(nums)); // 4

        // 3. 0-1 背包
        System.out.println("\n=== 0-1 背包 ===");
        int[] weights = {2, 3, 4, 5};
        int[] values = {3, 4, 5, 6};
        int capacity = 8;
        System.out.println("物品重量: " + Arrays.toString(weights));
        System.out.println("物品价值: " + Arrays.toString(values));
        System.out.println("背包容量: " + capacity);
        System.out.println("最大价值: " + knapsack(weights, values, capacity)); // 10

        // 4. 编辑距离
        System.out.println("\n=== 编辑距离 (LC 72) ===");
        System.out.println("horse -> ros: " + minDistance("horse", "ros"));       // 3
        System.out.println("intention -> execution: " + minDistance("intention", "execution")); // 5

        // 5. 最长公共子序列
        System.out.println("\n=== 最长公共子序列 (LC 1143) ===");
        System.out.println("abcde, ace -> " + longestCommonSubsequence("abcde", "ace")); // 3

        // 6. 零钱兑换
        System.out.println("\n=== 零钱兑换 (LC 322) ===");
        int[] coins = {1, 2, 5};
        System.out.println("coins=" + Arrays.toString(coins) + ", amount=11 -> " + coinChange(coins, 11)); // 3
    }
}

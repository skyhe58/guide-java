package com.example.basics.algorithm.dp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态规划算法正确性验证
 */
@DisplayName("动态规划测试")
class DPTest {

    // ==================== 爬楼梯 ====================

    @Test
    @DisplayName("爬楼梯 — 基础用例")
    void climbStairs_basic() {
        assertEquals(1, DPProblems.climbStairs(1));
        assertEquals(2, DPProblems.climbStairs(2));
        assertEquals(3, DPProblems.climbStairs(3));
        assertEquals(5, DPProblems.climbStairs(4));
        assertEquals(8, DPProblems.climbStairs(5));
    }

    // ==================== 最长递增子序列 ====================

    @Test
    @DisplayName("LIS — 标准用例")
    void lengthOfLIS_standard() {
        assertEquals(4, DPProblems.lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));
    }

    @Test
    @DisplayName("LIS — 全递增")
    void lengthOfLIS_allIncreasing() {
        assertEquals(4, DPProblems.lengthOfLIS(new int[]{1, 2, 3, 4}));
    }

    @Test
    @DisplayName("LIS — 全递减")
    void lengthOfLIS_allDecreasing() {
        assertEquals(1, DPProblems.lengthOfLIS(new int[]{4, 3, 2, 1}));
    }

    @Test
    @DisplayName("LIS 优化版 — 与 DP 版结果一致")
    void lengthOfLIS_optimizedMatchesDP() {
        int[] nums = {10, 9, 2, 5, 3, 7, 101, 18};
        assertEquals(DPProblems.lengthOfLIS(nums), DPProblems.lengthOfLISOptimized(nums));
    }

    // ==================== 0-1 背包 ====================

    @Test
    @DisplayName("0-1 背包 — 标准用例")
    void knapsack_standard() {
        int[] weights = {2, 3, 4, 5};
        int[] values = {3, 4, 5, 6};
        assertEquals(10, DPProblems.knapsack(weights, values, 8));
    }

    @Test
    @DisplayName("0-1 背包 — 容量为 0")
    void knapsack_zeroCapacity() {
        int[] weights = {1, 2};
        int[] values = {3, 4};
        assertEquals(0, DPProblems.knapsack(weights, values, 0));
    }

    @Test
    @DisplayName("0-1 背包 — 所有物品都能装下")
    void knapsack_allFit() {
        int[] weights = {1, 2, 3};
        int[] values = {6, 10, 12};
        assertEquals(28, DPProblems.knapsack(weights, values, 10));
    }

    // ==================== 编辑距离 ====================

    @Test
    @DisplayName("编辑距离 — 标准用例")
    void minDistance_standard() {
        assertEquals(3, DPProblems.minDistance("horse", "ros"));
        assertEquals(5, DPProblems.minDistance("intention", "execution"));
    }

    @Test
    @DisplayName("编辑距离 — 空串")
    void minDistance_emptyString() {
        assertEquals(0, DPProblems.minDistance("", ""));
        assertEquals(3, DPProblems.minDistance("abc", ""));
        assertEquals(3, DPProblems.minDistance("", "abc"));
    }

    @Test
    @DisplayName("编辑距离 — 相同字符串")
    void minDistance_sameString() {
        assertEquals(0, DPProblems.minDistance("abc", "abc"));
    }

    // ==================== 最长公共子序列 ====================

    @Test
    @DisplayName("LCS — 标准用例")
    void longestCommonSubsequence_standard() {
        assertEquals(3, DPProblems.longestCommonSubsequence("abcde", "ace"));
        assertEquals(3, DPProblems.longestCommonSubsequence("abc", "abc"));
        assertEquals(0, DPProblems.longestCommonSubsequence("abc", "def"));
    }

    // ==================== 零钱兑换 ====================

    @Test
    @DisplayName("零钱兑换 — 标准用例")
    void coinChange_standard() {
        assertEquals(3, DPProblems.coinChange(new int[]{1, 2, 5}, 11));
        assertEquals(0, DPProblems.coinChange(new int[]{1}, 0));
        assertEquals(-1, DPProblems.coinChange(new int[]{2}, 3));
    }

    @Test
    @DisplayName("零钱兑换 — 只有一种硬币")
    void coinChange_singleCoin() {
        assertEquals(3, DPProblems.coinChange(new int[]{5}, 15));
        assertEquals(-1, DPProblems.coinChange(new int[]{5}, 13));
    }
}

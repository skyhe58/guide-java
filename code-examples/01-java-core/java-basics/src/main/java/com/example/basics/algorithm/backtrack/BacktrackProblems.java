package com.example.basics.algorithm.backtrack;

import java.util.*;

/**
 * 回溯算法经典题目
 * 包含：全排列、子集、N 皇后
 *
 * @see <a href="https://leetcode.cn/problems/permutations/">LeetCode 46</a>
 * @see <a href="https://leetcode.cn/problems/subsets/">LeetCode 78</a>
 * @see <a href="https://leetcode.cn/problems/n-queens/">LeetCode 51</a>
 */
public class BacktrackProblems {

    // ==================== LC 46: 全排列 ====================

    /**
     * 全排列
     * 时间复杂度: O(n * n!)，空间复杂度: O(n)
     */
    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] used = new boolean[nums.length];
        backtrackPermute(nums, used, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackPermute(int[] nums, boolean[] used,
                                          List<Integer> path, List<List<Integer>> result) {
        if (path.size() == nums.length) {
            result.add(new ArrayList<>(path)); // 注意要拷贝
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue; // 跳过已使用的元素
            used[i] = true;
            path.add(nums[i]);
            backtrackPermute(nums, used, path, result);
            path.remove(path.size() - 1); // 回溯：撤销选择
            used[i] = false;
        }
    }

    // ==================== LC 78: 子集 ====================

    /**
     * 子集
     * 时间复杂度: O(n * 2^n)
     */
    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackSubsets(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackSubsets(int[] nums, int start,
                                          List<Integer> path, List<List<Integer>> result) {
        result.add(new ArrayList<>(path)); // 每个节点都是一个子集
        for (int i = start; i < nums.length; i++) {
            path.add(nums[i]);
            backtrackSubsets(nums, i + 1, path, result);
            path.remove(path.size() - 1); // 回溯
        }
    }

    // ==================== LC 39: 组合总和 ====================

    /**
     * 组合总和 — 元素可重复使用
     */
    public static List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates); // 排序方便剪枝
        backtrackCombination(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackCombination(int[] candidates, int remain, int start,
                                              List<Integer> path, List<List<Integer>> result) {
        if (remain == 0) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (int i = start; i < candidates.length; i++) {
            if (candidates[i] > remain) break; // 剪枝：后面的更大，不可能满足
            path.add(candidates[i]);
            backtrackCombination(candidates, remain - candidates[i], i, path, result);
            path.remove(path.size() - 1);
        }
    }

    // ==================== LC 51: N 皇后 ====================

    /**
     * N 皇后
     * 时间复杂度: O(n!)
     */
    public static List<List<String>> solveNQueens(int n) {
        List<List<String>> result = new ArrayList<>();
        int[] queens = new int[n]; // queens[i] = 第 i 行皇后所在的列
        Arrays.fill(queens, -1);
        Set<Integer> cols = new HashSet<>();      // 已占用的列
        Set<Integer> diag1 = new HashSet<>();     // 已占用的主对角线 (row - col)
        Set<Integer> diag2 = new HashSet<>();     // 已占用的副对角线 (row + col)
        backtrackQueens(n, 0, queens, cols, diag1, diag2, result);
        return result;
    }

    private static void backtrackQueens(int n, int row, int[] queens,
                                         Set<Integer> cols, Set<Integer> diag1, Set<Integer> diag2,
                                         List<List<String>> result) {
        if (row == n) {
            result.add(buildBoard(queens, n));
            return;
        }
        for (int col = 0; col < n; col++) {
            // 剪枝：列或对角线冲突
            if (cols.contains(col) || diag1.contains(row - col) || diag2.contains(row + col)) {
                continue;
            }
            queens[row] = col;
            cols.add(col);
            diag1.add(row - col);
            diag2.add(row + col);
            backtrackQueens(n, row + 1, queens, cols, diag1, diag2, result);
            // 回溯
            queens[row] = -1;
            cols.remove(col);
            diag1.remove(row - col);
            diag2.remove(row + col);
        }
    }

    private static List<String> buildBoard(int[] queens, int n) {
        List<String> board = new ArrayList<>();
        for (int queen : queens) {
            char[] row = new char[n];
            Arrays.fill(row, '.');
            row[queen] = 'Q';
            board.add(new String(row));
        }
        return board;
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 全排列
        System.out.println("=== 全排列 (LC 46) ===");
        int[] nums1 = {1, 2, 3};
        System.out.println("输入: " + Arrays.toString(nums1));
        System.out.println("全排列: " + permute(nums1));

        // 2. 子集
        System.out.println("\n=== 子集 (LC 78) ===");
        int[] nums2 = {1, 2, 3};
        System.out.println("输入: " + Arrays.toString(nums2));
        System.out.println("子集: " + subsets(nums2));

        // 3. 组合总和
        System.out.println("\n=== 组合总和 (LC 39) ===");
        int[] candidates = {2, 3, 6, 7};
        System.out.println("candidates=" + Arrays.toString(candidates) + ", target=7");
        System.out.println("结果: " + combinationSum(candidates, 7));

        // 4. N 皇后
        System.out.println("\n=== N 皇后 (LC 51) ===");
        List<List<String>> solutions = solveNQueens(4);
        System.out.println("4 皇后解的数量: " + solutions.size());
        for (int i = 0; i < solutions.size(); i++) {
            System.out.println("解 " + (i + 1) + ":");
            for (String row : solutions.get(i)) {
                System.out.println("  " + row);
            }
        }
    }
}

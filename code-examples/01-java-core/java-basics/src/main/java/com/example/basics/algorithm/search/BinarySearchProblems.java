package com.example.basics.algorithm.search;

import java.util.Arrays;

/**
 * 二分查找经典算法题
 * 包含：基础二分、旋转数组搜索
 *
 * @see <a href="https://leetcode.cn/problems/binary-search/">LeetCode 704</a>
 * @see <a href="https://leetcode.cn/problems/search-in-rotated-sorted-array/">LeetCode 33</a>
 */
public class BinarySearchProblems {

    // ==================== LC 704: 基础二分查找 ====================

    /**
     * 基础二分查找
     * 时间复杂度: O(log n)，空间复杂度: O(1)
     */
    public static int search(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2; // 防止整数溢出
            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }

    // ==================== LC 33: 搜索旋转排序数组 ====================

    /**
     * 搜索旋转排序数组
     * 二分后至少有一半是有序的，判断 target 在有序的那一半还是无序的那一半
     * 时间复杂度: O(log n)
     */
    public static int searchRotated(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) return mid;

            if (nums[left] <= nums[mid]) {
                // 左半部分有序
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                // 右半部分有序
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return -1;
    }

    // ==================== LC 34: 查找第一个和最后一个位置 ====================

    /**
     * 查找排序数组中目标值的起始和结束位置
     * 时间复杂度: O(log n)
     */
    public static int[] searchRange(int[] nums, int target) {
        return new int[]{findFirst(nums, target), findLast(nums, target)};
    }

    private static int findFirst(int[] nums, int target) {
        int left = 0, right = nums.length - 1, result = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                result = mid;
                right = mid - 1; // 继续向左找第一个
            } else if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return result;
    }

    private static int findLast(int[] nums, int target) {
        int left = 0, right = nums.length - 1, result = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                result = mid;
                left = mid + 1; // 继续向右找最后一个
            } else if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return result;
    }

    // ==================== LC 35: 搜索插入位置 ====================

    /**
     * 搜索插入位置 — 找到第一个 >= target 的位置
     * 时间复杂度: O(log n)
     */
    public static int searchInsert(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] >= target) right = mid - 1;
            else left = mid + 1;
        }
        return left;
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 基础二分查找
        System.out.println("=== 基础二分查找 (LC 704) ===");
        int[] sorted = {-1, 0, 3, 5, 9, 12};
        System.out.println("数组: " + Arrays.toString(sorted));
        System.out.println("查找 9: index = " + search(sorted, 9));   // 4
        System.out.println("查找 2: index = " + search(sorted, 2));   // -1

        // 2. 搜索旋转排序数组
        System.out.println("\n=== 搜索旋转排序数组 (LC 33) ===");
        int[] rotated = {4, 5, 6, 7, 0, 1, 2};
        System.out.println("数组: " + Arrays.toString(rotated));
        System.out.println("查找 0: index = " + searchRotated(rotated, 0)); // 4
        System.out.println("查找 3: index = " + searchRotated(rotated, 3)); // -1

        // 3. 查找第一个和最后一个位置
        System.out.println("\n=== 查找第一个和最后一个位置 (LC 34) ===");
        int[] nums = {5, 7, 7, 8, 8, 10};
        System.out.println("数组: " + Arrays.toString(nums));
        System.out.println("查找 8: " + Arrays.toString(searchRange(nums, 8)));  // [3, 4]
        System.out.println("查找 6: " + Arrays.toString(searchRange(nums, 6)));  // [-1, -1]

        // 4. 搜索插入位置
        System.out.println("\n=== 搜索插入位置 (LC 35) ===");
        int[] arr = {1, 3, 5, 6};
        System.out.println("数组: " + Arrays.toString(arr));
        System.out.println("插入 5: index = " + searchInsert(arr, 5)); // 2
        System.out.println("插入 2: index = " + searchInsert(arr, 2)); // 1
        System.out.println("插入 7: index = " + searchInsert(arr, 7)); // 4
    }
}

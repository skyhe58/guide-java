package com.example.basics.algorithm.heap;

import java.util.*;

/**
 * 堆（优先队列）经典算法题
 * 包含：TopK 问题、合并 K 个有序链表
 *
 * @see <a href="https://leetcode.cn/problems/kth-largest-element-in-an-array/">LeetCode 215</a>
 * @see <a href="https://leetcode.cn/problems/merge-k-sorted-lists/">LeetCode 23</a>
 */
public class HeapProblems {

    /** 链表节点（用于合并 K 个链表） */
    public static class ListNode {
        public int val;
        public ListNode next;

        public ListNode(int val) { this.val = val; }

        public ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    // ==================== LC 215: 第 K 大元素 ====================

    /**
     * 第 K 大元素 — 最小堆
     * 维护大小为 k 的最小堆，堆顶即为第 K 大
     * 时间复杂度: O(n log k)，空间复杂度: O(k)
     */
    public static int findKthLargest(int[] nums, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
        for (int num : nums) {
            minHeap.offer(num);
            if (minHeap.size() > k) {
                minHeap.poll(); // 弹出最小的，保留最大的 k 个
            }
        }
        return minHeap.peek();
    }

    /**
     * 第 K 大元素 — 快速选择
     * 平均时间复杂度: O(n)，最坏 O(n²)
     */
    public static int findKthLargestQuickSelect(int[] nums, int k) {
        int target = nums.length - k;
        return quickSelect(nums, 0, nums.length - 1, target);
    }

    private static int quickSelect(int[] nums, int left, int right, int target) {
        int pivot = partition(nums, left, right);
        if (pivot == target) return nums[pivot];
        else if (pivot < target) return quickSelect(nums, pivot + 1, right, target);
        else return quickSelect(nums, left, pivot - 1, target);
    }

    private static int partition(int[] nums, int left, int right) {
        // 随机选择 pivot 避免最坏情况
        int randomIdx = left + new Random().nextInt(right - left + 1);
        swap(nums, randomIdx, right);

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

    private static void swap(int[] nums, int i, int j) {
        int tmp = nums[i];
        nums[i] = nums[j];
        nums[j] = tmp;
    }

    // ==================== LC 23: 合并 K 个升序链表 ====================

    /**
     * 合并 K 个升序链表 — 最小堆
     * 时间复杂度: O(N log k)，N 为总节点数，k 为链表数
     */
    public static ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;
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

    // ==================== 工具方法 ====================

    /** 从数组创建链表 */
    public static ListNode createList(int... values) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        for (int val : values) {
            curr.next = new ListNode(val);
            curr = curr.next;
        }
        return dummy.next;
    }

    /** 链表转字符串 */
    public static String listToString(ListNode head) {
        StringBuilder sb = new StringBuilder("[");
        while (head != null) {
            sb.append(head.val);
            if (head.next != null) sb.append(", ");
            head = head.next;
        }
        return sb.append("]").toString();
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 第 K 大元素
        System.out.println("=== 第 K 大元素 (LC 215) ===");
        int[] nums = {3, 2, 1, 5, 6, 4};
        System.out.println("数组: " + Arrays.toString(nums));
        System.out.println("第 2 大元素（最小堆）: " + findKthLargest(nums, 2)); // 5
        System.out.println("第 2 大元素（快速选择）: " + findKthLargestQuickSelect(
            Arrays.copyOf(nums, nums.length), 2)); // 5

        // 2. 合并 K 个升序链表
        System.out.println("\n=== 合并 K 个升序链表 (LC 23) ===");
        ListNode[] lists = {
            createList(1, 4, 5),
            createList(1, 3, 4),
            createList(2, 6)
        };
        for (int i = 0; i < lists.length; i++) {
            System.out.println("链表" + (i + 1) + ": " + listToString(lists[i]));
        }
        ListNode merged = mergeKLists(lists);
        System.out.println("合并后: " + listToString(merged));
        // 输出: [1, 1, 2, 3, 4, 4, 5, 6]
    }
}

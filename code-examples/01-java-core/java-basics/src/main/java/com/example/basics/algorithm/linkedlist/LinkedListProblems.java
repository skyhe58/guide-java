package com.example.basics.algorithm.linkedlist;

import java.util.*;

/**
 * 链表经典算法题
 * 包含：反转链表、合并有序链表、环形链表检测
 *
 * @see <a href="https://leetcode.cn/problems/reverse-linked-list/">LeetCode 206</a>
 * @see <a href="https://leetcode.cn/problems/merge-two-sorted-lists/">LeetCode 21</a>
 * @see <a href="https://leetcode.cn/problems/linked-list-cycle/">LeetCode 141</a>
 */
public class LinkedListProblems {

    /**
     * 链表节点定义
     */
    public static class ListNode {
        public int val;
        public ListNode next;

        public ListNode(int val) {
            this.val = val;
        }

        public ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
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

    /** 链表转为列表（方便测试） */
    public static List<Integer> toList(ListNode head) {
        List<Integer> result = new ArrayList<>();
        while (head != null) {
            result.add(head.val);
            head = head.next;
        }
        return result;
    }

    // ==================== LC 206: 反转链表 ====================

    /**
     * 迭代法反转链表
     * 时间复杂度: O(n)，空间复杂度: O(1)
     */
    public static ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;
        while (curr != null) {
            ListNode next = curr.next; // 暂存下一个节点
            curr.next = prev;          // 反转指针
            prev = curr;               // prev 前进
            curr = next;               // curr 前进
        }
        return prev;
    }

    /**
     * 递归法反转链表
     * 时间复杂度: O(n)，空间复杂度: O(n) 递归栈
     */
    public static ListNode reverseListRecursive(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode newHead = reverseListRecursive(head.next);
        head.next.next = head; // 让下一个节点指向自己
        head.next = null;       // 断开原来的指向
        return newHead;
    }

    // ==================== LC 21: 合并两个有序链表 ====================

    /**
     * 合并两个有序链表 — 迭代法
     * 时间复杂度: O(m+n)，空间复杂度: O(1)
     */
    public static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(-1);
        ListNode curr = dummy;
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                curr.next = l1;
                l1 = l1.next;
            } else {
                curr.next = l2;
                l2 = l2.next;
            }
            curr = curr.next;
        }
        curr.next = (l1 != null) ? l1 : l2;
        return dummy.next;
    }

    // ==================== LC 141: 环形链表检测 ====================

    /**
     * 判断链表是否有环 — 快慢指针
     * 时间复杂度: O(n)，空间复杂度: O(1)
     */
    public static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;
        }
        return false;
    }

    /**
     * LC 142: 找到环的入口节点
     * 快慢指针相遇后，一个从 head 出发，一个从相遇点出发，再次相遇即为入口
     */
    public static ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                ListNode p = head;
                while (p != slow) {
                    p = p.next;
                    slow = slow.next;
                }
                return p;
            }
        }
        return null;
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 反转链表
        System.out.println("=== 反转链表 (LC 206) ===");
        ListNode list1 = createList(1, 2, 3, 4, 5);
        System.out.println("原链表: " + toList(list1));
        ListNode reversed = reverseList(list1);
        System.out.println("反转后: " + toList(reversed));

        // 2. 合并有序链表
        System.out.println("\n=== 合并两个有序链表 (LC 21) ===");
        ListNode l1 = createList(1, 2, 4);
        ListNode l2 = createList(1, 3, 4);
        System.out.println("链表1: " + toList(l1));
        System.out.println("链表2: " + toList(l2));
        ListNode merged = mergeTwoLists(l1, l2);
        System.out.println("合并后: " + toList(merged));

        // 3. 环形链表检测
        System.out.println("\n=== 环形链表检测 (LC 141) ===");
        ListNode noLoop = createList(1, 2, 3);
        System.out.println("无环链表: hasCycle = " + hasCycle(noLoop));

        // 构造有环链表: 1 -> 2 -> 3 -> 4 -> 2 (环)
        ListNode n1 = new ListNode(1);
        ListNode n2 = new ListNode(2);
        ListNode n3 = new ListNode(3);
        ListNode n4 = new ListNode(4);
        n1.next = n2;
        n2.next = n3;
        n3.next = n4;
        n4.next = n2; // 形成环
        System.out.println("有环链表: hasCycle = " + hasCycle(n1));

        ListNode cycleEntry = detectCycle(n1);
        System.out.println("环入口节点值: " + (cycleEntry != null ? cycleEntry.val : "null"));
    }
}

package com.example.basics.algorithm.hashtable;

import java.util.*;

/**
 * 哈希表经典算法题
 * 包含：两数之和、字母异位词分组、LRU 缓存实现
 *
 * @see <a href="https://leetcode.cn/problems/two-sum/">LeetCode 1</a>
 * @see <a href="https://leetcode.cn/problems/group-anagrams/">LeetCode 49</a>
 * @see <a href="https://leetcode.cn/problems/lru-cache/">LeetCode 146</a>
 */
public class HashTableProblems {

    // ==================== LC 1: 两数之和 ====================

    /**
     * 两数之和 — 哈希表一次遍历
     * 时间复杂度: O(n)，空间复杂度: O(n)
     */
    public static int[] twoSum(int[] nums, int target) {
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

    // ==================== LC 49: 字母异位词分组 ====================

    /**
     * 字母异位词分组 — 排序后的字符串作为 key
     * 时间复杂度: O(n * k * log k)，k 为字符串最大长度
     */
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();
        for (String s : strs) {
            char[] chars = s.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(map.values());
    }

    // ==================== LC 146: LRU 缓存 ====================

    /**
     * LRU 缓存 — HashMap + 双向链表
     * get 和 put 均为 O(1)
     */
    public static class LRUCache {
        /** 双向链表节点 */
        static class Node {
            int key, value;
            Node prev, next;

            Node(int key, int value) {
                this.key = key;
                this.value = value;
            }
        }

        private final int capacity;
        private final Map<Integer, Node> map;
        private final Node head, tail; // 虚拟头尾节点

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

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 两数之和
        System.out.println("=== 两数之和 (LC 1) ===");
        int[] nums = {2, 7, 11, 15};
        int[] result = twoSum(nums, 9);
        System.out.println("nums=" + Arrays.toString(nums) + ", target=9");
        System.out.println("结果: " + Arrays.toString(result)); // [0, 1]

        // 2. 字母异位词分组
        System.out.println("\n=== 字母异位词分组 (LC 49) ===");
        String[] strs = {"eat", "tea", "tan", "ate", "nat", "bat"};
        List<List<String>> groups = groupAnagrams(strs);
        System.out.println("输入: " + Arrays.toString(strs));
        System.out.println("分组: " + groups);

        // 3. LRU 缓存
        System.out.println("\n=== LRU 缓存 (LC 146) ===");
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println("get(1) = " + cache.get(1));  // 1
        cache.put(3, 3);  // 淘汰 key=2
        System.out.println("get(2) = " + cache.get(2));  // -1（已淘汰）
        cache.put(4, 4);  // 淘汰 key=1（因为 key=1 最近被访问过，淘汰的是 key=3）
        System.out.println("get(1) = " + cache.get(1));  // -1
        System.out.println("get(3) = " + cache.get(3));  // -1
        System.out.println("get(4) = " + cache.get(4));  // 4
    }
}

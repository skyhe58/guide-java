package com.example.basics.algorithm.tree;

import java.util.*;

/**
 * 二叉树经典算法题
 * 包含：前中后序遍历、层序遍历、最大深度、翻转二叉树
 *
 * @see <a href="https://leetcode.cn/problems/binary-tree-inorder-traversal/">LeetCode 94</a>
 * @see <a href="https://leetcode.cn/problems/binary-tree-level-order-traversal/">LeetCode 102</a>
 * @see <a href="https://leetcode.cn/problems/maximum-depth-of-binary-tree/">LeetCode 104</a>
 * @see <a href="https://leetcode.cn/problems/invert-binary-tree/">LeetCode 226</a>
 */
public class TreeProblems {

    /**
     * 二叉树节点定义
     */
    public static class TreeNode {
        public int val;
        public TreeNode left;
        public TreeNode right;

        public TreeNode(int val) {
            this.val = val;
        }

        public TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    // ==================== 前中后序遍历 ====================

    /** 前序遍历：根 → 左 → 右 (LC 144) */
    public static List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    private static void preorder(TreeNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.val);
        preorder(node.left, result);
        preorder(node.right, result);
    }

    /** 中序遍历：左 → 根 → 右 (LC 94) */
    public static List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    private static void inorder(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorder(node.left, result);
        result.add(node.val);
        inorder(node.right, result);
    }

    /** 后序遍历：左 → 右 → 根 (LC 145) */
    public static List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    private static void postorder(TreeNode node, List<Integer> result) {
        if (node == null) return;
        postorder(node.left, result);
        postorder(node.right, result);
        result.add(node.val);
    }

    // ==================== LC 102: 层序遍历 ====================

    /**
     * 层序遍历 — BFS
     * 时间复杂度: O(n)，空间复杂度: O(n)
     */
    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(level);
        }
        return result;
    }

    // ==================== LC 104: 最大深度 ====================

    /**
     * 二叉树的最大深度 — 递归
     * 时间复杂度: O(n)，空间复杂度: O(h)
     */
    public static int maxDepth(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    // ==================== LC 226: 翻转二叉树 ====================

    /**
     * 翻转二叉树 — 递归
     * 时间复杂度: O(n)，空间复杂度: O(h)
     */
    public static TreeNode invertTree(TreeNode root) {
        if (root == null) return null;
        TreeNode left = invertTree(root.left);
        TreeNode right = invertTree(root.right);
        root.left = right;
        root.right = left;
        return root;
    }

    // ==================== LC 98: 验证 BST ====================

    /**
     * 验证二叉搜索树 — 递归传递上下界
     */
    public static boolean isValidBST(TreeNode root) {
        return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean validate(TreeNode node, long min, long max) {
        if (node == null) return true;
        if (node.val <= min || node.val >= max) return false;
        return validate(node.left, min, node.val)
            && validate(node.right, node.val, max);
    }

    // ==================== LC 236: 最近公共祖先 ====================

    /**
     * 二叉树的最近公共祖先（LCA）
     * 时间复杂度: O(n)，空间复杂度: O(h)
     */
    public static TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if (left != null && right != null) return root;
        return left != null ? left : right;
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        /*
         * 构建测试二叉树:
         *       1
         *      / \
         *     2   3
         *    / \
         *   4   5
         */
        TreeNode root = new TreeNode(1,
            new TreeNode(2, new TreeNode(4), new TreeNode(5)),
            new TreeNode(3));

        // 1. 遍历
        System.out.println("=== 二叉树遍历 ===");
        System.out.println("前序遍历: " + preorderTraversal(root));  // [1, 2, 4, 5, 3]
        System.out.println("中序遍历: " + inorderTraversal(root));   // [4, 2, 5, 1, 3]
        System.out.println("后序遍历: " + postorderTraversal(root)); // [4, 5, 2, 3, 1]

        // 2. 层序遍历
        System.out.println("\n=== 层序遍历 (LC 102) ===");
        System.out.println("层序: " + levelOrder(root)); // [[1], [2, 3], [4, 5]]

        // 3. 最大深度
        System.out.println("\n=== 最大深度 (LC 104) ===");
        System.out.println("最大深度: " + maxDepth(root)); // 3

        // 4. 翻转二叉树
        System.out.println("\n=== 翻转二叉树 (LC 226) ===");
        TreeNode inverted = invertTree(root);
        System.out.println("翻转后层序: " + levelOrder(inverted)); // [[1], [3, 2], [5, 4]]

        // 5. 验证 BST
        System.out.println("\n=== 验证 BST (LC 98) ===");
        TreeNode bst = new TreeNode(2, new TreeNode(1), new TreeNode(3));
        TreeNode notBst = new TreeNode(5,
            new TreeNode(1),
            new TreeNode(4, new TreeNode(3), new TreeNode(6)));
        System.out.println("BST [2,1,3]: " + isValidBST(bst));       // true
        System.out.println("非 BST [5,1,4,3,6]: " + isValidBST(notBst)); // false
    }
}

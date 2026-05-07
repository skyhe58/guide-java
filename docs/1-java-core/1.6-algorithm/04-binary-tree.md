---
title: "二叉树"
module: "algorithm"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "数据结构"
  - "二叉树"
  - "BST"
  - "递归"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/algorithm/tree/"
relatedEntries:
  - "/1-java-core/1.6-algorithm/05-heap"
  - "/1-java-core/1.6-algorithm/10-backtracking"
prerequisites:
  - "/1-java-core/1.6-algorithm/01-linked-list"
  - "/1-java-core/1.6-algorithm/02-stack-queue"
estimatedTime: "60min"
---

# 二叉树

## 概念说明

二叉树是面试中出现频率最高的数据结构之一。核心技巧：**递归思维**（前中后序遍历）、**BFS 层序遍历**、**BST 性质**。

## 核心题目

### 一、前中后序遍历（LeetCode 144/94/145）🟢 Easy | 🔥🔥🔥

```java
// 前序遍历：根 → 左 → 右
public List<Integer> preorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    preorder(root, result);
    return result;
}
private void preorder(TreeNode node, List<Integer> result) {
    if (node == null) return;
    result.add(node.val);       // 根
    preorder(node.left, result); // 左
    preorder(node.right, result);// 右
}

// 中序遍历：左 → 根 → 右
public List<Integer> inorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    inorder(root, result);
    return result;
}
private void inorder(TreeNode node, List<Integer> result) {
    if (node == null) return;
    inorder(node.left, result);
    result.add(node.val);
    inorder(node.right, result);
}

// 后序遍历：左 → 右 → 根
public List<Integer> postorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    postorder(root, result);
    return result;
}
private void postorder(TreeNode node, List<Integer> result) {
    if (node == null) return;
    postorder(node.left, result);
    postorder(node.right, result);
    result.add(node.val);
}
```

**迭代法（中序遍历为例）**：

```java
public List<Integer> inorderIterative(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode curr = root;
    while (curr != null || !stack.isEmpty()) {
        while (curr != null) {
            stack.push(curr);
            curr = curr.left; // 一路向左
        }
        curr = stack.pop();
        result.add(curr.val); // 访问节点
        curr = curr.right;     // 转向右子树
    }
    return result;
}
```

---

### 二、层序遍历（LeetCode 102）🟡 Medium | 🔥🔥🔥

```java
/**
 * 层序遍历 — BFS
 * 时间复杂度: O(n)，空间复杂度: O(n)
 */
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    while (!queue.isEmpty()) {
        int size = queue.size(); // 当前层的节点数
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
```

---

### 三、二叉树的最大深度（LeetCode 104）🟢 Easy | 🔥🔥🔥

```java
/**
 * 最大深度 — 递归（DFS）
 * 时间复杂度: O(n)，空间复杂度: O(h) h 为树高
 */
public int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```

---

### 四、翻转二叉树（LeetCode 226）🟢 Easy | 🔥🔥

```java
/**
 * 翻转二叉树 — 递归
 */
public TreeNode invertTree(TreeNode root) {
    if (root == null) return null;
    TreeNode left = invertTree(root.left);
    TreeNode right = invertTree(root.right);
    root.left = right;
    root.right = left;
    return root;
}
```

---

### 五、验证二叉搜索树（LeetCode 98）🟡 Medium | 🔥🔥

**BST 性质**：左子树所有节点 < 根 < 右子树所有节点。

```java
/**
 * 验证 BST — 递归传递上下界
 */
public boolean isValidBST(TreeNode root) {
    return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

private boolean validate(TreeNode node, long min, long max) {
    if (node == null) return true;
    if (node.val <= min || node.val >= max) return false;
    return validate(node.left, min, node.val)
        && validate(node.right, node.val, max);
}
```

---

### 六、二叉树的最近公共祖先（LeetCode 236）🟡 Medium | 🔥🔥🔥

```java
/**
 * 最近公共祖先（LCA）
 * 时间复杂度: O(n)，空间复杂度: O(h)
 */
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    if (left != null && right != null) return root; // p 和 q 分别在左右子树
    return left != null ? left : right;
}
```

**思路**：后序遍历，如果左右子树分别找到 p 和 q，则当前节点就是 LCA。

## 代码示例

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/tree/](../../../code-examples/01-java-core/java-basics/src/main/java/com/example/basics/algorithm/tree/)

## 常见面试题

### Q1: 二叉树的前中后序遍历（递归和迭代）

**难度**：⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：递归写法简单直接；迭代写法用栈模拟递归过程。中序迭代最常考。

**深入追问**：
- Morris 遍历了解吗？（O(1) 空间的遍历方法，利用叶子节点的空指针）
- 如何通过前序+中序还原二叉树？（LC 105）

### Q2: 如何找到二叉树中两个节点的最近公共祖先？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：见上方 LC 236 解法。

**深入追问**：
- 如果是 BST，如何优化？（利用 BST 性质，根据值大小决定搜索方向，LC 235）
- 如果节点有父指针呢？（转化为两个链表求交点问题）

## 参考资料

- [LeetCode 102. 二叉树的层序遍历](https://leetcode.cn/problems/binary-tree-level-order-traversal/)
- [LeetCode 236. 二叉树的最近公共祖先](https://leetcode.cn/problems/lowest-common-ancestor-of-a-binary-tree/)

package com.example.basics.algorithm.stack;

import java.util.*;

/**
 * 栈与队列经典算法题
 * 包含：有效括号、最小栈、每日温度
 *
 * @see <a href="https://leetcode.cn/problems/valid-parentheses/">LeetCode 20</a>
 * @see <a href="https://leetcode.cn/problems/min-stack/">LeetCode 155</a>
 * @see <a href="https://leetcode.cn/problems/daily-temperatures/">LeetCode 739</a>
 */
public class StackProblems {

    // ==================== LC 20: 有效的括号 ====================

    /**
     * 有效的括号
     * 遇到左括号压入对应右括号，遇到右括号直接比较
     * 时间复杂度: O(n)，空间复杂度: O(n)
     */
    public static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(') stack.push(')');
            else if (c == '{') stack.push('}');
            else if (c == '[') stack.push(']');
            else if (stack.isEmpty() || stack.pop() != c) return false;
        }
        return stack.isEmpty();
    }

    // ==================== LC 155: 最小栈 ====================

    /**
     * 最小栈 — 辅助栈实现
     * 所有操作 O(1)
     */
    public static class MinStack {
        private final Deque<Integer> stack;
        private final Deque<Integer> minStack;

        public MinStack() {
            stack = new ArrayDeque<>();
            minStack = new ArrayDeque<>();
        }

        public void push(int val) {
            stack.push(val);
            if (minStack.isEmpty() || val <= minStack.peek()) {
                minStack.push(val);
            }
        }

        public void pop() {
            int val = stack.pop();
            if (val == minStack.peek()) {
                minStack.pop();
            }
        }

        public int top() {
            return stack.peek();
        }

        public int getMin() {
            return minStack.peek();
        }
    }

    // ==================== LC 739: 每日温度 ====================

    /**
     * 每日温度 — 单调递减栈
     * 栈中存储下标，维护温度单调递减
     * 时间复杂度: O(n)，空间复杂度: O(n)
     */
    public static int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] result = new int[n];
        Deque<Integer> stack = new ArrayDeque<>(); // 存下标
        for (int i = 0; i < n; i++) {
            // 当前温度高于栈顶温度时，弹出并计算等待天数
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int idx = stack.pop();
                result[idx] = i - idx;
            }
            stack.push(i);
        }
        return result;
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        // 1. 有效的括号
        System.out.println("=== 有效的括号 (LC 20) ===");
        System.out.println("'()[]{}' -> " + isValid("()[]{}"));   // true
        System.out.println("'(]'     -> " + isValid("(]"));       // false
        System.out.println("'([)]'   -> " + isValid("([)]"));     // false
        System.out.println("'{[]}'   -> " + isValid("{[]}"));     // true

        // 2. 最小栈
        System.out.println("\n=== 最小栈 (LC 155) ===");
        MinStack minStack = new MinStack();
        minStack.push(-2);
        minStack.push(0);
        minStack.push(-3);
        System.out.println("getMin: " + minStack.getMin()); // -3
        minStack.pop();
        System.out.println("top:    " + minStack.top());    // 0
        System.out.println("getMin: " + minStack.getMin()); // -2

        // 3. 每日温度
        System.out.println("\n=== 每日温度 (LC 739) ===");
        int[] temps = {73, 74, 75, 71, 69, 72, 76, 73};
        int[] result = dailyTemperatures(temps);
        System.out.println("温度: " + Arrays.toString(temps));
        System.out.println("等待: " + Arrays.toString(result));
        // 输出: [1, 1, 4, 2, 1, 1, 0, 0]
    }
}

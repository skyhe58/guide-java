package com.example.basics.exceptions;

/**
 * 异常处理示例
 *
 * 核心知识点：
 * 1. Checked vs Unchecked 异常
 * 2. 自定义异常
 * 3. try-with-resources（JDK 7+）
 * 4. 异常处理最佳实践
 * 5. finally 中的陷阱
 *
 * 对应文档：docs/java-basics/exceptions.md
 */
public class ExceptionDemo {

    // ==================== 自定义异常 ====================

    /** 自定义业务异常（Unchecked，继承 RuntimeException） */
    static class BusinessException extends RuntimeException {
        private final String errorCode;

        public BusinessException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public BusinessException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }

    /** 自定义 Checked 异常（继承 Exception） */
    static class InsufficientBalanceException extends Exception {
        private final double balance;
        private final double amount;

        public InsufficientBalanceException(double balance, double amount) {
            super(String.format("余额不足: 当前余额 %.2f, 请求金额 %.2f", balance, amount));
            this.balance = balance;
            this.amount = amount;
        }

        public double getBalance() { return balance; }
        public double getAmount() { return amount; }
    }

    public static void main(String[] args) {
        System.out.println("===== 1. 自定义异常 =====");
        demonstrateCustomException();

        System.out.println("\n===== 2. try-with-resources =====");
        demonstrateTryWithResources();

        System.out.println("\n===== 3. 异常链 =====");
        demonstrateExceptionChain();

        System.out.println("\n===== 4. finally 陷阱 =====");
        demonstrateFinallyTrap();

        System.out.println("\n===== 5. 最佳实践 =====");
        demonstrateBestPractices();
    }

    static void demonstrateCustomException() {
        // Unchecked 异常：不需要显式 catch
        try {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        } catch (BusinessException e) {
            System.out.println("业务异常 [" + e.getErrorCode() + "]: " + e.getMessage());
        }

        // Checked 异常：必须 catch 或 throws
        try {
            withdraw(100.0, 200.0);
        } catch (InsufficientBalanceException e) {
            System.out.println("Checked 异常: " + e.getMessage());
        }
    }

    static void withdraw(double balance, double amount) throws InsufficientBalanceException {
        if (amount > balance) {
            throw new InsufficientBalanceException(balance, amount);
        }
    }

    /**
     * try-with-resources：自动关闭实现了 AutoCloseable 的资源
     * 关闭顺序与声明顺序相反
     */
    static void demonstrateTryWithResources() {
        // 自定义 AutoCloseable 资源
        class MyResource implements AutoCloseable {
            final String name;

            MyResource(String name) {
                this.name = name;
                System.out.println("  打开资源: " + name);
            }

            @Override
            public void close() {
                System.out.println("  关闭资源: " + name); // 自动调用
            }
        }

        // 多个资源：关闭顺序与声明顺序相反
        try (var r1 = new MyResource("数据库连接");
             var r2 = new MyResource("文件流")) {
            System.out.println("  使用资源中...");
        }
        // 输出顺序：关闭文件流 → 关闭数据库连接

        // Suppressed Exception 机制
        System.out.println("\n--- Suppressed Exception ---");
        try {
            tryWithSuppressed();
        } catch (Exception e) {
            System.out.println("主异常: " + e.getMessage());
            for (Throwable suppressed : e.getSuppressed()) {
                System.out.println("被抑制的异常: " + suppressed.getMessage());
            }
        }
    }

    static void tryWithSuppressed() throws Exception {
        class FailingResource implements AutoCloseable {
            @Override
            public void close() throws Exception {
                throw new Exception("close() 异常");
            }
        }

        try (var r = new FailingResource()) {
            throw new Exception("try 块异常");
        }
        // try 块异常是主异常，close() 异常作为 suppressed exception
    }

    /**
     * 异常链：保留原始异常信息
     */
    static void demonstrateExceptionChain() {
        try {
            serviceMethod();
        } catch (BusinessException e) {
            System.out.println("捕获: " + e.getMessage());
            System.out.println("原因: " + e.getCause().getMessage());
        }
    }

    static void serviceMethod() {
        try {
            // 模拟底层异常
            throw new NumberFormatException("无效的数字格式");
        } catch (NumberFormatException e) {
            // 包装为业务异常，保留原始异常作为 cause
            throw new BusinessException("PARSE_ERROR", "数据解析失败", e);
        }
    }

    /**
     * finally 中的陷阱
     */
    static void demonstrateFinallyTrap() {
        // 陷阱：finally 中的 return 会覆盖 try 中的 return
        int result = finallyReturnTrap();
        System.out.println("finally return 陷阱: " + result); // 2（不是 1）

        // 陷阱：finally 中的异常会覆盖 try 中的异常
        try {
            finallyExceptionTrap();
        } catch (Exception e) {
            System.out.println("finally 异常覆盖: " + e.getMessage()); // "finally 异常"
        }
    }

    static int finallyReturnTrap() {
        try {
            return 1; // 会被 finally 的 return 覆盖
        } finally {
            return 2; // ⚠️ 不要在 finally 中 return！
        }
    }

    static void finallyExceptionTrap() {
        try {
            throw new RuntimeException("try 异常");
        } finally {
            throw new RuntimeException("finally 异常"); // ⚠️ 覆盖了 try 的异常
        }
    }

    /**
     * 异常处理最佳实践
     */
    static void demonstrateBestPractices() {
        // 1. 精确捕获，不要 catch Exception/Throwable
        try {
            int result = 10 / 0;
        } catch (ArithmeticException e) { // ✅ 精确捕获
            System.out.println("✅ 精确捕获: " + e.getMessage());
        }

        // 2. 不要用异常控制流程
        // ❌ 错误示范：用异常判断数组越界
        // ✅ 正确做法：先检查边界

        // 3. 记录日志时包含异常堆栈
        try {
            throw new RuntimeException("测试异常");
        } catch (RuntimeException e) {
            // ✅ 日志中包含异常对象（会打印堆栈）
            System.out.println("✅ 记录异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // 4. 使用 multi-catch（JDK 7+）
        try {
            String s = null;
            s.length();
        } catch (NullPointerException | IllegalArgumentException e) {
            System.out.println("✅ multi-catch: " + e.getClass().getSimpleName());
        }
    }
}

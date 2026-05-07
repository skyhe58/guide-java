package com.example.patterns.behavioral;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略模式演示 — 替代 if-else 的支付方式选择
 * <p>
 * 业务场景：电商支付系统
 * - 支持多种支付方式：支付宝、微信、信用卡
 * - 每种支付方式有不同的优惠策略
 * - 通过策略模式替代 if-else，符合开闭原则
 * </p>
 */
public class StrategyDemo {

    public static void main(String[] args) {
        System.out.println("========== 策略模式演示 ==========\n");

        demonstrateIfElseApproach();
        System.out.println();
        demonstrateStrategyPattern();
        System.out.println();
        demonstrateStrategyWithMap();
    }

    // ==================== 策略接口 ====================

    /** 支付策略接口 */
    public interface PayStrategy {
        /**
         * 计算实际支付金额
         * @param amount 原始金额
         * @return 实际支付金额
         */
        double pay(double amount);

        /** 获取支付方式名称 */
        String getName();
    }

    // ==================== 具体策略 ====================

    /** 支付宝策略：95 折优惠 */
    public static class AlipayStrategy implements PayStrategy {
        @Override
        public double pay(double amount) {
            double actual = amount * 0.95;
            System.out.printf("    [支付宝] 原价 %.2f，95 折优惠，实付 %.2f%n", amount, actual);
            return actual;
        }

        @Override
        public String getName() { return "支付宝"; }
    }

    /** 微信支付策略：满 100 减 5 */
    public static class WechatPayStrategy implements PayStrategy {
        @Override
        public double pay(double amount) {
            double discount = amount >= 100 ? 5.0 : 0.0;
            double actual = amount - discount;
            System.out.printf("    [微信] 原价 %.2f，满减 %.2f，实付 %.2f%n",
                    amount, discount, actual);
            return actual;
        }

        @Override
        public String getName() { return "微信支付"; }
    }

    /** 信用卡策略：收取 1% 手续费 */
    public static class CreditCardStrategy implements PayStrategy {
        @Override
        public double pay(double amount) {
            double fee = amount * 0.01;
            double actual = amount + fee;
            System.out.printf("    [信用卡] 原价 %.2f，手续费 %.2f，实付 %.2f%n",
                    amount, fee, actual);
            return actual;
        }

        @Override
        public String getName() { return "信用卡"; }
    }

    // ==================== 上下文 ====================

    /** 支付上下文：持有策略引用 */
    static class PaymentContext {
        private PayStrategy strategy;

        public void setStrategy(PayStrategy strategy) {
            this.strategy = strategy;
        }

        public double executePayment(double amount) {
            if (strategy == null) {
                throw new IllegalStateException("未设置支付策略");
            }
            return strategy.pay(amount);
        }
    }

    // ==================== 演示 ====================

    /** ❌ 传统 if-else 方式 */
    private static void demonstrateIfElseApproach() {
        System.out.println("--- ❌ 传统 if-else 方式 ---");
        String payType = "alipay";
        double amount = 200.0;

        double actual;
        if ("alipay".equals(payType)) {
            actual = amount * 0.95;
        } else if ("wechat".equals(payType)) {
            actual = amount >= 100 ? amount - 5 : amount;
        } else if ("credit".equals(payType)) {
            actual = amount + amount * 0.01;
        } else {
            throw new IllegalArgumentException("不支持的支付方式");
        }
        System.out.printf("    支付 %.2f，实付 %.2f%n", amount, actual);
        System.out.println("    问题：新增支付方式需要修改 if-else，违反开闭原则");
    }

    /** ✅ 策略模式方式 */
    private static void demonstrateStrategyPattern() {
        System.out.println("--- ✅ 策略模式方式 ---");
        double amount = 200.0;

        PaymentContext context = new PaymentContext();

        // 运行时切换策略
        context.setStrategy(new AlipayStrategy());
        context.executePayment(amount);

        context.setStrategy(new WechatPayStrategy());
        context.executePayment(amount);

        context.setStrategy(new CreditCardStrategy());
        context.executePayment(amount);

        System.out.println("    优点：新增支付方式只需新增策略类，无需修改已有代码");
    }

    /** ✅ 策略模式 + Map 注册（Spring 项目中的常用方式） */
    private static void demonstrateStrategyWithMap() {
        System.out.println("--- ✅ 策略模式 + Map 注册 ---");

        // 模拟 Spring 中通过 Map 自动注入所有策略
        Map<String, PayStrategy> strategyMap = new HashMap<>();
        strategyMap.put("alipay", new AlipayStrategy());
        strategyMap.put("wechat", new WechatPayStrategy());
        strategyMap.put("credit", new CreditCardStrategy());

        // 根据类型获取策略
        String payType = "wechat";
        double amount = 150.0;

        PayStrategy strategy = strategyMap.get(payType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的支付方式: " + payType);
        }
        strategy.pay(amount);

        System.out.println("    在 Spring 中可以通过 @Autowired Map<String, PayStrategy> 自动注入");
    }
}

package com.example.distributed.transaction;

/**
 * 分布式事务方案对比演示 — 用状态机模拟 2PC / TCC / Saga 三种方案
 *
 * <p>本示例用纯 Java 模拟三种主流分布式事务方案：
 * <ul>
 *   <li>2PC（两阶段提交）— 强一致，性能低</li>
 *   <li>TCC（Try-Confirm-Cancel）— 最终一致，高性能，侵入性高</li>
 *   <li>Saga — 最终一致，长事务编排，补偿机制</li>
 *   <li>消息最终一致性 — 基于 MQ 的异步方案</li>
 * </ul>
 *
 * <h3>四种方案对比：</h3>
 * <pre>
 *  2PC:  协调器 → Prepare(A,B) → 全部OK → Commit(A,B)
 *                              → 任一失败 → Rollback(A,B)
 *
 *  TCC:  协调器 → Try(A,B) → 全部OK → Confirm(A,B)
 *                           → 任一失败 → Cancel(A,B)
 *
 *  Saga: 步骤1(A) → 步骤2(B) → 步骤3(C)
 *                              → 步骤3失败 → 补偿3 → 补偿2 → 补偿1
 *
 *  消息: 本地事务 + 发MQ → 消费者消费 + 本地事务（最终一致）
 * </pre>
 */
public class DistributedTransactionDemo {

    // ==================== 参与方接口 ====================

    /** 事务参与方 */
    interface Participant {
        String name();
        boolean prepare();      // 2PC: 准备阶段
        boolean commit();       // 2PC: 提交
        boolean rollback();     // 2PC: 回滚
        boolean execute();      // Saga: 正向操作
        boolean compensate();   // Saga: 补偿操作
    }

    /** 订单服务参与方 */
    static class OrderParticipant implements Participant {
        private String status = "INIT";
        private boolean simulateFailure = false;

        void setSimulateFailure(boolean fail) { this.simulateFailure = fail; }

        @Override public String name() { return "订单服务"; }

        @Override
        public boolean prepare() {
            if (simulateFailure) { System.out.printf("    [%s] Prepare 失败%n", name()); return false; }
            status = "PREPARED";
            System.out.printf("    [%s] Prepare OK → 锁定订单资源%n", name());
            return true;
        }

        @Override
        public boolean commit() {
            status = "COMMITTED";
            System.out.printf("    [%s] Commit → 订单确认%n", name());
            return true;
        }

        @Override
        public boolean rollback() {
            status = "ROLLED_BACK";
            System.out.printf("    [%s] Rollback → 订单取消%n", name());
            return true;
        }

        @Override
        public boolean execute() {
            if (simulateFailure) { System.out.printf("    [%s] Execute 失败%n", name()); return false; }
            status = "CREATED";
            System.out.printf("    [%s] Execute → 创建订单%n", name());
            return true;
        }

        @Override
        public boolean compensate() {
            status = "CANCELLED";
            System.out.printf("    [%s] Compensate → 取消订单%n", name());
            return true;
        }

        String getStatus() { return status; }
    }

    /** 库存服务参与方 */
    static class InventoryParticipant implements Participant {
        private int stock;
        private int reserved = 0;
        private boolean simulateFailure = false;

        InventoryParticipant(int stock) { this.stock = stock; }
        void setSimulateFailure(boolean fail) { this.simulateFailure = fail; }

        @Override public String name() { return "库存服务"; }

        @Override
        public boolean prepare() {
            if (simulateFailure || stock <= 0) {
                System.out.printf("    [%s] Prepare 失败（库存不足）%n", name());
                return false;
            }
            reserved = 1;
            System.out.printf("    [%s] Prepare OK → 预留库存 1（剩余 %d）%n", name(), stock - reserved);
            return true;
        }

        @Override
        public boolean commit() {
            stock -= reserved;
            reserved = 0;
            System.out.printf("    [%s] Commit → 扣减库存（剩余 %d）%n", name(), stock);
            return true;
        }

        @Override
        public boolean rollback() {
            reserved = 0;
            System.out.printf("    [%s] Rollback → 释放预留库存（剩余 %d）%n", name(), stock);
            return true;
        }

        @Override
        public boolean execute() {
            if (simulateFailure || stock <= 0) {
                System.out.printf("    [%s] Execute 失败%n", name());
                return false;
            }
            stock--;
            System.out.printf("    [%s] Execute → 扣减库存（剩余 %d）%n", name(), stock);
            return true;
        }

        @Override
        public boolean compensate() {
            stock++;
            System.out.printf("    [%s] Compensate → 恢复库存（剩余 %d）%n", name(), stock);
            return true;
        }

        String getStatus() { return String.format("stock=%d, reserved=%d", stock, reserved); }
    }

    /** 支付服务参与方 */
    static class PaymentParticipant implements Participant {
        private double balance;
        private double frozen = 0;
        private boolean simulateFailure = false;

        PaymentParticipant(double balance) { this.balance = balance; }
        void setSimulateFailure(boolean fail) { this.simulateFailure = fail; }

        @Override public String name() { return "支付服务"; }

        @Override
        public boolean prepare() {
            if (simulateFailure) { System.out.printf("    [%s] Prepare 失败%n", name()); return false; }
            frozen = 100;
            System.out.printf("    [%s] Prepare OK → 冻结 %.0f 元%n", name(), frozen);
            return true;
        }

        @Override
        public boolean commit() {
            balance -= frozen;
            frozen = 0;
            System.out.printf("    [%s] Commit → 扣款（余额 %.0f）%n", name(), balance);
            return true;
        }

        @Override
        public boolean rollback() {
            frozen = 0;
            System.out.printf("    [%s] Rollback → 解冻（余额 %.0f）%n", name(), balance);
            return true;
        }

        @Override
        public boolean execute() {
            if (simulateFailure) { System.out.printf("    [%s] Execute 失败%n", name()); return false; }
            balance -= 100;
            System.out.printf("    [%s] Execute → 扣款 100（余额 %.0f）%n", name(), balance);
            return true;
        }

        @Override
        public boolean compensate() {
            balance += 100;
            System.out.printf("    [%s] Compensate → 退款 100（余额 %.0f）%n", name(), balance);
            return true;
        }

        String getStatus() { return String.format("balance=%.0f, frozen=%.0f", balance, frozen); }
    }

    // ==================== 2PC 协调器 ====================

    static class TwoPhaseCommitCoordinator {
        boolean execute(java.util.List<Participant> participants) {
            System.out.println("\n  【Phase 1: Prepare】");
            boolean allPrepared = true;
            for (Participant p : participants) {
                if (!p.prepare()) { allPrepared = false; break; }
            }

            if (allPrepared) {
                System.out.println("  【Phase 2: Commit】全部 Prepare 成功");
                for (Participant p : participants) p.commit();
                return true;
            } else {
                System.out.println("  【Phase 2: Rollback】存在 Prepare 失败");
                for (Participant p : participants) p.rollback();
                return false;
            }
        }
    }

    // ==================== Saga 编排器 ====================

    static class SagaOrchestrator {
        boolean execute(java.util.List<Participant> participants) {
            java.util.List<Participant> executed = new java.util.ArrayList<>();

            System.out.println("\n  【Saga 正向执行】");
            for (Participant p : participants) {
                if (p.execute()) {
                    executed.add(p);
                } else {
                    // 失败 → 逆序补偿已执行的步骤
                    System.out.println("  【Saga 补偿回滚】");
                    for (int i = executed.size() - 1; i >= 0; i--) {
                        executed.get(i).compensate();
                    }
                    return false;
                }
            }
            return true;
        }
    }

    // ==================== 演示方法 ====================

    static void demo2PC() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：2PC（两阶段提交）");
        System.out.println("═══════════════════════════════════════════════════");

        // 正常提交
        System.out.println("\n  【场景1：正常提交】");
        OrderParticipant order1 = new OrderParticipant();
        InventoryParticipant inv1 = new InventoryParticipant(10);
        PaymentParticipant pay1 = new PaymentParticipant(1000);
        boolean r1 = new TwoPhaseCommitCoordinator().execute(java.util.Arrays.asList(order1, inv1, pay1));
        System.out.printf("  结果: %s | 订单=%s, 库存=%s, 支付=%s%n",
                r1 ? "✓ 提交" : "✗ 回滚", order1.getStatus(), inv1.getStatus(), pay1.getStatus());

        // 支付失败 → 回滚
        System.out.println("\n  【场景2：支付失败 → 全部回滚】");
        OrderParticipant order2 = new OrderParticipant();
        InventoryParticipant inv2 = new InventoryParticipant(10);
        PaymentParticipant pay2 = new PaymentParticipant(1000);
        pay2.setSimulateFailure(true);
        boolean r2 = new TwoPhaseCommitCoordinator().execute(java.util.Arrays.asList(order2, inv2, pay2));
        System.out.printf("  结果: %s | 订单=%s, 库存=%s, 支付=%s%n",
                r2 ? "✓ 提交" : "✗ 回滚", order2.getStatus(), inv2.getStatus(), pay2.getStatus());
        System.out.println();
    }

    static void demoSaga() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Saga（补偿事务）");
        System.out.println("═══════════════════════════════════════════════════");

        // 正常执行
        System.out.println("\n  【场景1：正常执行】");
        OrderParticipant order1 = new OrderParticipant();
        InventoryParticipant inv1 = new InventoryParticipant(10);
        PaymentParticipant pay1 = new PaymentParticipant(1000);
        boolean r1 = new SagaOrchestrator().execute(java.util.Arrays.asList(order1, inv1, pay1));
        System.out.printf("  结果: %s%n", r1 ? "✓ 成功" : "✗ 补偿回滚");

        // 支付失败 → 逆序补偿
        System.out.println("\n  【场景2：支付失败 → 逆序补偿（订单+库存）】");
        OrderParticipant order2 = new OrderParticipant();
        InventoryParticipant inv2 = new InventoryParticipant(10);
        PaymentParticipant pay2 = new PaymentParticipant(1000);
        pay2.setSimulateFailure(true);
        boolean r2 = new SagaOrchestrator().execute(java.util.Arrays.asList(order2, inv2, pay2));
        System.out.printf("  结果: %s | 订单=%s, 库存=%s%n",
                r2 ? "✓ 成功" : "✗ 补偿回滚", order2.getStatus(), inv2.getStatus());
        System.out.println();
    }

    static void demoComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：四种方案综合对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"方案",       "一致性",   "性能",  "侵入性", "复杂度", "适用场景"},
                {"2PC",       "强一致",   "低",   "低",    "低",   "数据库层面，短事务"},
                {"TCC",       "最终一致", "高",   "高",    "高",   "高性能，需改造业务"},
                {"Saga",      "最终一致", "高",   "中",    "中",   "长事务，流程编排"},
                {"消息最终一致","最终一致", "高",   "低",    "中",   "异步场景"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-10s %-10s %-6s %-8s %-8s %-20s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2],
                    comparison[i][3], comparison[i][4], comparison[i][5]);
            if (i == 0) System.out.println("  " + "─".repeat(65));
        }

        System.out.println("\n  推荐选择：");
        System.out.println("    大多数场景 → 消息最终一致性（简单可靠）");
        System.out.println("    高性能场景 → TCC（如支付扣款）");
        System.out.println("    长流程场景 → Saga（如订单履约）");
        System.out.println("    强一致场景 → 2PC / Seata AT（如银行转账）");
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  分布式事务方案对比演示（纯内存模拟）                    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demo2PC();
        demoSaga();
        demoComparison();
    }
}

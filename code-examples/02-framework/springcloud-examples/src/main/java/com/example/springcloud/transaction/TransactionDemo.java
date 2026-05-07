package com.example.springcloud.transaction;

/**
 * 分布式事务 TCC 模式演示 — 用状态机模拟 Seata TCC 补偿事务
 *
 * <p>本示例用纯 Java 模拟 TCC（Try-Confirm-Cancel）分布式事务的完整流程：
 * <ul>
 *   <li>TCC 三阶段：Try（资源预留）→ Confirm（提交）/ Cancel（回滚）</li>
 *   <li>事务协调器（Transaction Coordinator）</li>
 *   <li>空回滚、悬挂、幂等三大问题及解决方案</li>
 *   <li>TCC vs 2PC vs Saga 对比</li>
 * </ul>
 *
 * <h3>TCC 事务流程：</h3>
 * <pre>
 *  事务发起方                事务协调器              参与方A（订单）     参与方B（库存）     参与方C（账户）
 *      │                      │                     │                │                │
 *      │── 开启全局事务 ──→    │                     │                │                │
 *      │                      │── Try ──────────→   │                │                │
 *      │                      │── Try ──────────────────────────→   │                │
 *      │                      │── Try ──────────────────────────────────────────→    │
 *      │                      │                     │                │                │
 *      │                      │   全部 Try 成功？                                      │
 *      │                      │── Confirm ──────→   │                │                │
 *      │                      │── Confirm ──────────────────────→   │                │
 *      │                      │── Confirm ──────────────────────────────────────→    │
 *      │                      │                     │                │                │
 *      │                      │   如果任一 Try 失败：                                   │
 *      │                      │── Cancel ───────→   │                │                │
 *      │                      │── Cancel ───────────────────────→   │                │
 *      │                      │── Cancel ───────────────────────────────────────→    │
 * </pre>
 */
public class TransactionDemo {

    // ==================== TCC 参与方接口 ====================

    /** TCC 参与方接口：每个参与方必须实现 Try / Confirm / Cancel 三个方法 */
    interface TccParticipant {
        String name();
        boolean tryAction(String txId, java.util.Map<String, Object> params);
        boolean confirm(String txId);
        boolean cancel(String txId);
    }

    /** 事务状态 */
    enum TxStatus { TRYING, CONFIRMING, CANCELLING, CONFIRMED, CANCELLED, FAILED }

    // ==================== 订单服务 ====================

    /** 订单服务 TCC 参与方 */
    static class OrderService implements TccParticipant {
        // 订单状态：orderId → status（INIT/TRYING/CONFIRMED/CANCELLED）
        private final java.util.Map<String, String> orders = new java.util.concurrent.ConcurrentHashMap<>();
        // 防悬挂：记录已经 Cancel 过的事务，防止后到的 Try 执行
        private final java.util.Set<String> cancelledTxIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

        @Override
        public String name() { return "订单服务"; }

        @Override
        public boolean tryAction(String txId, java.util.Map<String, Object> params) {
            // 防悬挂检查：如果已经 Cancel 过，拒绝 Try
            if (cancelledTxIds.contains(txId)) {
                System.out.printf("    [%s] Try 拒绝（防悬挂：该事务已 Cancel）%n", name());
                return false;
            }
            String orderId = (String) params.get("orderId");
            orders.put(orderId, "TRYING");
            System.out.printf("    [%s] Try: 创建订单 %s（状态=TRYING）%n", name(), orderId);
            return true;
        }

        @Override
        public boolean confirm(String txId) {
            orders.replaceAll((k, v) -> "TRYING".equals(v) ? "CONFIRMED" : v);
            System.out.printf("    [%s] Confirm: 订单状态 → CONFIRMED%n", name());
            return true;
        }

        @Override
        public boolean cancel(String txId) {
            cancelledTxIds.add(txId);
            // 空回滚检查：如果没有 Try 过，直接返回成功
            boolean hasTrying = orders.values().stream().anyMatch("TRYING"::equals);
            if (!hasTrying) {
                System.out.printf("    [%s] Cancel（空回滚：未执行过 Try，直接返回成功）%n", name());
                return true;
            }
            orders.replaceAll((k, v) -> "TRYING".equals(v) ? "CANCELLED" : v);
            System.out.printf("    [%s] Cancel: 订单状态 → CANCELLED%n", name());
            return true;
        }

        java.util.Map<String, String> getOrders() { return new java.util.LinkedHashMap<>(orders); }
    }

    // ==================== 库存服务 ====================

    /** 库存服务 TCC 参与方 */
    static class InventoryService implements TccParticipant {
        private int totalStock;
        private int frozenStock = 0; // Try 阶段冻结的库存
        // 幂等控制：记录已处理的事务
        private final java.util.Set<String> processedTxIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

        InventoryService(int totalStock) { this.totalStock = totalStock; }

        @Override
        public String name() { return "库存服务"; }

        @Override
        public boolean tryAction(String txId, java.util.Map<String, Object> params) {
            // 幂等检查
            if (processedTxIds.contains(txId + ":try")) {
                System.out.printf("    [%s] Try 幂等：已处理过，直接返回成功%n", name());
                return true;
            }
            int quantity = (int) params.get("quantity");
            if (totalStock - frozenStock < quantity) {
                System.out.printf("    [%s] Try 失败: 库存不足（可用=%d, 需要=%d）%n",
                        name(), totalStock - frozenStock, quantity);
                return false;
            }
            frozenStock += quantity;
            processedTxIds.add(txId + ":try");
            System.out.printf("    [%s] Try: 冻结库存 %d（总=%d, 冻结=%d, 可用=%d）%n",
                    name(), quantity, totalStock, frozenStock, totalStock - frozenStock);
            return true;
        }

        @Override
        public boolean confirm(String txId) {
            // Confirm：扣减冻结的库存
            totalStock -= frozenStock;
            int deducted = frozenStock;
            frozenStock = 0;
            System.out.printf("    [%s] Confirm: 扣减库存 %d（剩余=%d）%n", name(), deducted, totalStock);
            return true;
        }

        @Override
        public boolean cancel(String txId) {
            // Cancel：释放冻结的库存
            int released = frozenStock;
            frozenStock = 0;
            System.out.printf("    [%s] Cancel: 释放冻结库存 %d（总=%d, 可用=%d）%n",
                    name(), released, totalStock, totalStock);
            return true;
        }

        String getStatus() {
            return String.format("总库存=%d, 冻结=%d, 可用=%d", totalStock, frozenStock, totalStock - frozenStock);
        }
    }

    // ==================== 账户服务 ====================

    /** 账户服务 TCC 参与方 */
    static class AccountService implements TccParticipant {
        private double balance;
        private double frozenAmount = 0;
        private boolean simulateFailure = false;

        AccountService(double balance) { this.balance = balance; }

        void setSimulateFailure(boolean fail) { this.simulateFailure = fail; }

        @Override
        public String name() { return "账户服务"; }

        @Override
        public boolean tryAction(String txId, java.util.Map<String, Object> params) {
            if (simulateFailure) {
                System.out.printf("    [%s] Try 失败: 模拟服务异常%n", name());
                return false;
            }
            double amount = (double) params.get("amount");
            if (balance - frozenAmount < amount) {
                System.out.printf("    [%s] Try 失败: 余额不足（可用=%.2f, 需要=%.2f）%n",
                        name(), balance - frozenAmount, amount);
                return false;
            }
            frozenAmount += amount;
            System.out.printf("    [%s] Try: 冻结金额 %.2f（余额=%.2f, 冻结=%.2f）%n",
                    name(), amount, balance, frozenAmount);
            return true;
        }

        @Override
        public boolean confirm(String txId) {
            balance -= frozenAmount;
            double deducted = frozenAmount;
            frozenAmount = 0;
            System.out.printf("    [%s] Confirm: 扣款 %.2f（余额=%.2f）%n", name(), deducted, balance);
            return true;
        }

        @Override
        public boolean cancel(String txId) {
            double released = frozenAmount;
            frozenAmount = 0;
            System.out.printf("    [%s] Cancel: 释放冻结金额 %.2f（余额=%.2f）%n", name(), released, balance);
            return true;
        }

        String getStatus() {
            return String.format("余额=%.2f, 冻结=%.2f", balance, frozenAmount);
        }
    }

    // ==================== 事务协调器 ====================

    /** TCC 事务协调器（模拟 Seata TC） */
    static class TransactionCoordinator {
        private int txCounter = 0;

        /** 执行 TCC 分布式事务 */
        boolean execute(java.util.List<TccParticipant> participants,
                        java.util.List<java.util.Map<String, Object>> paramsList) {
            String txId = "TX-" + (++txCounter);
            System.out.printf("\n  ═══ 全局事务 %s 开始 ═══%n", txId);

            // Phase 1: Try（资源预留）
            System.out.println("  【Phase 1: Try】");
            boolean allTrySuccess = true;
            int trySuccessCount = 0;

            for (int i = 0; i < participants.size(); i++) {
                boolean success = participants.get(i).tryAction(txId, paramsList.get(i));
                if (!success) {
                    allTrySuccess = false;
                    break;
                }
                trySuccessCount++;
            }

            // Phase 2: Confirm or Cancel
            if (allTrySuccess) {
                System.out.println("  【Phase 2: Confirm】所有 Try 成功");
                for (TccParticipant p : participants) {
                    p.confirm(txId);
                }
                System.out.printf("  ═══ 全局事务 %s 提交成功 ═══%n", txId);
                return true;
            } else {
                System.out.println("  【Phase 2: Cancel】存在 Try 失败，回滚");
                // 只回滚已经 Try 成功的参与方
                for (int i = 0; i < trySuccessCount; i++) {
                    participants.get(i).cancel(txId);
                }
                System.out.printf("  ═══ 全局事务 %s 回滚完成 ═══%n", txId);
                return false;
            }
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：TCC 正常提交 */
    static void demoNormalCommit() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：TCC 正常提交 — 下单扣库存扣款");
        System.out.println("═══════════════════════════════════════════════════");

        OrderService orderService = new OrderService();
        InventoryService inventoryService = new InventoryService(100);
        AccountService accountService = new AccountService(1000.00);
        TransactionCoordinator tc = new TransactionCoordinator();

        java.util.List<TccParticipant> participants = java.util.Arrays.asList(
                orderService, inventoryService, accountService);
        java.util.List<java.util.Map<String, Object>> params = java.util.Arrays.asList(
                map("orderId", "ORD-1001"),
                map("quantity", 2),
                map("amount", 299.99));

        boolean success = tc.execute(participants, params);

        System.out.println("\n  最终状态：");
        System.out.printf("    事务结果: %s%n", success ? "✓ 提交" : "✗ 回滚");
        System.out.printf("    订单: %s%n", orderService.getOrders());
        System.out.printf("    库存: %s%n", inventoryService.getStatus());
        System.out.printf("    账户: %s%n", accountService.getStatus());
        System.out.println();
    }

    /** 演示2：TCC 回滚 — 账户服务 Try 失败 */
    static void demoRollback() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：TCC 回滚 — 账户服务异常");
        System.out.println("═══════════════════════════════════════════════════");

        OrderService orderService = new OrderService();
        InventoryService inventoryService = new InventoryService(100);
        AccountService accountService = new AccountService(1000.00);
        accountService.setSimulateFailure(true); // 模拟账户服务异常
        TransactionCoordinator tc = new TransactionCoordinator();

        java.util.List<TccParticipant> participants = java.util.Arrays.asList(
                orderService, inventoryService, accountService);
        java.util.List<java.util.Map<String, Object>> params = java.util.Arrays.asList(
                map("orderId", "ORD-1002"),
                map("quantity", 1),
                map("amount", 199.99));

        boolean success = tc.execute(participants, params);

        System.out.println("\n  最终状态：");
        System.out.printf("    事务结果: %s%n", success ? "✓ 提交" : "✗ 回滚");
        System.out.printf("    订单: %s（已回滚为 CANCELLED）%n", orderService.getOrders());
        System.out.printf("    库存: %s（冻结已释放）%n", inventoryService.getStatus());
        System.out.printf("    账户: %s（未扣款）%n", accountService.getStatus());
        System.out.println();
    }

    /** 演示3：TCC 三大问题 */
    static void demoTccProblems() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：TCC 三大问题 — 空回滚/悬挂/幂等");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【问题1：空回滚】");
        System.out.println("    场景：Try 超时未执行，协调器直接发 Cancel");
        System.out.println("    解决：Cancel 时检查是否执行过 Try，没有则直接返回成功");

        System.out.println("\n  【问题2：悬挂】");
        System.out.println("    场景：Cancel 先到达，Try 后到达（网络延迟）");
        System.out.println("    解决：Try 时检查是否已 Cancel，已 Cancel 则拒绝 Try");

        System.out.println("\n  【问题3：幂等】");
        System.out.println("    场景：网络重试导致 Confirm/Cancel 被调用多次");
        System.out.println("    解决：记录事务执行状态，重复调用直接返回成功");

        // 模拟空回滚
        System.out.println("\n  【模拟空回滚】");
        OrderService orderService = new OrderService();
        orderService.cancel("TX-EMPTY"); // 没有 Try 过，直接 Cancel
        System.out.println();
    }

    /** 演示4：TCC vs 2PC vs Saga 对比 */
    static void demoComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：分布式事务方案对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"方案",   "一致性",   "性能",    "侵入性",  "适用场景"},
                {"2PC",   "强一致",   "低",     "低",     "数据库层面，短事务"},
                {"TCC",   "最终一致", "高",     "高",     "高性能场景，需要改造业务代码"},
                {"Saga",  "最终一致", "高",     "中",     "长事务，业务流程编排"},
                {"消息事务","最终一致", "高",     "低",     "异步场景，最终一致即可"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-10s %-10s %-8s %-8s %-30s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2],
                    comparison[i][3], comparison[i][4]);
            if (i == 0) System.out.println("  " + "─".repeat(70));
        }
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    static java.util.Map<String, Object> map(String key, Object value) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put(key, value);
        return m;
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  分布式事务 TCC 模式演示 — 状态机模拟（纯内存）          ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoNormalCommit();
        demoRollback();
        demoTccProblems();
        demoComparison();
    }
}

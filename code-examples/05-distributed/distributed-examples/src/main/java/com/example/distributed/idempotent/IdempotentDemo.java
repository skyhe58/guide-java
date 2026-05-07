package com.example.distributed.idempotent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等性设计方案实现示例
 *
 * 展示三种常见的幂等方案：Token 机制、唯一索引、状态机。
 * 使用内存模拟 Redis/数据库，可直接运行。
 */
public class IdempotentDemo {

    // ==================== 方案一：Token 机制 ====================

    /**
     * Token 机制幂等实现
     *
     * 流程：
     * 1. 客户端请求服务端获取 Token
     * 2. 服务端生成 Token 存入 Redis，返回给客户端
     * 3. 客户端提交请求时携带 Token
     * 4. 服务端验证并删除 Token（原子操作）
     * 5. 重复提交时 Token 已被删除，拒绝请求
     */
    static class TokenIdempotent {
        // 模拟 Redis 存储
        private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

        /**
         * 生成 Token
         */
        public String createToken() {
            String token = UUID.randomUUID().toString();
            tokenStore.put("token:" + token, "1");
            System.out.println("  生成 Token: " + token);
            return token;
        }

        /**
         * 验证并消费 Token
         * 实际生产中应使用 Lua 脚本保证原子性：
         * if redis.call('get', KEYS[1]) then
         *     return redis.call('del', KEYS[1])
         * else
         *     return 0
         * end
         */
        public boolean consumeToken(String token) {
            // 模拟原子操作：检查并删除
            return tokenStore.remove("token:" + token) != null;
        }
    }

    // ==================== 方案二：唯一索引去重 ====================

    /**
     * 唯一索引幂等实现
     *
     * 利用数据库唯一索引约束，重复插入会抛出 DuplicateKeyException。
     * 适用于有唯一业务标识的场景（订单号、流水号）。
     */
    static class UniqueIndexIdempotent {
        // 模拟数据库唯一索引
        private final Map<String, String> orderTable = new ConcurrentHashMap<>();

        /**
         * 创建订单（幂等）
         *
         * @param orderNo 订单号（唯一）
         * @param data    订单数据
         * @return true=创建成功，false=订单已存在
         */
        public boolean createOrder(String orderNo, String data) {
            // putIfAbsent 模拟数据库唯一索引
            String existing = orderTable.putIfAbsent(orderNo, data);
            if (existing == null) {
                System.out.println("  订单创建成功: " + orderNo);
                return true;
            } else {
                System.out.println("  订单已存在（幂等返回）: " + orderNo);
                return false;
            }
        }
    }

    // ==================== 方案三：状态机幂等 ====================

    /**
     * 状态机幂等实现
     *
     * 通过状态流转条件控制幂等性。
     * 例如：订单只有"待支付"状态才能变为"已支付"。
     *
     * SQL 示例：
     * UPDATE orders SET status = 'PAID'
     * WHERE order_no = ? AND status = 'UNPAID';
     * -- 影响行数=1 → 成功；影响行数=0 → 状态已变更
     */
    static class StateMachineIdempotent {

        enum OrderStatus {
            UNPAID("待支付"),
            PAID("已支付"),
            SHIPPED("已发货"),
            COMPLETED("已完成"),
            CANCELLED("已取消");

            final String desc;
            OrderStatus(String desc) { this.desc = desc; }
        }

        // 模拟数据库中的订单状态
        private final Map<String, OrderStatus> orderStatusMap = new ConcurrentHashMap<>();

        public void createOrder(String orderNo) {
            orderStatusMap.put(orderNo, OrderStatus.UNPAID);
            System.out.println("  创建订单: " + orderNo + " [" + OrderStatus.UNPAID.desc + "]");
        }

        /**
         * 支付订单（幂等）
         * 只有"待支付"状态才能变为"已支付"
         */
        public boolean payOrder(String orderNo) {
            // 模拟 CAS 操作：只有当前状态是 UNPAID 才更新为 PAID
            boolean success = orderStatusMap.replace(orderNo, OrderStatus.UNPAID, OrderStatus.PAID);
            if (success) {
                System.out.println("  支付成功: " + orderNo + " [" + OrderStatus.PAID.desc + "]");
            } else {
                OrderStatus current = orderStatusMap.get(orderNo);
                System.out.println("  支付忽略（幂等）: " + orderNo
                        + " 当前状态=[" + (current != null ? current.desc : "不存在") + "]");
            }
            return success;
        }
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        System.out.println("========== 幂等性设计方案演示 ==========");
        System.out.println();

        // 方案一：Token 机制
        System.out.println("--- 方案一：Token 机制 ---");
        TokenIdempotent tokenIdempotent = new TokenIdempotent();
        String token = tokenIdempotent.createToken();
        System.out.println("  第一次提交: " + (tokenIdempotent.consumeToken(token) ? "成功" : "失败"));
        System.out.println("  第二次提交: " + (tokenIdempotent.consumeToken(token) ? "成功" : "失败（Token 已消费）"));
        System.out.println();

        // 方案二：唯一索引
        System.out.println("--- 方案二：唯一索引去重 ---");
        UniqueIndexIdempotent uniqueIdempotent = new UniqueIndexIdempotent();
        uniqueIdempotent.createOrder("ORD202401001", "iPhone 16");
        uniqueIdempotent.createOrder("ORD202401001", "iPhone 16");  // 重复创建
        System.out.println();

        // 方案三：状态机
        System.out.println("--- 方案三：状态机幂等 ---");
        StateMachineIdempotent stateMachine = new StateMachineIdempotent();
        stateMachine.createOrder("ORD202401002");
        stateMachine.payOrder("ORD202401002");   // 第一次支付
        stateMachine.payOrder("ORD202401002");   // 重复支付（幂等）
        System.out.println();

        System.out.println("========== 方案对比 ==========");
        System.out.println("| 方案     | 适用场景           | 实现复杂度 |");
        System.out.println("|---------|-------------------|----------|");
        System.out.println("| Token   | 表单防重复提交       | ⭐⭐      |");
        System.out.println("| 唯一索引 | 有唯一业务标识       | ⭐        |");
        System.out.println("| 状态机   | 有状态流转的业务     | ⭐⭐      |");
        System.out.println("| 乐观锁   | 并发更新场景        | ⭐⭐      |");
        System.out.println("| 分布式锁 | 通用场景           | ⭐⭐⭐    |");
        System.out.println("| 去重表   | MQ 消费去重        | ⭐⭐      |");
    }
}

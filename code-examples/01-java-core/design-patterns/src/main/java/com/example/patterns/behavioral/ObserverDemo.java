package com.example.patterns.behavioral;

import java.util.ArrayList;
import java.util.List;

/**
 * 观察者模式演示 — 事件驱动的订单通知系统
 * <p>
 * 业务场景：订单创建后通知多个系统
 * - 邮件服务：发送订单确认邮件
 * - 库存服务：扣减库存
 * - 积分服务：增加用户积分
 * 类似 Spring 的 ApplicationEvent + ApplicationListener
 * </p>
 */
public class ObserverDemo {

    public static void main(String[] args) {
        System.out.println("========== 观察者模式演示 ==========\n");

        // 创建事件源（被观察者）
        OrderEventPublisher publisher = new OrderEventPublisher();

        // 注册观察者
        publisher.addListener(new EmailNotificationListener());
        publisher.addListener(new InventoryListener());
        publisher.addListener(new PointsListener());

        // 触发事件
        System.out.println("--- 创建订单 ---");
        publisher.createOrder("ORD-001", "张三", 299.0);

        System.out.println("\n--- 取消订单 ---");
        publisher.cancelOrder("ORD-001", "张三");

        // 动态移除观察者
        System.out.println("\n--- 移除积分监听器后再创建订单 ---");
        publisher.removeListener(PointsListener.class);
        publisher.createOrder("ORD-002", "李四", 599.0);
    }

    // ==================== 事件定义 ====================

    /** 订单事件 */
    record OrderEvent(String type, String orderId, String userName, double amount) {
        static OrderEvent created(String orderId, String userName, double amount) {
            return new OrderEvent("CREATED", orderId, userName, amount);
        }

        static OrderEvent cancelled(String orderId, String userName) {
            return new OrderEvent("CANCELLED", orderId, userName, 0);
        }
    }

    // ==================== 观察者接口 ====================

    /** 事件监听器接口（类似 Spring ApplicationListener） */
    interface OrderEventListener {
        void onEvent(OrderEvent event);
    }

    // ==================== 具体观察者 ====================

    /** 邮件通知监听器 */
    static class EmailNotificationListener implements OrderEventListener {
        @Override
        public void onEvent(OrderEvent event) {
            switch (event.type()) {
                case "CREATED" -> System.out.printf(
                        "  [邮件] 发送订单确认邮件给 %s，订单号: %s，金额: %.2f%n",
                        event.userName(), event.orderId(), event.amount());
                case "CANCELLED" -> System.out.printf(
                        "  [邮件] 发送订单取消通知给 %s，订单号: %s%n",
                        event.userName(), event.orderId());
            }
        }
    }

    /** 库存监听器 */
    static class InventoryListener implements OrderEventListener {
        @Override
        public void onEvent(OrderEvent event) {
            switch (event.type()) {
                case "CREATED" -> System.out.println(
                        "  [库存] 扣减库存，订单号: " + event.orderId());
                case "CANCELLED" -> System.out.println(
                        "  [库存] 恢复库存，订单号: " + event.orderId());
            }
        }
    }

    /** 积分监听器 */
    static class PointsListener implements OrderEventListener {
        @Override
        public void onEvent(OrderEvent event) {
            if ("CREATED".equals(event.type())) {
                int points = (int) event.amount();
                System.out.printf("  [积分] 为 %s 增加 %d 积分%n",
                        event.userName(), points);
            }
        }
    }

    // ==================== 被观察者（事件发布者） ====================

    /** 订单事件发布者（类似 Spring ApplicationEventPublisher） */
    static class OrderEventPublisher {
        private final List<OrderEventListener> listeners = new ArrayList<>();

        public void addListener(OrderEventListener listener) {
            listeners.add(listener);
        }

        public void removeListener(Class<? extends OrderEventListener> listenerClass) {
            listeners.removeIf(l -> l.getClass().equals(listenerClass));
        }

        /** 发布事件：通知所有监听器 */
        private void publishEvent(OrderEvent event) {
            for (OrderEventListener listener : listeners) {
                listener.onEvent(event);
            }
        }

        public void createOrder(String orderId, String userName, double amount) {
            System.out.println("  订单创建: " + orderId);
            publishEvent(OrderEvent.created(orderId, userName, amount));
        }

        public void cancelOrder(String orderId, String userName) {
            System.out.println("  订单取消: " + orderId);
            publishEvent(OrderEvent.cancelled(orderId, userName));
        }
    }
}

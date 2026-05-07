package com.example.sharding.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 *
 * 对应分片表 t_order_0 ~ t_order_3，由 ShardingSphere 自动路由。
 * 分片键：order_id（取模分片）
 */
public class OrderEntity {

    /** 订单ID（分片键，雪花算法生成） */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单状态（NEW / PAID / SHIPPED / COMPLETED） */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    public OrderEntity() {
    }

    public OrderEntity(Long orderId, Long userId, BigDecimal amount, String status, LocalDateTime createTime) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createTime = createTime;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "OrderEntity{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}

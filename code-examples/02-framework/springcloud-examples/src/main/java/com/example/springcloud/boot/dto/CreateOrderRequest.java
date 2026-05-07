package com.example.springcloud.boot.dto;

import jakarta.validation.constraints.*;

/**
 * 订单创建请求 DTO — 演示分组校验
 *
 * <p>分组校验：不同场景使用不同的校验规则
 * <ul>
 *   <li>Create 分组：创建订单时必须校验 productName 和 quantity</li>
 *   <li>Update 分组：更新订单时必须校验 orderId</li>
 * </ul>
 */
public class CreateOrderRequest {

    /** 创建分组 */
    public interface Create {}

    /** 更新分组 */
    public interface Update {}

    @NotNull(message = "订单ID不能为空", groups = Update.class)
    private Long orderId;

    @NotBlank(message = "商品名称不能为空", groups = {Create.class, Update.class})
    private String productName;

    @NotNull(message = "数量不能为空", groups = Create.class)
    @Min(value = 1, message = "数量最少为 1", groups = Create.class)
    @Max(value = 9999, message = "数量最多为 9999", groups = Create.class)
    private Integer quantity;

    @DecimalMin(value = "0.01", message = "金额最小为 0.01", groups = Create.class)
    private Double amount;

    // getter/setter
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}

package com.example.springcloud.boot.exception;

/**
 * 自定义业务异常 — 用于表示可预期的业务错误
 *
 * <p>与 RuntimeException 的区别：
 * <ul>
 *   <li>BusinessException：业务逻辑错误（如余额不足、用户不存在），返回明确的错误码和消息</li>
 *   <li>RuntimeException：系统级异常（如 NPE、数组越界），返回 500</li>
 * </ul>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}

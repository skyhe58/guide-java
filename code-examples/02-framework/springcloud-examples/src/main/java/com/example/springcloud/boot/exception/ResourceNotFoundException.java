package com.example.springcloud.boot.exception;

/**
 * 资源不存在异常 — 当请求的资源（如用户、订单）不存在时抛出
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(404, resourceType + " 不存在，ID: " + id);
    }
}

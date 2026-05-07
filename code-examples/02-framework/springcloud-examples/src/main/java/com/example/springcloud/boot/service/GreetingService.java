package com.example.springcloud.boot.service;

/**
 * 问候服务接口 — 用于演示 Spring IoC/DI 的多实现注入
 */
public interface GreetingService {
    /** 返回问候语 */
    String greet(String name);

    /** 返回实现名称 */
    String getImplName();
}

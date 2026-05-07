package com.example.springcloud.boot.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 中文问候服务 — 标记为 @Primary，当按类型注入时优先使用
 */
@Service("chineseGreeting")
@Primary
public class ChineseGreetingService implements GreetingService {

    @Override
    public String greet(String name) {
        return "你好，" + name + "！欢迎使用 Spring IoC 容器";
    }

    @Override
    public String getImplName() {
        return "ChineseGreetingService（@Primary）";
    }
}

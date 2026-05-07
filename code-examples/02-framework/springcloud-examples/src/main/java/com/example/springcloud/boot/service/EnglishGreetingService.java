package com.example.springcloud.boot.service;

import org.springframework.stereotype.Service;

/**
 * 英文问候服务 — 通过 @Qualifier("englishGreeting") 指定注入
 */
@Service("englishGreeting")
public class EnglishGreetingService implements GreetingService {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "! Welcome to Spring IoC Container";
    }

    @Override
    public String getImplName() {
        return "EnglishGreetingService";
    }
}

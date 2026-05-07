package com.example.springcloud.boot.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Prototype 作用域 Bean — 每次注入都会创建新实例
 *
 * <p>与默认的 Singleton 作用域对比：
 * <ul>
 *   <li>Singleton：整个容器只有一个实例（默认）</li>
 *   <li>Prototype：每次 getBean() 都创建新实例</li>
 * </ul>
 */
@Service
@Scope("prototype")
public class PrototypeDemoService {

    /** 实例唯一标识，用于证明每次获取的是不同实例 */
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    public String getInstanceId() {
        return instanceId;
    }
}

package com.example.springcloud.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端 — 声明式 HTTP 调用
 *
 * <p>通过接口 + 注解的方式定义远程调用，OpenFeign 自动生成代理实现。
 * <ul>
 *   <li>name: 目标服务名（注册中心中的服务名）</li>
 *   <li>fallbackFactory: 降级工厂（可获取异常信息，比 fallback 更推荐）</li>
 * </ul>
 */
@FeignClient(name = "user-service", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 根据 ID 获取用户
     */
    @GetMapping("/users/{id}")
    UserDTO getUser(@PathVariable("id") Long id);

    /**
     * 创建用户
     */
    @PostMapping("/users")
    UserDTO createUser(@RequestBody UserDTO user);

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    List<UserDTO> listUsers();
}

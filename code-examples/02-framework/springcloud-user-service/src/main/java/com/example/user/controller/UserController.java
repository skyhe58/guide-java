package com.example.user.controller;

import com.example.user.entity.User;
import com.example.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 *
 * 提供用户 CRUD 接口，被 springcloud-examples 通过 Feign 调用。
 * 完整调用链路：客户端 → Gateway(8080) → demo-service(8090) → [Feign] → user-service(8092)
 *
 * 测试命令：
 * {@code curl http://localhost:8092/users/init}
 * {@code curl http://localhost:8092/users}
 * {@code curl http://localhost:8092/users/1}
 * {@code curl -X POST http://localhost:8092/users -H "Content-Type: application/json" -d '{"name":"测试用户","email":"test@example.com","age":25}'}
 * {@code curl "http://localhost:8092/users/search?name=张"}
 *
 * 通过 Gateway 访问：
 * {@code curl http://localhost:8080/api/user/users/init -H "Authorization: Bearer test-token"}
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 初始化测试表和数据
     *
     * curl http://localhost:8092/users/init
     */
    @GetMapping("/init")
    public Map<String, Object> init() {
        String message = userService.initTable();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", message);
        result.put("service", "user-service");
        result.put("port", 8092);
        return result;
    }

    /**
     * 根据 ID 查询用户（被 Feign 调用）
     *
     * curl http://localhost:8092/users/1
     */
    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * 创建用户
     *
     * curl -X POST http://localhost:8092/users \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"测试用户","email":"test@example.com","age":25}'
     */
    @PostMapping
    public User create(@RequestBody User user) {
        return userService.create(user);
    }

    /**
     * 列出所有用户
     *
     * curl http://localhost:8092/users
     */
    @GetMapping
    public List<User> listAll() {
        return userService.findAll();
    }

    /**
     * 按名称搜索用户
     *
     * curl "http://localhost:8092/users/search?name=张"
     */
    @GetMapping("/search")
    public List<User> search(@RequestParam String name) {
        return userService.searchByName(name);
    }
}

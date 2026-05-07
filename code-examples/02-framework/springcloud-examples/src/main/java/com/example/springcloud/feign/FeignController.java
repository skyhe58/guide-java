package com.example.springcloud.feign;

import com.example.springcloud.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 10.A.12 OpenFeign 声明式调用实战 Controller
 *
 * <p>演示 Spring Cloud OpenFeign 的核心功能：
 * <ul>
 *   <li>声明式 HTTP 调用（接口 + 注解，无需手写 HTTP 客户端）</li>
 *   <li>FallbackFactory 降级处理（服务不可用时返回兜底数据）</li>
 *   <li>与 Resilience4j 熔断器集成</li>
 * </ul>
 *
 * <p>注意：需要启动 user-service 才能正常调用，否则会触发 Fallback 降级。
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 根据 ID 获取用户（user-service 未启动时触发 Fallback）
 * curl http://localhost:8090/demo/feign/user/1
 *
 * # 获取用户列表
 * curl http://localhost:8090/demo/feign/users
 *
 * # 创建用户
 * curl -X POST http://localhost:8090/demo/feign/user \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"张三","email":"zhangsan@example.com","phone":"13800138000"}'
 *
 * # 模拟调用失败触发 Fallback
 * curl http://localhost:8090/demo/feign/fallback
 * </pre>
 */
@RestController
@RequestMapping("/demo/feign")
public class FeignController {

    private final UserFeignClient userFeignClient;

    public FeignController(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    /**
     * 根据 ID 获取用户 — 调用 UserFeignClient.getUser
     *
     * @param id 用户 ID
     */
    @GetMapping("/user/{id}")
    public Result<Map<String, Object>> getUser(@PathVariable Long id) {
        UserDTO user = userFeignClient.getUser(id);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "通过 OpenFeign 调用 user-service 获取用户");
        data.put("user", user);
        return Result.ok(data);
    }

    /**
     * 获取用户列表 — 调用 UserFeignClient.listUsers
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers() {
        List<UserDTO> users = userFeignClient.listUsers();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "通过 OpenFeign 调用 user-service 获取用户列表");
        data.put("数量", users.size());
        data.put("users", users);
        return Result.ok(data);
    }

    /**
     * 创建用户 — 调用 UserFeignClient.createUser
     */
    @PostMapping("/user")
    public Result<Map<String, Object>> createUser(@RequestBody UserDTO user) {
        UserDTO created = userFeignClient.createUser(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "通过 OpenFeign 调用 user-service 创建用户");
        data.put("user", created);
        return Result.ok(data);
    }

    /**
     * 模拟调用失败触发 Fallback — 调用一个不存在的用户 ID
     */
    @GetMapping("/fallback")
    public Result<Map<String, Object>> fallbackDemo() {
        // user-service 未启动时，所有调用都会触发 FallbackFactory
        UserDTO user = userFeignClient.getUser(-1L);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "Feign 调用失败时，FallbackFactory 返回降级数据");
        data.put("降级数据", user);
        data.put("提示", "查看控制台日志，会看到 [Feign Fallback] 和 [Fallback] 开头的日志");
        return Result.ok(data);
    }
}

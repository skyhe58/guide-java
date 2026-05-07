package com.example.springcloud.boot;

import com.example.springcloud.boot.annotation.LogExecution;
import com.example.springcloud.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10.A.8 Spring AOP 切面编程演示 Controller
 *
 * <p>演示 Spring AOP 的核心功能：
 * <ul>
 *   <li>自定义注解 @LogExecution + 日志切面（记录方法执行时间）</li>
 *   <li>慢方法告警（耗时超过阈值自动 WARN）</li>
 *   <li>权限切面（检查请求头中的 Authorization token）</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 触发日志切面（自动记录执行时间）
 * curl http://localhost:8090/demo/boot/aop/log-demo
 *
 * # 模拟慢方法（sleep 500ms，触发耗时告警）
 * curl http://localhost:8090/demo/boot/aop/slow-method
 *
 * # 需要 Authorization 头的接口（无 token → 401）
 * curl http://localhost:8090/demo/boot/aop/auth-check
 *
 * # 携带 token 调用
 * curl -H "Authorization: Bearer my-token-123" http://localhost:8090/demo/boot/aop/auth-check
 * </pre>
 */
@RestController
@RequestMapping("/demo/boot/aop")
public class AopController {

    /**
     * 触发日志切面 — @LogExecution 注解会被 LogAspect 拦截
     * <p>切面自动记录：方法名、参数、返回值、执行耗时
     */
    @GetMapping("/log-demo")
    @LogExecution("AOP日志演示")
    public Result<Map<String, Object>> logDemo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "此方法被 @LogExecution 注解标记，LogAspect 自动记录执行日志");
        data.put("切面功能", "记录方法名、参数、执行耗时");
        data.put("查看日志", "请查看控制台输出，会看到 [AOP] 开头的日志");
        data.put("时间戳", System.currentTimeMillis());
        return Result.ok(data);
    }

    /**
     * 模拟慢方法 — sleep 500ms，触发 LogAspect 的耗时告警（阈值 300ms）
     */
    @GetMapping("/slow-method")
    @LogExecution("慢方法模拟")
    public Result<Map<String, Object>> slowMethod() throws InterruptedException {
        // 模拟耗时操作（如复杂数据库查询、远程调用等）
        Thread.sleep(500);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "此方法 sleep 500ms，超过 LogAspect 的 300ms 阈值");
        data.put("预期行为", "控制台会输出 WARN 级别的慢方法告警日志");
        data.put("模拟耗时", "500ms");
        data.put("告警阈值", "300ms");
        return Result.ok(data);
    }

    /**
     * 需要 Authorization 头的接口 — 被 AuthAspect 拦截
     * <p>如果请求头中没有 Authorization，切面直接返回 401
     */
    @GetMapping("/auth-check")
    public Result<Map<String, String>> authCheck() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("说明", "鉴权通过！此方法被 AuthAspect 保护");
        data.put("切面逻辑", "AuthAspect 检查请求头中的 Authorization token");
        data.put("状态", "已授权");
        return Result.ok(data);
    }
}

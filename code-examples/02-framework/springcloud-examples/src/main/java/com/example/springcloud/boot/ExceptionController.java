package com.example.springcloud.boot;

import com.example.springcloud.boot.exception.BusinessException;
import com.example.springcloud.boot.exception.ResourceNotFoundException;
import com.example.springcloud.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10.A.9 全局异常处理演示 Controller
 *
 * <p>演示 Spring Boot 的异常处理机制：
 * <ul>
 *   <li>@ControllerAdvice + @ExceptionHandler 全局异常捕获</li>
 *   <li>自定义 BusinessException 业务异常</li>
 *   <li>ResourceNotFoundException 资源不存在异常</li>
 *   <li>统一 Result 响应格式</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 触发业务异常（余额不足）
 * curl http://localhost:8090/demo/boot/exception/business
 *
 * # 触发参数校验异常
 * curl http://localhost:8090/demo/boot/exception/validation
 *
 * # 触发运行时异常
 * curl http://localhost:8090/demo/boot/exception/runtime
 *
 * # 触发资源不存在异常
 * curl "http://localhost:8090/demo/boot/exception/not-found?id=999"
 * </pre>
 */
@RestController
@RequestMapping("/demo/boot/exception")
public class ExceptionController {

    /**
     * 触发业务异常 — 模拟余额不足场景
     */
    @GetMapping("/business")
    public Result<Void> businessException() {
        // 模拟业务逻辑：检查余额
        double balance = 50.0;
        double amount = 100.0;
        if (balance < amount) {
            throw new BusinessException(4001, "余额不足：当前余额 " + balance + "，需要 " + amount);
        }
        return Result.ok();
    }

    /**
     * 触发参数校验异常 — 模拟手动校验失败
     */
    @GetMapping("/validation")
    public Result<Void> validationException() {
        // 模拟参数校验
        String email = "not-a-valid-email";
        if (!email.contains("@")) {
            throw new BusinessException(4002, "参数校验失败：邮箱格式不正确 → " + email);
        }
        return Result.ok();
    }

    /**
     * 触发运行时异常 — 模拟空指针等系统级异常
     */
    @GetMapping("/runtime")
    public Result<Void> runtimeException() {
        // 模拟运行时异常
        Map<String, String> map = new LinkedHashMap<>();
        // 故意触发异常
        String value = map.get("nonexistent");
        if (value == null) {
            throw new RuntimeException("模拟运行时异常：尝试访问不存在的配置项 'nonexistent'");
        }
        return Result.ok();
    }

    /**
     * 触发资源不存在异常 — 模拟查询不存在的用户
     *
     * @param id 用户 ID
     */
    @GetMapping("/not-found")
    public Result<Void> notFound(@RequestParam(defaultValue = "999") Long id) {
        // 模拟数据库查询
        if (id > 100) {
            throw new ResourceNotFoundException("用户", id);
        }
        return Result.ok();
    }
}

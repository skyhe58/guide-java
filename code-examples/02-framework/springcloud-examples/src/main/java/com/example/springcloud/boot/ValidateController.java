package com.example.springcloud.boot;

import com.example.springcloud.boot.dto.CreateOrderRequest;
import com.example.springcloud.boot.dto.CreateUserRequest;
import com.example.springcloud.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10.A.10 参数校验演示 Controller
 *
 * <p>演示 Spring Boot 参数校验的多种方式：
 * <ul>
 *   <li>@Valid + @RequestBody：校验 JSON 请求体</li>
 *   <li>@Validated + 分组校验：不同场景使用不同校验规则</li>
 *   <li>@Validated + @RequestParam：校验请求参数</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 校验用户创建参数（正确参数）
 * curl -X POST http://localhost:8090/demo/boot/validate/user \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"张三","email":"zhangsan@example.com","age":25,"phone":"13800138000"}'
 *
 * # 校验用户创建参数（错误参数 → 触发校验失败）
 * curl -X POST http://localhost:8090/demo/boot/validate/user \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"","email":"not-email","age":200,"phone":"123"}'
 *
 * # 校验订单参数（分组校验 — Create 分组）
 * curl -X POST http://localhost:8090/demo/boot/validate/order \
 *   -H "Content-Type: application/json" \
 *   -d '{"productName":"iPhone 15","quantity":2,"amount":9999.00}'
 *
 * # 校验订单参数（缺少必填字段 → 触发校验失败）
 * curl -X POST http://localhost:8090/demo/boot/validate/order \
 *   -H "Content-Type: application/json" \
 *   -d '{"quantity":0}'
 *
 * # 自定义手机号校验
 * curl "http://localhost:8090/demo/boot/validate/phone?number=13800138000"
 * curl "http://localhost:8090/demo/boot/validate/phone?number=123456"
 * </pre>
 */
@RestController
@RequestMapping("/demo/boot/validate")
@Validated
public class ValidateController {

    /**
     * 校验用户创建参数 — @Valid + @RequestBody
     *
     * <p>校验规则：
     * <ul>
     *   <li>name: @NotBlank + @Size(2-20)</li>
     *   <li>email: @NotBlank + @Email</li>
     *   <li>age: @Min(1) + @Max(150)</li>
     *   <li>phone: @Pattern(手机号正则)</li>
     * </ul>
     */
    @PostMapping("/user")
    public Result<Map<String, Object>> validateUser(@Valid @RequestBody CreateUserRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "参数校验通过！用户创建成功");
        data.put("name", request.getName());
        data.put("email", request.getEmail());
        data.put("age", request.getAge());
        data.put("phone", request.getPhone());
        return Result.ok(data);
    }

    /**
     * 校验订单参数 — @Validated + 分组校验（Create 分组）
     */
    @PostMapping("/order")
    public Result<Map<String, Object>> validateOrder(
            @Validated(CreateOrderRequest.Create.class) @RequestBody CreateOrderRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "分组校验通过！订单创建成功（使用 Create 分组）");
        data.put("productName", request.getProductName());
        data.put("quantity", request.getQuantity());
        data.put("amount", request.getAmount());
        return Result.ok(data);
    }

    /**
     * 自定义手机号校验 — @Validated + @Pattern 在 @RequestParam 上
     *
     * @param number 手机号
     */
    @GetMapping("/phone")
    public Result<Map<String, Object>> validatePhone(
            @RequestParam
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确，必须是 1 开头的 11 位数字")
            String number) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "手机号校验通过");
        data.put("手机号", number);
        data.put("校验规则", "^1[3-9]\\d{9}$（1 开头的 11 位数字）");
        return Result.ok(data);
    }
}

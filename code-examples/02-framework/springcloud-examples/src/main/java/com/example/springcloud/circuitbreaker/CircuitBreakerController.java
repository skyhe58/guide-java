package com.example.springcloud.circuitbreaker;

import com.example.springcloud.common.Result;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 10.A.13 Resilience4j 熔断降级实战 Controller
 *
 * <p>演示 Resilience4j 熔断器的核心功能：
 * <ul>
 *   <li>CLOSED 状态：正常通行，记录成功/失败</li>
 *   <li>OPEN 状态：快速失败，执行降级逻辑</li>
 *   <li>HALF_OPEN 状态：放行少量探测请求</li>
 *   <li>手动查看和重置熔断器状态</li>
 * </ul>
 *
 * <p>配置参见 application.yml 中的 resilience4j.circuitbreaker.instances.demoService
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 正常调用（CLOSED 状态）
 * curl http://localhost:8090/demo/cb/success
 *
 * # 模拟失败（多次调用触发熔断 → OPEN）
 * curl http://localhost:8090/demo/cb/fail
 *
 * # 查看熔断器当前状态
 * curl http://localhost:8090/demo/cb/status
 *
 * # 重置熔断器
 * curl http://localhost:8090/demo/cb/reset
 *
 * # 完整测试流程：
 * # 1. 先查看状态（CLOSED）
 * # 2. 连续调用 /fail 多次（触发熔断）
 * # 3. 再查看状态（OPEN）
 * # 4. 调用 /success（被熔断，返回降级数据）
 * # 5. 重置后恢复正常
 * </pre>
 */
@RestController
@RequestMapping("/demo/cb")
public class CircuitBreakerController {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerController.class);

    private static final String CB_NAME = "demoService";

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * 正常调用 — CLOSED 状态下正常通行
     */
    @GetMapping("/success")
    public Result<Map<String, Object>> success() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_NAME);

        try {
            String result = CircuitBreaker.decorateSupplier(cb, () -> {
                log.info("[CircuitBreaker] 正常调用成功");
                return "调用成功！服务正常响应";
            }).get();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("说明", "正常调用，熔断器处于 CLOSED 状态");
            data.put("结果", result);
            data.put("熔断器状态", cb.getState().name());
            data.put("失败率", cb.getMetrics().getFailureRate() + "%");
            return Result.ok(data);

        } catch (Exception e) {
            // 熔断器 OPEN 时会抛出 CallNotPermittedException
            log.warn("[CircuitBreaker] 调用被熔断: {}", e.getMessage());
            return fallbackResponse(cb, "success", e.getMessage());
        }
    }

    /**
     * 模拟失败 — 连续调用触发熔断（CLOSED → OPEN）
     */
    @GetMapping("/fail")
    public Result<Map<String, Object>> fail() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_NAME);

        try {
            String result = CircuitBreaker.decorateSupplier(cb, (Supplier<String>) () -> {
                log.error("[CircuitBreaker] 模拟调用失败");
                throw new RuntimeException("模拟服务超时/不可用");
            }).get();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("结果", result);
            return Result.ok(data);

        } catch (Exception e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("说明", "调用失败，熔断器记录失败次数");
            data.put("异常", e.getMessage());
            data.put("熔断器状态", cb.getState().name());
            data.put("失败率", cb.getMetrics().getFailureRate() + "%");
            data.put("调用总数", cb.getMetrics().getNumberOfBufferedCalls());
            data.put("失败次数", cb.getMetrics().getNumberOfFailedCalls());
            data.put("提示", "连续调用此接口多次，失败率超过阈值后熔断器将变为 OPEN 状态");
            return Result.fail(500, "调用失败（熔断器记录中）");
        }
    }

    /**
     * 查看熔断器当前状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("熔断器名称", CB_NAME);
        data.put("当前状态", cb.getState().name());
        data.put("失败率", metrics.getFailureRate() + "%");
        data.put("慢调用率", metrics.getSlowCallRate() + "%");
        data.put("调用总数", metrics.getNumberOfBufferedCalls());
        data.put("成功次数", metrics.getNumberOfSuccessfulCalls());
        data.put("失败次数", metrics.getNumberOfFailedCalls());
        data.put("未允许调用数", metrics.getNumberOfNotPermittedCalls());

        // 配置信息
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("滑动窗口大小", cb.getCircuitBreakerConfig().getSlidingWindowSize());
        config.put("失败率阈值", cb.getCircuitBreakerConfig().getFailureRateThreshold() + "%");
        config.put("OPEN等待时间", cb.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1) + "ms");
        config.put("HALF_OPEN允许调用数", cb.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState());
        config.put("滑动窗口类型", cb.getCircuitBreakerConfig().getSlidingWindowType().name());
        data.put("配置", config);

        return Result.ok(data);
    }

    /**
     * 重置熔断器 — 恢复到 CLOSED 状态
     */
    @GetMapping("/reset")
    public Result<Map<String, Object>> reset() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        String oldState = cb.getState().name();

        cb.reset();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "熔断器已重置");
        data.put("重置前状态", oldState);
        data.put("重置后状态", cb.getState().name());
        return Result.ok(data);
    }

    /**
     * 降级响应 — 熔断器 OPEN 时的兜底逻辑
     */
    private Result<Map<String, Object>> fallbackResponse(CircuitBreaker cb, String method, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "熔断器已打开，请求被快速拒绝（降级响应）");
        data.put("调用方法", method);
        data.put("降级原因", reason);
        data.put("熔断器状态", cb.getState().name());
        data.put("提示", "调用 /demo/cb/reset 可重置熔断器");
        return Result.fail(503, "服务熔断中，返回降级数据");
    }
}

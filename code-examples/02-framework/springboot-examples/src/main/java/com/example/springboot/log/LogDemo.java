package com.example.springboot.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 日志体系演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>SLF4J 日志门面使用</li>
 *   <li>MDC 链路追踪（traceId）</li>
 *   <li>日志级别使用规范</li>
 * </ul>
 */
@Service
public class LogDemo {

    private static final Logger log = LoggerFactory.getLogger(LogDemo.class);

    /**
     * 演示不同日志级别的使用
     *
     * <p>日志级别从低到高：TRACE < DEBUG < INFO < WARN < ERROR</p>
     * <ul>
     *   <li>TRACE：最细粒度，一般不用</li>
     *   <li>DEBUG：调试信息，开发环境使用</li>
     *   <li>INFO：重要业务信息，生产环境默认级别</li>
     *   <li>WARN：警告信息，需要关注但不影响运行</li>
     *   <li>ERROR：错误信息，需要立即处理</li>
     * </ul>
     */
    public void demonstrateLogLevels() {
        log.trace("TRACE 级别 — 最细粒度的跟踪信息");
        log.debug("DEBUG 级别 — 调试信息，开发环境使用");
        log.info("INFO 级别 — 重要业务信息");
        log.warn("WARN 级别 — 警告信息，需要关注");
        log.error("ERROR 级别 — 错误信息，需要立即处理");
    }

    /**
     * 演示 MDC 链路追踪
     *
     * <p>MDC（Mapped Diagnostic Context）可以在日志中自动携带上下文信息。
     * 配合日志格式中的 %X{traceId} 使用，实现请求链路追踪。</p>
     *
     * <p>使用步骤：</p>
     * <ol>
     *   <li>在请求入口（Filter/Interceptor）设置 MDC</li>
     *   <li>日志格式中引用 %X{traceId}</li>
     *   <li>请求结束后清除 MDC（防止内存泄漏）</li>
     * </ol>
     *
     * @param orderId 订单ID
     */
    public void demonstrateMDC(String orderId) {
        // 生成 traceId 并放入 MDC
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        MDC.put("orderId", orderId);

        try {
            log.info("开始处理订单: {}", orderId);
            log.info("查询订单详情...");
            log.info("更新库存...");
            log.info("订单处理完成");
        } finally {
            // 必须清除 MDC，防止线程池复用导致内存泄漏
            MDC.clear();
        }
    }

    /**
     * 演示日志最佳实践
     */
    public void logBestPractices() {
        // ✅ 使用占位符，避免字符串拼接
        String userId = "12345";
        log.info("查询用户信息, userId={}", userId);

        // ❌ 不推荐：字符串拼接（即使日志级别不输出也会执行拼接）
        // log.debug("查询用户信息, userId=" + userId);

        // ✅ 记录异常时传入异常对象（打印完整堆栈）
        try {
            int result = 1 / 0;
        } catch (ArithmeticException e) {
            log.error("计算异常, userId={}", userId, e);
        }

        // ✅ 使用 isDebugEnabled() 避免不必要的计算
        if (log.isDebugEnabled()) {
            log.debug("详细调试信息: {}", buildExpensiveDebugInfo());
        }
    }

    private String buildExpensiveDebugInfo() {
        return "expensive-debug-info";
    }
}

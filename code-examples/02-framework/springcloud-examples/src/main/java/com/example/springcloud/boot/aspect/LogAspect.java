package com.example.springcloud.boot.aspect;

import com.example.springcloud.boot.annotation.LogExecution;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 日志切面 — 拦截 @LogExecution 注解标记的方法
 *
 * <p>功能：
 * <ul>
 *   <li>记录方法名、参数、返回值</li>
 *   <li>记录执行耗时</li>
 *   <li>耗时超过 300ms 输出 WARN 级别告警</li>
 * </ul>
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** 慢方法阈值（毫秒） */
    private static final long SLOW_THRESHOLD_MS = 300;

    @Around("@annotation(logExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String desc = logExecution.value().isEmpty() ? methodName : logExecution.value();

        log.info("[AOP] 开始执行: {} | 参数: {}", desc, Arrays.toString(joinPoint.getArgs()));

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        if (elapsed > SLOW_THRESHOLD_MS) {
            log.warn("[AOP] 慢方法告警: {} | 耗时: {}ms（超过阈值 {}ms）", desc, elapsed, SLOW_THRESHOLD_MS);
        } else {
            log.info("[AOP] 执行完成: {} | 耗时: {}ms", desc, elapsed);
        }

        return result;
    }
}

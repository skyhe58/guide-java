package com.example.springcloud.boot.aspect;

import com.example.springcloud.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限切面 — 检查请求头中的 Authorization token
 *
 * <p>拦截 AopController 中的 authCheck 方法，
 * 如果请求头中没有 Authorization 则返回 401。
 */
@Aspect
@Component
public class AuthAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthAspect.class);

    /**
     * 拦截标记了 @RequireAuth 的方法（这里用切点表达式直接匹配方法）
     */
    @Around("execution(* com.example.springcloud.boot.AopController.authCheck(..))")
    public Object checkAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Result.fail(500, "无法获取请求上下文");
        }

        HttpServletRequest request = attrs.getRequest();
        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            log.warn("[AuthAspect] 请求被拦截：缺少 Authorization 头");
            return Result.fail(401, "未授权：请在请求头中携带 Authorization token");
        }

        log.info("[AuthAspect] 鉴权通过，token: {}...", token.substring(0, Math.min(token.length(), 20)));
        return joinPoint.proceed();
    }
}

package com.example.springcloud.boot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解 — 标记需要记录执行日志的方法
 *
 * <p>被此注解标记的方法会被 {@link com.example.springcloud.boot.aspect.LogAspect} 拦截，
 * 自动记录方法名、参数、返回值和执行耗时。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    /** 操作描述（可选） */
    String value() default "";
}

package com.example.springboot.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * AOP 原理与事务管理演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>自定义切面（@Aspect）</li>
 *   <li>切面执行顺序（@Around → @Before → 目标方法 → @AfterReturning → @After）</li>
 *   <li>事务失效场景说明</li>
 *   <li>多切面排序（@Order）</li>
 * </ul>
 */
public class AopDemo {

    // ==================== 1. 自定义日志切面 ====================

    /**
     * 日志切面 — 记录方法执行时间和参数
     *
     * <p>切面执行顺序（正常情况）：</p>
     * <ol>
     *   <li>@Around（proceed 之前）</li>
     *   <li>@Before</li>
     *   <li>目标方法执行</li>
     *   <li>@AfterReturning</li>
     *   <li>@After</li>
     *   <li>@Around（proceed 之后）</li>
     * </ol>
     */
    @Aspect
    @Component
    @Order(1) // 值越小优先级越高
    public static class LogAspect {

        private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

        // 切入点：匹配 web 包下所有 public 方法
        @Pointcut("execution(public * com.example.springboot.web..*.*(..))")
        public void webPointcut() {
        }

        @Around("webPointcut()")
        public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
            String methodName = joinPoint.getSignature().toShortString();
            long start = System.currentTimeMillis();
            log.info("[Around-前] 方法: {}, 参数: {}", methodName, joinPoint.getArgs());

            try {
                Object result = joinPoint.proceed();
                long elapsed = System.currentTimeMillis() - start;
                log.info("[Around-后] 方法: {}, 耗时: {}ms", methodName, elapsed);
                return result;
            } catch (Throwable e) {
                log.error("[Around-异常] 方法: {}, 异常: {}", methodName, e.getMessage());
                throw e;
            }
        }

        @Before("webPointcut()")
        public void before() {
            log.info("[Before] 方法执行前");
        }

        @AfterReturning(pointcut = "webPointcut()", returning = "result")
        public void afterReturning(Object result) {
            log.info("[AfterReturning] 返回值: {}", result);
        }

        @AfterThrowing(pointcut = "webPointcut()", throwing = "ex")
        public void afterThrowing(Throwable ex) {
            log.error("[AfterThrowing] 异常: {}", ex.getMessage());
        }

        @After("webPointcut()")
        public void after() {
            log.info("[After] 方法执行完毕（无论是否异常）");
        }
    }

    // ==================== 2. 事务失效场景说明 ====================

    /**
     * 事务失效场景演示（注释说明，不实际执行事务操作）
     *
     * <p>@Transactional 事务失效的 8 种场景：</p>
     * <ol>
     *   <li>方法不是 public — AOP 默认只拦截 public 方法</li>
     *   <li>同类内部方法调用 — 不经过代理对象，事务不生效</li>
     *   <li>方法用 final 修饰 — CGLIB 无法重写 final 方法</li>
     *   <li>非 Spring 管理的类 — 没有被代理</li>
     *   <li>异常被 catch 吞掉 — 事务管理器感知不到异常</li>
     *   <li>抛出 checked 异常 — 默认只回滚 RuntimeException</li>
     *   <li>数据库引擎不支持事务 — MyISAM 不支持</li>
     *   <li>传播行为设置不当 — 如 NOT_SUPPORTED</li>
     * </ol>
     */
    @Service
    public static class TransactionFailureDemo {

        private static final Logger log = LoggerFactory.getLogger(TransactionFailureDemo.class);

        /**
         * 场景2：同类内部调用导致事务失效
         *
         * <p>this.doSomethingTransactional() 是直接调用，
         * 不经过 Spring 代理对象，因此 @Transactional 不生效。</p>
         *
         * <p>解决方案：</p>
         * <ul>
         *   <li>注入自身（@Autowired private TransactionFailureDemo self）</li>
         *   <li>使用 AopContext.currentProxy()</li>
         *   <li>将事务方法拆分到另一个 Service</li>
         * </ul>
         */
        public void callInternalMethod() {
            log.warn("同类内部调用 — 事务不会生效！");
            // this.doSomethingTransactional(); // ❌ 事务失效
        }

        // @Transactional // 被内部调用时不生效
        public void doSomethingTransactional() {
            log.info("执行事务操作...");
        }

        /**
         * 场景6：抛出 checked 异常导致事务不回滚
         *
         * <p>默认只回滚 RuntimeException 和 Error。
         * 解决方案：@Transactional(rollbackFor = Exception.class)</p>
         */
        // @Transactional // 默认不回滚 checked 异常
        public void checkedExceptionDemo() throws Exception {
            log.warn("抛出 checked 异常 — 默认不回滚！");
            throw new Exception("checked exception");
        }
    }
}

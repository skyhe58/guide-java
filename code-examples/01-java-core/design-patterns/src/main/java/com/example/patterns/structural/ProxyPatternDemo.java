package com.example.patterns.structural;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 代理模式演示 — 静态代理 + JDK 动态代理 + CGLIB 代理
 * <p>
 * 业务场景：用户服务的日志增强
 * - 静态代理：手动编写代理类
 * - JDK 动态代理：基于接口 + InvocationHandler
 * - CGLIB 代理：基于字节码生成子类（此处用模拟方式演示原理）
 * </p>
 */
public class ProxyPatternDemo {

    public static void main(String[] args) {
        System.out.println("========== 代理模式演示 ==========\n");

        demonstrateStaticProxy();
        System.out.println();
        demonstrateJdkDynamicProxy();
        System.out.println();
        demonstrateCglibStyleProxy();
    }

    // ==================== 公共接口和实现 ====================

    /** 用户服务接口 */
    public interface UserService {
        void save(String name);
        String findByName(String name);
    }

    /** 用户服务实现 */
    static class UserServiceImpl implements UserService {
        @Override
        public void save(String name) {
            System.out.println("    [业务] 保存用户: " + name);
        }

        @Override
        public String findByName(String name) {
            System.out.println("    [业务] 查询用户: " + name);
            return "User(" + name + ")";
        }
    }

    // ==================== 1. 静态代理 ====================

    /**
     * 静态代理类：编译期确定代理关系
     * 优点：简单直观
     * 缺点：每个接口都需要一个代理类，代码量大
     */
    static class UserServiceStaticProxy implements UserService {
        private final UserService target;

        UserServiceStaticProxy(UserService target) {
            this.target = target;
        }

        @Override
        public void save(String name) {
            System.out.println("    [静态代理] 前置：记录日志");
            long start = System.currentTimeMillis();
            target.save(name);
            long cost = System.currentTimeMillis() - start;
            System.out.println("    [静态代理] 后置：耗时 " + cost + "ms");
        }

        @Override
        public String findByName(String name) {
            System.out.println("    [静态代理] 前置：记录日志");
            String result = target.findByName(name);
            System.out.println("    [静态代理] 后置：返回结果 " + result);
            return result;
        }
    }

    private static void demonstrateStaticProxy() {
        System.out.println("--- 1. 静态代理 ---");
        UserService target = new UserServiceImpl();
        UserService proxy = new UserServiceStaticProxy(target);
        proxy.save("张三");
        proxy.findByName("李四");
    }

    // ==================== 2. JDK 动态代理 ====================

    /**
     * JDK 动态代理：基于 InvocationHandler + Proxy.newProxyInstance
     * 要求：目标类必须实现接口
     * 原理：运行时通过反射生成代理类
     */
    static class LogInvocationHandler implements InvocationHandler {
        private final Object target;

        LogInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("    [JDK代理] 前置：调用方法 " + method.getName());
            long start = System.currentTimeMillis();
            Object result = method.invoke(target, args);
            long cost = System.currentTimeMillis() - start;
            System.out.println("    [JDK代理] 后置：耗时 " + cost + "ms");
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T createJdkProxy(T target) {
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new LogInvocationHandler(target)
        );
    }

    private static void demonstrateJdkDynamicProxy() {
        System.out.println("--- 2. JDK 动态代理 ---");
        UserService target = new UserServiceImpl();
        UserService proxy = createJdkProxy(target);

        proxy.save("王五");
        String result = proxy.findByName("赵六");
        System.out.println("    查询结果: " + result);

        // 验证代理类信息
        System.out.println("    代理类: " + proxy.getClass().getName());
        System.out.println("    是否是 UserService: " + (proxy instanceof UserService));
    }

    // ==================== 3. CGLIB 风格代理（模拟） ====================

    /**
     * CGLIB 代理原理演示（通过继承模拟）
     * <p>
     * 真实 CGLIB 通过 ASM 字节码框架在运行时生成目标类的子类，
     * 这里通过手动继承来演示其核心原理。
     * </p>
     * 特点：不需要接口，通过生成子类实现代理
     * 限制：目标类不能是 final，目标方法不能是 final
     */
    static class OrderService {
        public void createOrder(String orderId) {
            System.out.println("    [业务] 创建订单: " + orderId);
        }
    }

    /** 模拟 CGLIB 生成的子类代理 */
    static class OrderServiceCglibProxy extends OrderService {
        private final OrderService target;

        OrderServiceCglibProxy(OrderService target) {
            this.target = target;
        }

        @Override
        public void createOrder(String orderId) {
            System.out.println("    [CGLIB代理] 前置：开启事务");
            target.createOrder(orderId);
            System.out.println("    [CGLIB代理] 后置：提交事务");
        }
    }

    private static void demonstrateCglibStyleProxy() {
        System.out.println("--- 3. CGLIB 风格代理（继承方式模拟） ---");
        OrderService target = new OrderService();
        OrderService proxy = new OrderServiceCglibProxy(target);
        proxy.createOrder("ORD-20240101-001");

        System.out.println("\n    CGLIB 代理 vs JDK 动态代理：");
        System.out.println("    JDK 动态代理：基于接口，通过反射调用");
        System.out.println("    CGLIB 代理：基于继承，通过字节码生成子类");
        System.out.println("    Spring AOP：有接口用 JDK，无接口用 CGLIB");
    }
}

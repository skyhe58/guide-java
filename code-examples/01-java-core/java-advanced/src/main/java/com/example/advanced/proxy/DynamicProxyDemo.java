package com.example.advanced.proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 动态代理演示
 * <p>
 * 演示内容：
 * 1. JDK 动态代理（Proxy + InvocationHandler）
 * 2. CGLIB 代理（Enhancer + MethodInterceptor）
 * 3. 性能对比
 * 4. 模拟 Spring AOP 代理选择
 * </p>
 */
public class DynamicProxyDemo {

    // ==================== 接口和实现类定义 ====================

    /** 用户服务接口 */
    public interface UserService {
        String findUser(String name);
        void saveUser(String name);
    }

    /** 用户服务实现 */
    public static class UserServiceImpl implements UserService {
        @Override
        public String findUser(String name) {
            return "User: " + name;
        }

        @Override
        public void saveUser(String name) {
            System.out.println("    [业务] 保存用户: " + name);
        }
    }

    /** 没有实现接口的服务类（只能用 CGLIB 代理） */
    public static class OrderService {
        public String createOrder(String product) {
            return "Order for: " + product;
        }

        public void cancelOrder(String orderId) {
            System.out.println("    [业务] 取消订单: " + orderId);
        }
    }

    // ==================== JDK 动态代理 ====================

    /**
     * JDK 动态代理的 InvocationHandler
     * 实现日志增强功能
     */
    public static class JdkLoggingHandler implements InvocationHandler {
        private final Object target;

        public JdkLoggingHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("    [JDK 代理] 前置: 调用 " + method.getName()
                    + "(" + (args != null ? String.join(", ", toStringArray(args)) : "") + ")");
            long start = System.nanoTime();

            Object result = method.invoke(target, args);

            long cost = (System.nanoTime() - start) / 1000;
            System.out.println("    [JDK 代理] 后置: " + method.getName()
                    + " 耗时 " + cost + "μs, 返回: " + result);
            return result;
        }

        /**
         * 创建 JDK 动态代理实例
         */
        @SuppressWarnings("unchecked")
        public static <T> T createProxy(T target, Class<?>... interfaces) {
            return (T) Proxy.newProxyInstance(
                    target.getClass().getClassLoader(),
                    interfaces,
                    new JdkLoggingHandler(target)
            );
        }
    }

    // ==================== CGLIB 代理 ====================

    /**
     * CGLIB 代理的 MethodInterceptor
     * 实现日志增强功能
     */
    public static class CglibLoggingInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args,
                                MethodProxy proxy) throws Throwable {
            System.out.println("    [CGLIB 代理] 前置: 调用 " + method.getName()
                    + "(" + (args != null ? String.join(", ", toStringArray(args)) : "") + ")");
            long start = System.nanoTime();

            // 调用父类方法（即原始方法）
            Object result = proxy.invokeSuper(obj, args);

            long cost = (System.nanoTime() - start) / 1000;
            System.out.println("    [CGLIB 代理] 后置: " + method.getName()
                    + " 耗时 " + cost + "μs, 返回: " + result);
            return result;
        }

        /**
         * 创建 CGLIB 代理实例
         */
        @SuppressWarnings("unchecked")
        public static <T> T createProxy(Class<T> targetClass) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(targetClass);
            enhancer.setCallback(new CglibLoggingInterceptor());
            return (T) enhancer.create();
        }
    }

    // ==================== 模拟 Spring AOP 代理选择 ====================

    /**
     * 模拟 Spring AOP 的代理选择策略
     */
    @SuppressWarnings("unchecked")
    public static <T> T createSpringLikeProxy(T target, boolean proxyTargetClass) {
        Class<?> targetClass = target.getClass();
        Class<?>[] interfaces = targetClass.getInterfaces();

        if (!proxyTargetClass && interfaces.length > 0) {
            // 有接口且未强制 CGLIB → 使用 JDK 动态代理
            System.out.println("  [Spring] 选择 JDK 动态代理（目标类实现了接口）");
            return JdkLoggingHandler.createProxy(target, interfaces);
        } else {
            // 无接口或强制 CGLIB → 使用 CGLIB 代理
            System.out.println("  [Spring] 选择 CGLIB 代理"
                    + (interfaces.length == 0 ? "（目标类未实现接口）" : "（proxyTargetClass=true）"));
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(targetClass);
            enhancer.setCallback(new CglibLoggingInterceptor());
            return (T) enhancer.create();
        }
    }

    // ==================== main ====================

    public static void main(String[] args) {
        System.out.println("========== 动态代理演示 ==========\n");

        demonstrateJdkProxy();
        System.out.println();

        demonstrateCglibProxy();
        System.out.println();

        demonstratePerformanceComparison();
        System.out.println();

        demonstrateSpringAopSelection();
    }

    private static void demonstrateJdkProxy() {
        System.out.println("--- 1. JDK 动态代理 ---");
        System.out.println("特点：基于接口，只能代理实现了接口的类\n");

        UserService proxy = JdkLoggingHandler.createProxy(
                new UserServiceImpl(), UserService.class);

        System.out.println("  代理类: " + proxy.getClass().getName());
        System.out.println("  是否是 UserService: " + (proxy instanceof UserService));
        System.out.println();

        String result = proxy.findUser("张三");
        System.out.println("  最终结果: " + result);
    }

    private static void demonstrateCglibProxy() {
        System.out.println("--- 2. CGLIB 代理 ---");
        System.out.println("特点：基于继承，可以代理没有实现接口的类\n");

        OrderService proxy = CglibLoggingInterceptor.createProxy(OrderService.class);

        System.out.println("  代理类: " + proxy.getClass().getName());
        System.out.println("  是否是 OrderService: " + (proxy instanceof OrderService));
        System.out.println();

        String result = proxy.createOrder("iPhone 15");
        System.out.println("  最终结果: " + result);
    }

    private static void demonstratePerformanceComparison() {
        System.out.println("--- 3. 性能对比 ---");

        int warmup = 10000;
        int iterations = 100000;

        // JDK 代理
        UserService jdkProxy = JdkLoggingHandler.createProxy(
                new UserServiceImpl(), UserService.class);
        // CGLIB 代理
        UserServiceImpl cglibProxy = CglibLoggingInterceptor.createProxy(UserServiceImpl.class);

        // 预热
        for (int i = 0; i < warmup; i++) {
            jdkProxy.findUser("test");
            cglibProxy.findUser("test");
        }

        // JDK 代理性能
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jdkProxy.findUser("test");
        }
        long jdkTime = (System.nanoTime() - start) / 1_000_000;

        // CGLIB 代理性能
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            cglibProxy.findUser("test");
        }
        long cglibTime = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  " + iterations + " 次方法调用：");
        System.out.println("  JDK 代理耗时:   " + jdkTime + "ms");
        System.out.println("  CGLIB 代理耗时: " + cglibTime + "ms");
    }

    private static void demonstrateSpringAopSelection() {
        System.out.println("--- 4. 模拟 Spring AOP 代理选择 ---\n");

        // 场景 1：有接口，proxyTargetClass=false → JDK 代理
        System.out.println("场景 1：UserServiceImpl（有接口）+ proxyTargetClass=false");
        UserService proxy1 = createSpringLikeProxy(new UserServiceImpl(), false);
        proxy1.findUser("测试1");

        System.out.println();

        // 场景 2：有接口，proxyTargetClass=true → CGLIB 代理
        System.out.println("场景 2：UserServiceImpl（有接口）+ proxyTargetClass=true");
        UserServiceImpl proxy2 = createSpringLikeProxy(new UserServiceImpl(), true);
        proxy2.findUser("测试2");

        System.out.println();

        // 场景 3：无接口 → CGLIB 代理
        System.out.println("场景 3：OrderService（无接口）");
        OrderService proxy3 = createSpringLikeProxy(new OrderService(), false);
        proxy3.createOrder("测试商品");
    }

    private static String[] toStringArray(Object[] args) {
        String[] result = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = String.valueOf(args[i]);
        }
        return result;
    }
}

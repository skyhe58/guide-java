package com.example.advanced.proxy;

import com.example.advanced.proxy.DynamicProxyDemo.*;
import net.sf.cglib.proxy.Enhancer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态代理功能验证测试
 */
class DynamicProxyTest {

    // ==================== JDK 动态代理测试 ====================

    @Test
    @DisplayName("JDK 动态代理应正确代理接口方法")
    void jdkProxyShouldDelegateToTarget() {
        UserService proxy = JdkLoggingHandler.createProxy(
                new UserServiceImpl(), UserService.class);

        String result = proxy.findUser("张三");

        assertEquals("User: 张三", result);
    }

    @Test
    @DisplayName("JDK 动态代理对象应实现目标接口")
    void jdkProxyShouldImplementInterface() {
        UserService proxy = JdkLoggingHandler.createProxy(
                new UserServiceImpl(), UserService.class);

        assertTrue(proxy instanceof UserService);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
    }

    @Test
    @DisplayName("JDK 动态代理应正确处理无返回值方法")
    void jdkProxyShouldHandleVoidMethod() {
        UserService proxy = JdkLoggingHandler.createProxy(
                new UserServiceImpl(), UserService.class);

        // 不应抛出异常
        assertDoesNotThrow(() -> proxy.saveUser("李四"));
    }

    // ==================== CGLIB 代理测试 ====================

    @Test
    @DisplayName("CGLIB 代理应正确代理类方法")
    void cglibProxyShouldDelegateToTarget() {
        OrderService proxy = CglibLoggingInterceptor.createProxy(OrderService.class);

        String result = proxy.createOrder("iPhone 15");

        assertEquals("Order for: iPhone 15", result);
    }

    @Test
    @DisplayName("CGLIB 代理对象应是目标类的子类")
    void cglibProxyShouldExtendTargetClass() {
        OrderService proxy = CglibLoggingInterceptor.createProxy(OrderService.class);

        assertTrue(proxy instanceof OrderService);
        assertNotEquals(OrderService.class, proxy.getClass());
        assertTrue(proxy.getClass().getName().contains("CGLIB"));
    }

    @Test
    @DisplayName("CGLIB 代理也能代理实现了接口的类")
    void cglibProxyShouldWorkWithInterfaceImpl() {
        UserServiceImpl proxy = CglibLoggingInterceptor.createProxy(UserServiceImpl.class);

        String result = proxy.findUser("王五");

        assertEquals("User: 王五", result);
        assertTrue(proxy instanceof UserService);
    }

    // ==================== Spring AOP 模拟测试 ====================

    @Test
    @DisplayName("有接口且 proxyTargetClass=false 时应使用 JDK 代理")
    void shouldUseJdkProxyWhenInterfaceExists() {
        UserService proxy = DynamicProxyDemo.createSpringLikeProxy(
                new UserServiceImpl(), false);

        assertTrue(Proxy.isProxyClass(proxy.getClass()),
                "应使用 JDK 动态代理");
        assertEquals("User: test", proxy.findUser("test"));
    }

    @Test
    @DisplayName("无接口时应使用 CGLIB 代理")
    void shouldUseCglibProxyWhenNoInterface() {
        OrderService proxy = DynamicProxyDemo.createSpringLikeProxy(
                new OrderService(), false);

        assertTrue(proxy.getClass().getName().contains("CGLIB"),
                "应使用 CGLIB 代理");
        assertEquals("Order for: test", proxy.createOrder("test"));
    }

    @Test
    @DisplayName("proxyTargetClass=true 时应强制使用 CGLIB 代理")
    void shouldForceCglibWhenProxyTargetClassTrue() {
        UserServiceImpl proxy = DynamicProxyDemo.createSpringLikeProxy(
                new UserServiceImpl(), true);

        assertTrue(proxy.getClass().getName().contains("CGLIB"),
                "proxyTargetClass=true 应强制使用 CGLIB");
        assertEquals("User: test", proxy.findUser("test"));
    }
}

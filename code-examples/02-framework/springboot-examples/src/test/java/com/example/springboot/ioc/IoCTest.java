package com.example.springboot.ioc;

import com.example.springboot.SpringBootApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IoC 容器与 Bean 生命周期测试
 *
 * <p>验证以下知识点：</p>
 * <ul>
 *   <li>Bean 是否正确注册到容器</li>
 *   <li>构造器注入是否正常工作</li>
 *   <li>Bean 生命周期回调是否执行</li>
 *   <li>BeanPostProcessor 是否生效</li>
 * </ul>
 */
@SpringBootTest(classes = SpringBootApp.class)
class IoCTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IoCDemo.UserService userService;

    @Autowired
    private IoCDemo.LifecycleBean lifecycleBean;

    @Test
    @DisplayName("Bean 应该正确注册到 Spring 容器")
    void beanShouldBeRegistered() {
        assertNotNull(applicationContext.getBean(IoCDemo.LifecycleBean.class));
        assertNotNull(applicationContext.getBean(IoCDemo.UserService.class));
        assertNotNull(applicationContext.getBean(IoCDemo.SimpleUserRepository.class));
    }

    @Test
    @DisplayName("构造器注入应该正常工作")
    void constructorInjectionShouldWork() {
        // UserService 通过构造器注入了 UserRepository
        String result = userService.getUser(1L);
        assertEquals("User-1", result);
    }

    @Test
    @DisplayName("BeanNameAware 应该设置 Bean 名称")
    void beanNameAwareShouldSetName() {
        // LifecycleBean 实现了 BeanNameAware，应该获取到 Bean 名称
        assertEquals("lifecycleBean", lifecycleBean.getBeanName());
    }

    @Test
    @DisplayName("容器中应该包含 BeanPostProcessor")
    void beanPostProcessorShouldBeRegistered() {
        assertNotNull(applicationContext.getBean(IoCDemo.CustomBeanPostProcessor.class));
    }

    @Test
    @DisplayName("单例 Bean 每次获取应该是同一个实例")
    void singletonBeanShouldBeSameInstance() {
        var bean1 = applicationContext.getBean(IoCDemo.LifecycleBean.class);
        var bean2 = applicationContext.getBean(IoCDemo.LifecycleBean.class);
        assertSame(bean1, bean2, "单例 Bean 应该是同一个实例");
    }
}

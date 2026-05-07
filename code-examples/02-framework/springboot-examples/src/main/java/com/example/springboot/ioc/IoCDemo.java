package com.example.springboot.ioc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Spring IoC 容器与依赖注入演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>Bean 生命周期的完整回调顺序</li>
 *   <li>BeanPostProcessor 扩展机制</li>
 *   <li>构造器注入（推荐方式）</li>
 *   <li>Aware 接口回调</li>
 * </ul>
 */
public class IoCDemo {

    // ==================== 1. Bean 生命周期演示 ====================

    /**
     * 完整的 Bean 生命周期演示类
     *
     * <p>回调顺序：</p>
     * <ol>
     *   <li>构造方法（实例化）</li>
     *   <li>BeanNameAware#setBeanName</li>
     *   <li>ApplicationContextAware#setApplicationContext</li>
     *   <li>BeanPostProcessor#postProcessBeforeInitialization</li>
     *   <li>@PostConstruct</li>
     *   <li>InitializingBean#afterPropertiesSet</li>
     *   <li>BeanPostProcessor#postProcessAfterInitialization</li>
     *   <li>Bean 就绪</li>
     *   <li>@PreDestroy</li>
     *   <li>DisposableBean#destroy</li>
     * </ol>
     */
    @Component("lifecycleBean")
    public static class LifecycleBean implements BeanNameAware, ApplicationContextAware,
            InitializingBean, DisposableBean {

        private static final Logger log = LoggerFactory.getLogger(LifecycleBean.class);

        private String beanName;

        // 1. 构造方法 — 实例化阶段
        public LifecycleBean() {
            log.info("【1. 构造方法】Bean 实例化");
        }

        // 3. BeanNameAware 回调
        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            log.info("【3. BeanNameAware】setBeanName: {}", name);
        }

        // 3. ApplicationContextAware 回调
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            log.info("【3. ApplicationContextAware】获取到 ApplicationContext");
        }

        // 5. @PostConstruct — 在 BeanPostProcessor#before 中触发
        @PostConstruct
        public void postConstruct() {
            log.info("【5. @PostConstruct】初始化回调");
        }

        // 6. InitializingBean#afterPropertiesSet
        @Override
        public void afterPropertiesSet() {
            log.info("【6. InitializingBean】afterPropertiesSet");
        }

        // 9. @PreDestroy — 容器关闭时
        @PreDestroy
        public void preDestroy() {
            log.info("【9. @PreDestroy】销毁前回调");
        }

        // 10. DisposableBean#destroy
        @Override
        public void destroy() {
            log.info("【10. DisposableBean】destroy");
        }

        public String getBeanName() {
            return beanName;
        }
    }

    // ==================== 2. BeanPostProcessor 示例 ====================

    /**
     * 自定义 BeanPostProcessor
     *
     * <p>BeanPostProcessor 会对容器中所有 Bean 生效，
     * 这里只对 lifecycleBean 打印日志以减少输出。</p>
     */
    @Component
    public static class CustomBeanPostProcessor implements BeanPostProcessor {

        private static final Logger log = LoggerFactory.getLogger(CustomBeanPostProcessor.class);

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if ("lifecycleBean".equals(beanName)) {
                log.info("【4. BeanPostProcessor】postProcessBeforeInitialization: {}", beanName);
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if ("lifecycleBean".equals(beanName)) {
                log.info("【7. BeanPostProcessor】postProcessAfterInitialization: {}", beanName);
            }
            return bean;
        }
    }

    // ==================== 3. 构造器注入示例（推荐） ====================

    /**
     * 用户仓库接口
     */
    public interface UserRepository {
        String findById(Long id);
    }

    /**
     * 用户仓库实现
     */
    @Component
    public static class SimpleUserRepository implements UserRepository {
        @Override
        public String findById(Long id) {
            return "User-" + id;
        }
    }

    /**
     * 构造器注入示例 — Spring 官方推荐的注入方式
     *
     * <p>优点：</p>
     * <ul>
     *   <li>依赖不可变（final 字段）</li>
     *   <li>防止 NullPointerException</li>
     *   <li>利于单元测试</li>
     *   <li>可以发现循环依赖</li>
     * </ul>
     */
    @Service
    public static class UserService {

        private static final Logger log = LoggerFactory.getLogger(UserService.class);

        // 使用 final 字段 + 构造器注入
        private final UserRepository userRepository;

        // Spring 4.3+ 单构造器可省略 @Autowired
        public UserService(UserRepository userRepository) {
            this.userRepository = userRepository;
            log.info("UserService 构造器注入完成");
        }

        public String getUser(Long id) {
            return userRepository.findById(id);
        }
    }
}

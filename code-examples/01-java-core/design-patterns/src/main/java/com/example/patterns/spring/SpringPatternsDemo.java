package com.example.patterns.spring;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Spring 中的设计模式演示 — 工厂 / 代理 / 模板方法 / 观察者
 *
 * <p>本示例用纯 Java 模拟 Spring 框架中四种核心设计模式的实现：
 * <ul>
 *   <li>工厂模式：模拟 BeanFactory / ApplicationContext 的 Bean 创建与管理</li>
 *   <li>代理模式：模拟 Spring AOP 的 JDK 动态代理 + CGLIB 代理对比</li>
 *   <li>模板方法模式：模拟 JdbcTemplate 的模板回调机制</li>
 *   <li>观察者模式：模拟 ApplicationEvent / ApplicationListener 事件机制</li>
 * </ul>
 *
 * <h3>Spring 中的设计模式全景：</h3>
 * <pre>
 *  ┌─────────────────────────────────────────────────────────┐
 *  │                Spring 设计模式全景                        │
 *  ├──────────────┬──────────────────────────────────────────┤
 *  │ 工厂模式      │ BeanFactory, FactoryBean, AppContext    │
 *  │ 单例模式      │ DefaultSingletonBeanRegistry（三级缓存） │
 *  │ 代理模式      │ AOP（JDK 动态代理 / CGLIB）              │
 *  │ 模板方法      │ JdbcTemplate, RestTemplate, RedisTemplate│
 *  │ 观察者模式    │ ApplicationEvent, ApplicationListener    │
 *  │ 适配器模式    │ HandlerAdapter（MVC）                    │
 *  │ 策略模式      │ Resource（ClassPath/FileSystem/URL）     │
 *  │ 责任链模式    │ Filter, HandlerInterceptor               │
 *  └──────────────┴──────────────────────────────────────────┘
 * </pre>
 *
 * @author Spring 设计模式示例
 * @since 1.0
 */
public class SpringPatternsDemo {

    // ==================== 一、工厂模式 — 模拟 BeanFactory ====================

    /**
     * 模拟 Spring BeanFactory — IoC 容器核心。
     *
     * <pre>
     *  BeanFactory 体系：
     *  BeanFactory (顶层接口)
     *    ├── ListableBeanFactory (可列举所有 Bean)
     *    ├── HierarchicalBeanFactory (父子容器)
     *    └── AutowireCapableBeanFactory (自动装配)
     *          └── DefaultListableBeanFactory (默认实现)
     *
     *  Bean 生命周期：
     *  实例化 → 属性注入 → Aware回调 → BeanPostProcessor前置
     *  → InitializingBean → init-method → BeanPostProcessor后置 → 就绪
     * </pre>
     */
    static class SimpleBeanFactory {

        /** Bean 定义 */
        static class BeanDefinition {
            final String name;
            final Class<?> type;
            final Supplier<?> supplier;
            final boolean singleton;

            BeanDefinition(String name, Class<?> type, Supplier<?> supplier, boolean singleton) {
                this.name = name;
                this.type = type;
                this.supplier = supplier;
                this.singleton = singleton;
            }
        }

        // Bean 定义注册表
        private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
        // 单例缓存（一级缓存 singletonObjects）
        private final Map<String, Object> singletonCache = new ConcurrentHashMap<>();
        // BeanPostProcessor 列表（扩展点）
        private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

        /** BeanPostProcessor 接口 — Bean 初始化前后的扩展点 */
        interface BeanPostProcessor {
            default Object postProcessBeforeInit(Object bean, String beanName) { return bean; }
            default Object postProcessAfterInit(Object bean, String beanName) { return bean; }
        }

        void addBeanPostProcessor(BeanPostProcessor processor) {
            postProcessors.add(processor);
        }

        void registerBean(String name, Class<?> type, Supplier<?> supplier, boolean singleton) {
            definitions.put(name, new BeanDefinition(name, type, supplier, singleton));
        }

        @SuppressWarnings("unchecked")
        <T> T getBean(String name, Class<T> type) {
            BeanDefinition bd = definitions.get(name);
            if (bd == null) throw new RuntimeException("No bean: " + name);

            if (bd.singleton) {
                return (T) singletonCache.computeIfAbsent(name, k -> createBean(bd));
            }
            return (T) createBean(bd);
        }

        @SuppressWarnings("unchecked")
        <T> T getBean(Class<T> type) {
            for (BeanDefinition bd : definitions.values()) {
                if (type.isAssignableFrom(bd.type)) return getBean(bd.name, type);
            }
            throw new RuntimeException("No bean of type: " + type.getName());
        }

        private Object createBean(BeanDefinition bd) {
            // 实例化
            Object bean = bd.supplier.get();
            // BeanPostProcessor 前置处理
            for (BeanPostProcessor pp : postProcessors) {
                bean = pp.postProcessBeforeInit(bean, bd.name);
            }
            // BeanPostProcessor 后置处理（AOP 代理在此创建）
            for (BeanPostProcessor pp : postProcessors) {
                bean = pp.postProcessAfterInit(bean, bd.name);
            }
            return bean;
        }

        String[] getBeanNames() { return definitions.keySet().toArray(new String[0]); }
        int getBeanCount() { return definitions.size(); }
    }

    // 业务接口与实现
    interface OrderService {
        String createOrder(String product, int quantity);
    }

    static class OrderServiceImpl implements OrderService {
        @Override
        public String createOrder(String product, int quantity) {
            return String.format("Order{product='%s', qty=%d, id=%d}",
                    product, quantity, System.nanoTime() % 10000);
        }
    }

    /** 演示1：工厂模式 — BeanFactory + BeanPostProcessor */
    static void demoFactoryPattern() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：工厂模式 — 模拟 BeanFactory");
        System.out.println("═══════════════════════════════════════════════════");

        SimpleBeanFactory factory = new SimpleBeanFactory();

        // 注册 BeanPostProcessor（模拟 AOP 自动代理）
        factory.addBeanPostProcessor(new SimpleBeanFactory.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInit(Object bean, String beanName) {
                System.out.printf("    [BeanPostProcessor] 后置处理: %s%n", beanName);
                return bean; // 实际 AOP 会在这里返回代理对象
            }
        });

        // 注册 Bean 定义
        factory.registerBean("orderService", OrderServiceImpl.class,
                OrderServiceImpl::new, true);

        System.out.printf("\n    已注册 %d 个 Bean: %s%n",
                factory.getBeanCount(), Arrays.toString(factory.getBeanNames()));

        // 获取 Bean 并使用
        OrderService service = factory.getBean("orderService", OrderService.class);
        System.out.printf("    创建订单: %s%n", service.createOrder("iPhone", 1));

        // 验证单例
        OrderService service2 = factory.getBean(OrderService.class);
        System.out.printf("    单例验证: service == service2 → %s%n", service == service2);

        // 原型模式对比
        factory.registerBean("prototypeService", OrderServiceImpl.class,
                OrderServiceImpl::new, false);
        OrderService p1 = factory.getBean("prototypeService", OrderService.class);
        OrderService p2 = factory.getBean("prototypeService", OrderService.class);
        System.out.printf("    原型验证: p1 == p2 → %s（每次创建新实例）%n", p1 == p2);
        System.out.println();
    }

    // ==================== 二、代理模式 — 模拟 Spring AOP ====================

    /** 方法拦截器（模拟 MethodInterceptor） */
    interface MethodAdvice {
        Object invoke(Object proxy, Method method, Object[] args, MethodInvoker next) throws Throwable;
    }

    /** 原始方法调用器 */
    interface MethodInvoker {
        Object invoke() throws Throwable;
    }

    /**
     * AOP 代理工厂 — 模拟 JDK 动态代理和 CGLIB 代理的选择逻辑。
     *
     * <pre>
     *  Spring AOP 代理选择：
     *  ┌──────────────────┐
     *  │ 目标对象实现接口？ │
     *  └────────┬─────────┘
     *       是  │  否
     *       ↓      ↓
     *  ┌─────────┐ ┌──────────┐
     *  │JDK 代理 │ │CGLIB 代理 │
     *  │基于接口  │ │基于子类   │
     *  └─────────┘ └──────────┘
     *
     *  拦截器链（洋葱模型）：
     *  请求 → Advice1 → Advice2 → 目标方法 → Advice2 → Advice1 → 响应
     * </pre>
     */
    static class AopProxyFactory {
        @SuppressWarnings("unchecked")
        static <T> T createJdkProxy(T target, Class<?>[] interfaces, List<MethodAdvice> advices) {
            return (T) Proxy.newProxyInstance(
                    target.getClass().getClassLoader(), interfaces,
                    (proxy, method, args) -> {
                        // 构建拦截器链（从后往前包装，形成洋葱模型）
                        MethodInvoker chain = () -> method.invoke(target, args);
                        for (int i = advices.size() - 1; i >= 0; i--) {
                            MethodAdvice advice = advices.get(i);
                            MethodInvoker next = chain;
                            chain = () -> advice.invoke(proxy, method, args, next);
                        }
                        return chain.invoke();
                    });
        }
    }

    /** 日志切面 */
    static class LoggingAdvice implements MethodAdvice {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args, MethodInvoker next) throws Throwable {
            long start = System.nanoTime();
            System.out.printf("      [AOP-Log] → %s(%s)%n",
                    method.getName(), Arrays.toString(args));
            Object result = next.invoke();
            System.out.printf("      [AOP-Log] ← %s = %s (%dμs)%n",
                    method.getName(), result, (System.nanoTime() - start) / 1000);
            return result;
        }
    }

    /** 事务切面 */
    static class TransactionAdvice implements MethodAdvice {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args, MethodInvoker next) throws Throwable {
            System.out.println("      [AOP-Tx] 开启事务");
            try {
                Object result = next.invoke();
                System.out.println("      [AOP-Tx] 提交事务");
                return result;
            } catch (Throwable t) {
                System.out.printf("      [AOP-Tx] 回滚事务: %s%n", t.getMessage());
                throw t;
            }
        }
    }

    /** 演示2：代理模式 — JDK 动态代理 + CGLIB 对比 */
    static void demoProxyPattern() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：代理模式 — 模拟 Spring AOP");
        System.out.println("═══════════════════════════════════════════════════");

        OrderService target = new OrderServiceImpl();
        List<MethodAdvice> advices = Arrays.asList(new LoggingAdvice(), new TransactionAdvice());

        // JDK 动态代理
        System.out.println("\n  【JDK 动态代理 — 基于接口】");
        OrderService proxy = AopProxyFactory.createJdkProxy(
                target, new Class[]{OrderService.class}, advices);
        System.out.println("    调用代理方法：");
        String result = proxy.createOrder("MacBook", 2);
        System.out.printf("    结果: %s%n", result);

        System.out.printf("\n    目标类: %s%n", target.getClass().getSimpleName());
        System.out.printf("    代理类: %s%n", proxy.getClass().getName());
        System.out.printf("    是 Proxy: %s%n", Proxy.isProxyClass(proxy.getClass()));

        // CGLIB 代理说明（纯 JDK 无法直接演示 CGLIB，用说明替代）
        System.out.println("\n  【JDK 代理 vs CGLIB 代理对比】");
        String[][] comparison = {
            {"特性", "JDK 动态代理", "CGLIB 代理"},
            {"原理", "基于接口(Proxy)", "基于子类(ASM字节码)"},
            {"要求", "目标类必须实现接口", "目标类不能是 final"},
            {"性能", "JDK8+性能优秀", "创建慢，调用快"},
            {"Spring默认", "有接口时使用", "无接口时使用"},
        };
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("    %-8s %-20s %s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2]);
            if (i == 0) System.out.println("    " + "─".repeat(50));
        }
        System.out.println();
    }

    // ==================== 三、模板方法模式 — 模拟 JdbcTemplate ====================

    /** 模拟数据库行 */
    static class MockRow {
        private final Map<String, Object> data;
        MockRow(Map<String, Object> data) { this.data = data; }
        @SuppressWarnings("unchecked")
        <T> T get(String col, Class<T> type) { return (T) data.get(col); }
    }

    /** RowMapper 接口 */
    interface RowMapper<T> {
        T mapRow(MockRow row, int rowNum);
    }

    /**
     * 模拟 JdbcTemplate — 模板方法模式的经典应用。
     *
     * <pre>
     *  模板方法骨架（不变的部分）：
     *  ┌──────────────┐
     *  │ 获取连接       │ ← 模板固定步骤
     *  ├──────────────┤
     *  │ 创建 Statement │ ← 模板固定步骤
     *  ├──────────────┤
     *  │ 执行 SQL      │ ← 模板固定步骤
     *  ├──────────────┤
     *  │ 结果映射       │ ← 用户自定义（RowMapper 回调）
     *  ├──────────────┤
     *  │ 关闭资源       │ ← 模板固定步骤（finally）
     *  └──────────────┘
     * </pre>
     */
    static class SimpleJdbcTemplate {
        private final List<Map<String, Object>> database = new ArrayList<>();
        private int queryCount = 0;

        SimpleJdbcTemplate() {
            database.add(Map.of("id", 1, "name", "Alice", "salary", 8000.0));
            database.add(Map.of("id", 2, "name", "Bob", "salary", 12000.0));
            database.add(Map.of("id", 3, "name", "Charlie", "salary", 15000.0));
        }

        /** 模板方法：查询列表 */
        <T> List<T> query(String sql, RowMapper<T> mapper) {
            queryCount++;
            openConnection();
            try {
                List<Map<String, Object>> rs = executeSQL(sql);
                List<T> results = new ArrayList<>();
                for (int i = 0; i < rs.size(); i++) {
                    results.add(mapper.mapRow(new MockRow(rs.get(i)), i));
                }
                return results;
            } finally {
                closeConnection();
            }
        }

        /** 模板方法：查询单个 */
        <T> T queryForObject(String sql, RowMapper<T> mapper, Object... params) {
            queryCount++;
            openConnection();
            try {
                List<Map<String, Object>> rs = executeSQL(sql);
                if (params.length > 0 && params[0] instanceof Integer) {
                    int targetId = (Integer) params[0];
                    for (Map<String, Object> row : rs) {
                        if (row.get("id").equals(targetId)) {
                            return mapper.mapRow(new MockRow(row), 0);
                        }
                    }
                }
                return rs.isEmpty() ? null : mapper.mapRow(new MockRow(rs.get(0)), 0);
            } finally {
                closeConnection();
            }
        }

        int getQueryCount() { return queryCount; }
        private void openConnection() { /* 模拟获取连接 */ }
        private void closeConnection() { /* 模拟释放连接 */ }
        private List<Map<String, Object>> executeSQL(String sql) { return new ArrayList<>(database); }
    }

    /** 演示3：模板方法模式 — JdbcTemplate */
    static void demoTemplateMethod() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：模板方法模式 — 模拟 JdbcTemplate");
        System.out.println("═══════════════════════════════════════════════════");

        SimpleJdbcTemplate jdbc = new SimpleJdbcTemplate();

        // 查询列表 — 用户只需提供 RowMapper
        System.out.println("\n  【查询所有员工】");
        List<String> employees = jdbc.query("SELECT * FROM employee",
                (row, idx) -> String.format("Employee{id=%d, name=%s, salary=%.0f}",
                        row.get("id", Integer.class),
                        row.get("name", String.class),
                        row.get("salary", Double.class)));
        employees.forEach(e -> System.out.printf("    %s%n", e));

        // 查询单个对象
        System.out.println("\n  【查询单个员工（id=2）】");
        String emp = jdbc.queryForObject("SELECT * FROM employee WHERE id=?",
                (row, idx) -> row.get("name", String.class) + " (salary="
                        + row.get("salary", Double.class) + ")", 2);
        System.out.printf("    结果: %s%n", emp);

        // 不同的 RowMapper 复用同一模板
        System.out.println("\n  【不同 RowMapper 复用模板】");
        List<Double> salaries = jdbc.query("SELECT salary FROM employee",
                (row, idx) -> row.get("salary", Double.class));
        double avgSalary = salaries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        System.out.printf("    薪资列表: %s%n", salaries);
        System.out.printf("    平均薪资: %.0f%n", avgSalary);
        System.out.printf("    总查询次数: %d（连接获取/释放由模板管理）%n", jdbc.getQueryCount());
        System.out.println();
    }

    // ==================== 四、观察者模式 — 模拟 ApplicationEvent ====================

    /** 事件基类 */
    static abstract class AppEvent {
        private final Object source;
        private final long timestamp = System.currentTimeMillis();
        AppEvent(Object source) { this.source = source; }
        Object getSource() { return source; }
        long getTimestamp() { return timestamp; }
    }

    /** 用户注册事件 */
    static class UserRegisteredEvent extends AppEvent {
        final String username;
        final String email;
        UserRegisteredEvent(Object source, String username, String email) {
            super(source);
            this.username = username;
            this.email = email;
        }
    }

    /** 订单创建事件 */
    static class OrderCreatedEvent extends AppEvent {
        final String orderId;
        final double amount;
        OrderCreatedEvent(Object source, String orderId, double amount) {
            super(source);
            this.orderId = orderId;
            this.amount = amount;
        }
    }

    /** 事件监听器接口 */
    interface EventListener<E extends AppEvent> {
        void onEvent(E event);
        Class<E> getEventType();
    }

    /**
     * 事件广播器 — 模拟 SimpleApplicationEventMulticaster。
     *
     * <pre>
     *  事件发布流程：
     *  Publisher ──publish──→ Multicaster ──dispatch──→ Listener1
     *                                    ──dispatch──→ Listener2
     *                                    ──dispatch──→ Listener3
     *
     *  Spring 支持同步/异步事件：
     *  同步: 默认，在发布线程中依次调用监听器
     *  异步: @Async + @EventListener，使用线程池异步调用
     * </pre>
     */
    static class EventMulticaster {
        private final List<EventListener<?>> listeners = new ArrayList<>();
        private int publishCount = 0;

        void addListener(EventListener<?> listener) { listeners.add(listener); }

        @SuppressWarnings("unchecked")
        void publishEvent(AppEvent event) {
            publishCount++;
            int matched = 0;
            for (EventListener<?> listener : listeners) {
                if (listener.getEventType().isAssignableFrom(event.getClass())) {
                    ((EventListener<AppEvent>) listener).onEvent(event);
                    matched++;
                }
            }
            System.out.printf("      → 事件 %s 分发给 %d 个监听器%n",
                    event.getClass().getSimpleName(), matched);
        }

        int getPublishCount() { return publishCount; }
    }

    /** 演示4：观察者模式 — ApplicationEvent */
    static void demoObserverPattern() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：观察者模式 — 模拟 Spring 事件机制");
        System.out.println("═══════════════════════════════════════════════════");

        EventMulticaster multicaster = new EventMulticaster();

        // 注册监听器
        multicaster.addListener(new EventListener<UserRegisteredEvent>() {
            @Override
            public void onEvent(UserRegisteredEvent e) {
                System.out.printf("      [邮件服务] 发送欢迎邮件给 %s (%s)%n", e.username, e.email);
            }
            @Override
            public Class<UserRegisteredEvent> getEventType() { return UserRegisteredEvent.class; }
        });

        multicaster.addListener(new EventListener<UserRegisteredEvent>() {
            @Override
            public void onEvent(UserRegisteredEvent e) {
                System.out.printf("      [积分服务] 为 %s 初始化 100 积分%n", e.username);
            }
            @Override
            public Class<UserRegisteredEvent> getEventType() { return UserRegisteredEvent.class; }
        });

        multicaster.addListener(new EventListener<OrderCreatedEvent>() {
            @Override
            public void onEvent(OrderCreatedEvent e) {
                System.out.printf("      [通知服务] 订单 %s 已创建，金额 %.2f%n", e.orderId, e.amount);
            }
            @Override
            public Class<OrderCreatedEvent> getEventType() { return OrderCreatedEvent.class; }
        });

        multicaster.addListener(new EventListener<OrderCreatedEvent>() {
            @Override
            public void onEvent(OrderCreatedEvent e) {
                System.out.printf("      [库存服务] 订单 %s 扣减库存%n", e.orderId);
            }
            @Override
            public Class<OrderCreatedEvent> getEventType() { return OrderCreatedEvent.class; }
        });

        // 发布事件
        System.out.println("\n    发布 UserRegisteredEvent：");
        multicaster.publishEvent(new UserRegisteredEvent("demo", "张三", "zhangsan@example.com"));

        System.out.println("\n    发布 OrderCreatedEvent：");
        multicaster.publishEvent(new OrderCreatedEvent("demo", "ORD-20240101-001", 299.99));

        System.out.printf("\n    总发布事件数: %d%n", multicaster.getPublishCount());
        System.out.println("    监听器数量: 4（2 个用户事件 + 2 个订单事件）");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Spring 设计模式演示 — 工厂+代理+模板方法+观察者       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoFactoryPattern();
        demoProxyPattern();
        demoTemplateMethod();
        demoObserverPattern();
    }
}

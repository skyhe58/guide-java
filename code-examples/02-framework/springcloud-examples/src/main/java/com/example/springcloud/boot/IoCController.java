package com.example.springcloud.boot;

import com.example.springcloud.boot.service.GreetingService;
import com.example.springcloud.boot.service.PrototypeDemoService;
import com.example.springcloud.common.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 10.A.7 Spring IoC/DI 核心概念演示 Controller
 *
 * <p>演示 Spring 依赖注入的多种方式：
 * <ul>
 *   <li>@Autowired 按类型注入（配合 @Primary）</li>
 *   <li>@Qualifier 按名称注入</li>
 *   <li>构造器注入（Spring 官方推荐）</li>
 *   <li>@Value 属性注入</li>
 *   <li>Bean 作用域（Singleton vs Prototype）</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 查看各种注入方式的结果
 * curl http://localhost:8090/demo/boot/ioc/inject-info
 *
 * # 列出容器中的 Bean（按前缀过滤）
 * curl http://localhost:8090/demo/boot/ioc/beans?prefix=greeting
 *
 * # 演示 Bean 作用域
 * curl http://localhost:8090/demo/boot/ioc/scope
 * </pre>
 */
@RestController
@RequestMapping("/demo/boot/ioc")
public class IoCController {

    // ===== 1. @Autowired 按类型注入（@Primary 优先） =====
    @Autowired
    private GreetingService primaryGreeting;

    // ===== 2. @Qualifier 按名称注入 =====
    @Autowired
    @Qualifier("englishGreeting")
    private GreetingService englishGreeting;

    // ===== 3. @Value 属性注入 =====
    @Value("${spring.application.name:springcloud-demo}")
    private String appName;

    @Value("${server.port:8080}")
    private int serverPort;

    // ===== 4. 构造器注入（推荐方式） =====
    private final ApplicationContext applicationContext;
    private final ObjectProvider<PrototypeDemoService> prototypeProvider;

    /**
     * 构造器注入 — Spring 官方推荐的注入方式
     * <p>优点：依赖不可变（final）、不会出现 NPE、方便单元测试
     */
    public IoCController(ApplicationContext applicationContext,
                         ObjectProvider<PrototypeDemoService> prototypeProvider) {
        this.applicationContext = applicationContext;
        this.prototypeProvider = prototypeProvider;
    }

    /**
     * 展示各种注入方式的结果
     */
    @GetMapping("/inject-info")
    public Result<Map<String, Object>> injectInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // @Autowired + @Primary
        info.put("1_autowired_primary", Map.of(
                "说明", "@Autowired 按类型注入，@Primary 标记的 ChineseGreetingService 被优先选中",
                "实现类", primaryGreeting.getImplName(),
                "结果", primaryGreeting.greet("Spring")
        ));

        // @Qualifier 指定名称
        info.put("2_qualifier", Map.of(
                "说明", "@Qualifier(\"englishGreeting\") 按 Bean 名称注入",
                "实现类", englishGreeting.getImplName(),
                "结果", englishGreeting.greet("Spring")
        ));

        // @Value 属性注入
        info.put("3_value", Map.of(
                "说明", "@Value 从配置文件注入属性值",
                "appName", appName,
                "serverPort", serverPort
        ));

        // 构造器注入
        info.put("4_constructor_inject", Map.of(
                "说明", "构造器注入 ApplicationContext（推荐方式，依赖不可变）",
                "applicationContext类型", applicationContext.getClass().getSimpleName(),
                "容器启动时间", new Date(applicationContext.getStartupDate()).toString()
        ));

        return Result.ok(info);
    }

    /**
     * 列出容器中的 Bean（按前缀过滤）
     *
     * @param prefix Bean 名称前缀（不区分大小写）
     */
    @GetMapping("/beans")
    public Result<Map<String, Object>> listBeans(
            @RequestParam(defaultValue = "greeting") String prefix) {
        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        List<String> matched = Arrays.stream(allBeanNames)
                .filter(name -> name.toLowerCase().contains(prefix.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("过滤条件", prefix);
        result.put("匹配数量", matched.size());
        result.put("总Bean数", allBeanNames.length);

        List<Map<String, String>> beanDetails = new ArrayList<>();
        for (String beanName : matched) {
            Object bean = applicationContext.getBean(beanName);
            beanDetails.add(Map.of(
                    "名称", beanName,
                    "类型", bean.getClass().getSimpleName(),
                    "全限定名", bean.getClass().getName()
            ));
        }
        result.put("Bean列表", beanDetails);

        return Result.ok(result);
    }

    /**
     * 演示 Bean 作用域（Singleton vs Prototype）
     */
    @GetMapping("/scope")
    public Result<Map<String, Object>> scope() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Singleton：每次获取都是同一个实例
        GreetingService s1 = applicationContext.getBean("chineseGreeting", GreetingService.class);
        GreetingService s2 = applicationContext.getBean("chineseGreeting", GreetingService.class);
        result.put("singleton", Map.of(
                "说明", "Singleton 作用域（默认）：整个容器只有一个实例",
                "实例1_hashCode", System.identityHashCode(s1),
                "实例2_hashCode", System.identityHashCode(s2),
                "是否同一实例", s1 == s2
        ));

        // Prototype：每次获取都是新实例
        PrototypeDemoService p1 = prototypeProvider.getObject();
        PrototypeDemoService p2 = prototypeProvider.getObject();
        result.put("prototype", Map.of(
                "说明", "Prototype 作用域：每次 getBean() 都创建新实例",
                "实例1_id", p1.getInstanceId(),
                "实例2_id", p2.getInstanceId(),
                "是否同一实例", p1 == p2
        ));

        return Result.ok(result);
    }
}

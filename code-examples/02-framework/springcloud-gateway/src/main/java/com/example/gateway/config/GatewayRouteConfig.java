package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 路由配置（Java 代码方式）
 *
 * 补充 application.yml 中的 YAML 路由配置，演示用代码定义路由规则。
 * 两种方式可以共存，代码方式优先级更高，适合动态路由场景。
 *
 * 路由断言（Predicate）：
 * - Path：按请求路径匹配
 * - Method：按 HTTP 方法匹配
 * - Header：按请求头匹配
 *
 * 过滤器（Filter）：
 * - StripPrefix：去掉路径前缀
 * - AddRequestHeader：添加请求头
 *
 * 测试命令：
 * {@code curl http://localhost:8080/code/demo/registry/services -H "Authorization: Bearer test-token"}
 * {@code curl -X POST http://localhost:8080/code/demo/registry/services -H "Authorization: Bearer test-token" -H "X-Api-Version: v2"}
 */
@Configuration
public class GatewayRouteConfig {

    /**
     * 通过 Java 代码定义路由规则
     *
     * @param builder RouteLocatorBuilder，Spring Cloud Gateway 提供的路由构建器
     * @return RouteLocator 路由定位器
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // 路由 1：按路径匹配 — 将 /code/** 转发到业务服务
                .route("code-route", r -> r
                        .path("/code/**")
                        .filters(f -> f
                                .stripPrefix(1)                              // 去掉 /code 前缀
                                .addRequestHeader("X-Gateway-Source", "java-config")  // 标记来源
                        )
                        .uri("lb://springcloud-demo")
                )

                // 路由 2：按方法 + 路径匹配 — 只允许 POST 请求访问写入接口
                .route("write-route", r -> r
                        .path("/write/**")
                        .and()
                        .method("POST", "PUT")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("X-Request-Type", "write")
                        )
                        .uri("lb://springcloud-demo")
                )

                // 路由 3：按请求头匹配 — 带有特定版本头的请求走独立路由
                .route("versioned-route", r -> r
                        .path("/api/v2/**")
                        .and()
                        .header("X-Api-Version", "v2")
                        .filters(f -> f
                                .stripPrefix(2)                              // 去掉 /api/v2 前缀
                                .addRequestHeader("X-Routed-By", "version-header")
                        )
                        .uri("lb://springcloud-demo")
                )

                .build();
    }
}

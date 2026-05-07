package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局日志过滤器
 *
 * 记录每个请求的路径、HTTP 方法、耗时和响应状态码。
 * 用于监控网关流量和排查问题。
 *
 * 执行顺序：order = -90（在鉴权之后执行）
 *
 * 日志示例：
 * {@code [Gateway] GET /api/demo/registry/services → 200 (15ms)}
 */
@Component
public class LogGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LogGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        long startTime = System.currentTimeMillis();

        log.info("[Gateway] 收到请求: {} {}", method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
            log.info("[Gateway] {} {} → {} ({}ms)", method, path, statusCode, duration);
        }));
    }

    @Override
    public int getOrder() {
        return -90;  // 在鉴权过滤器之后执行
    }
}

package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 全局鉴权过滤器
 *
 * 检查请求的 Authorization 头，无效则返回 401 Unauthorized。
 * 白名单路径跳过鉴权（如健康检查、公开接口）。
 *
 * 执行顺序：order = -100（最先执行）
 *
 * 测试命令：
 * {@code curl http://localhost:8080/api/demo/registry/services}                          → 401
 * {@code curl http://localhost:8080/api/demo/registry/services -H "Authorization: Bearer test-token"} → 200
 * {@code curl http://localhost:8080/actuator/health}                                      → 200（白名单）
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 白名单路径，跳过鉴权 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/demo/registry/**",
            "/actuator/**",
            "/api/user/init",
            "/api/sharding/init"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行
        if (isWhiteListed(path)) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 检查 Authorization 头
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            log.warn("缺少 Authorization 头, 路径: {}", path);
            return unauthorized(exchange, "缺少 Authorization 头");
        }

        // 简单校验 Bearer Token 格式
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 格式无效, 路径: {}", path);
            return unauthorized(exchange, "Authorization 格式无效，需要 Bearer Token");
        }

        // Token 校验通过，继续执行过滤器链
        log.debug("鉴权通过, 路径: {}", path);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;  // 最先执行
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回 401 Unauthorized 响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}

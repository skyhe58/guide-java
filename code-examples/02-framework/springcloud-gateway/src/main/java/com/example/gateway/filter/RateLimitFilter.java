package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 全局限流过滤器（基于 Redis 令牌桶）
 *
 * 使用 ReactiveStringRedisTemplate 实现简单的令牌桶限流。
 * 限流 key 基于请求路径，每个路径独立计数。
 * 超过限制返回 429 Too Many Requests。
 *
 * 令牌桶参数：
 * - 窗口时间：1 秒
 * - 最大请求数：100 次/秒/路径
 *
 * 执行顺序：order = -80（在日志过滤器之后执行）
 *
 * 注意：Gateway 是 WebFlux 架构，必须使用 ReactiveStringRedisTemplate（非阻塞）
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** 限流窗口时间 */
    private static final Duration WINDOW = Duration.ofSeconds(1);

    /** 每个窗口最大请求数 */
    private static final long MAX_REQUESTS = 100;

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "gateway:rate_limit:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String redisKey = KEY_PREFIX + path;

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // 第一次请求，设置过期时间
                        return redisTemplate.expire(redisKey, WINDOW)
                                .then(chain.filter(exchange));
                    }

                    if (count > MAX_REQUESTS) {
                        // 超过限流阈值
                        log.warn("[限流] 路径 {} 请求过于频繁，当前计数: {}", path, count);
                        return tooManyRequests(exchange);
                    }

                    // 未超过限制，放行
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -80;  // 在日志过滤器之后执行
    }

    /**
     * 返回 429 Too Many Requests 响应
     */
    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}

package com.example.springcloud.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置类
 *
 * <p>启用 Spring Cache 注解支持（@Cacheable / @CacheEvict / @CachePut），
 * 并配置 RedisCacheManager：
 * <ul>
 *   <li>默认 TTL：600 秒（10 分钟）</li>
 *   <li>Key 序列化：StringRedisSerializer</li>
 *   <li>Value 序列化：GenericJackson2JsonRedisSerializer（JSON 格式）</li>
 *   <li>不缓存 null 值</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置 RedisCacheManager
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisCacheManager 实例
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 默认过期时间 600 秒
                .entryTtl(Duration.ofSeconds(600))
                // Key 使用 String 序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Value 使用 JSON 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 不缓存 null 值
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}

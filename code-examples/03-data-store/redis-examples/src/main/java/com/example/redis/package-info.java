/**
 * Redis 知识代码示例
 *
 * <p>本模块包含 Redis 相关的代码示例，涵盖：</p>
 * <ul>
 *   <li>{@link com.example.redis.datastructure} — 五种基本数据类型使用场景与底层编码说明</li>
 *   <li>{@link com.example.redis.cache} — 缓存穿透/击穿/雪崩解决方案（布隆过滤器、互斥锁、逻辑过期）</li>
 *   <li>{@link com.example.redis.lock} — 分布式锁实现（SETNX + Lua 脚本、可重入锁）</li>
 *   <li>{@link com.example.redis.spring} — RedisTemplate 配置和 Spring Cache 集成</li>
 * </ul>
 *
 * <p>⚠️ 需要 Redis 环境的示例：{@code docker compose -f docker/docker-compose.yml up -d redis}</p>
 */
package com.example.redis;

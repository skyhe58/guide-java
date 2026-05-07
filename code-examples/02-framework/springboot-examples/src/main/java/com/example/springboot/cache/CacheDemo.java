package com.example.springboot.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存集成演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>@Cacheable — 查询缓存（命中直接返回，未命中执行方法并缓存）</li>
 *   <li>@CachePut — 更新缓存（始终执行方法，更新缓存）</li>
 *   <li>@CacheEvict — 删除缓存</li>
 *   <li>@CacheConfig — 类级别缓存配置</li>
 * </ul>
 *
 * <p>默认使用 ConcurrentMapCacheManager（内存缓存），
 * 生产环境建议替换为 Redis。</p>
 */
@Service
@CacheConfig(cacheNames = "products")
public class CacheDemo {

    private static final Logger log = LoggerFactory.getLogger(CacheDemo.class);

    // 模拟数据库
    private final Map<Long, Product> database = new ConcurrentHashMap<>();

    public CacheDemo() {
        // 初始化模拟数据
        database.put(1L, new Product(1L, "Java 编程思想", 99.0));
        database.put(2L, new Product(2L, "Spring 实战", 79.0));
        database.put(3L, new Product(3L, "深入理解 JVM", 89.0));
    }

    /**
     * @Cacheable — 先查缓存，命中直接返回；未命中执行方法并缓存结果
     *
     * <p>第一次调用会执行方法体（打印日志），后续调用直接返回缓存。</p>
     */
    @Cacheable(key = "#id")
    public Product findById(Long id) {
        log.info("【缓存未命中】从数据库查询商品: {}", id);
        simulateSlowQuery();
        return database.get(id);
    }

    /**
     * @CachePut — 始终执行方法，并将结果更新到缓存
     *
     * <p>与 @Cacheable 的区别：@CachePut 不管缓存是否存在都会执行方法。</p>
     */
    @CachePut(key = "#product.id")
    public Product update(Product product) {
        log.info("更新商品: {}", product);
        database.put(product.id(), product);
        return product;
    }

    /**
     * @CacheEvict — 删除指定缓存
     */
    @CacheEvict(key = "#id")
    public void delete(Long id) {
        log.info("删除商品: {}", id);
        database.remove(id);
    }

    /**
     * @CacheEvict(allEntries = true) — 清空所有缓存
     */
    @CacheEvict(allEntries = true)
    public void clearAll() {
        log.info("清空所有商品缓存");
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(100); // 模拟数据库查询耗时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 商品记录
     */
    public record Product(Long id, String name, Double price) {
    }
}

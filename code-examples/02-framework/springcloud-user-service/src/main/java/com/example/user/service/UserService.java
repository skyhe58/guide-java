package com.example.user.service;

import com.example.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务层
 *
 * 提供用户 CRUD 操作，集成 Redis 缓存。
 * 查询用户时先查 Redis，未命中再查 MySQL 并回填缓存。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Redis 缓存 key 前缀 */
    private static final String CACHE_PREFIX = "user:";

    /** 缓存过期时间 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 用户行映射器 */
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setAge(rs.getInt("age"));
        user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return user;
    };

    public UserService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 初始化 users 表和测试数据
     */
    public String initTable() {
        // 创建 users 表
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(50) NOT NULL, " +
                "email VARCHAR(100), " +
                "age INT, " +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")");
        log.info("users 表创建完成");

        // 插入测试数据（先清空避免重复）
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", "张三", "zhangsan@example.com", 28);
        jdbcTemplate.update("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", "李四", "lisi@example.com", 32);
        jdbcTemplate.update("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", "王五", "wangwu@example.com", 25);
        jdbcTemplate.update("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", "赵六", "zhaoliu@example.com", 30);
        jdbcTemplate.update("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", "张伟", "zhangwei@example.com", 27);
        log.info("测试数据插入完成，共 5 条");

        // 清除 Redis 缓存
        redisTemplate.delete(redisTemplate.keys(CACHE_PREFIX + "*"));
        log.info("Redis 用户缓存已清除");

        return "users 表初始化完成，插入 5 条测试数据";
    }

    /**
     * 根据 ID 查询用户（带 Redis 缓存）
     *
     * 查询流程：Redis 缓存 → MySQL → 回填缓存
     */
    public User findById(Long id) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. 先查 Redis 缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("缓存命中: {}", cacheKey);
            try {
                return objectMapper.readValue(cached, User.class);
            } catch (JsonProcessingException e) {
                log.warn("缓存反序列化失败: {}", e.getMessage());
            }
        }

        // 2. 缓存未命中，查询数据库
        log.info("缓存未命中，查询数据库: id={}", id);
        List<User> users = jdbcTemplate.query(
                "SELECT id, name, email, age, create_time FROM users WHERE id = ?",
                USER_ROW_MAPPER, id
        );

        if (users.isEmpty()) {
            return null;
        }

        User user = users.get(0);

        // 3. 回填 Redis 缓存
        try {
            String json = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            log.info("缓存回填: {}", cacheKey);
        } catch (JsonProcessingException e) {
            log.warn("缓存序列化失败: {}", e.getMessage());
        }

        return user;
    }

    /**
     * 查询所有用户
     */
    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT id, name, email, age, create_time FROM users ORDER BY id",
                USER_ROW_MAPPER
        );
    }

    /**
     * 创建用户
     */
    public User create(User user) {
        jdbcTemplate.update(
                "INSERT INTO users (name, email, age) VALUES (?, ?, ?)",
                user.getName(), user.getEmail(), user.getAge()
        );

        // 查询刚插入的用户（获取自增 ID 和默认值）
        List<User> users = jdbcTemplate.query(
                "SELECT id, name, email, age, create_time FROM users WHERE name = ? ORDER BY id DESC LIMIT 1",
                USER_ROW_MAPPER, user.getName()
        );
        return users.isEmpty() ? user : users.get(0);
    }

    /**
     * 按名称模糊查询
     */
    public List<User> searchByName(String name) {
        return jdbcTemplate.query(
                "SELECT id, name, email, age, create_time FROM users WHERE name LIKE ? ORDER BY id",
                USER_ROW_MAPPER, "%" + name + "%"
        );
    }
}

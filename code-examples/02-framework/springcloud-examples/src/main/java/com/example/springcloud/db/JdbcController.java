package com.example.springcloud.db;

import com.example.springcloud.common.Result;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 10.A.19 JDBC 数据库操作 Controller
 *
 * <p>演示 Spring JDBC 的核心功能：
 * <ul>
 *   <li>JdbcTemplate 增删改查</li>
 *   <li>HikariCP 连接池状态监控</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 初始化测试表和数据
 * curl http://localhost:8090/demo/db/init
 *
 * # 查询所有用户
 * curl http://localhost:8090/demo/db/users
 *
 * # 查询单个用户
 * curl http://localhost:8090/demo/db/users/1
 *
 * # 创建用户
 * curl -X POST http://localhost:8090/demo/db/users \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"newUser","email":"new@example.com","age":28}'
 *
 * # 查看连接池状态
 * curl http://localhost:8090/demo/db/pool
 * </pre>
 */
@RestController
@RequestMapping("/demo/db")
public class JdbcController {

    private static final Logger log = LoggerFactory.getLogger(JdbcController.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public JdbcController(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    // ==================== 初始化 ====================

    /**
     * 初始化测试表和数据
     *
     * <p>创建 users 表并插入 5 条测试数据。
     *
     * <pre>
     * curl http://localhost:8090/demo/db/init
     * </pre>
     *
     * @return 初始化结果
     */
    @GetMapping("/init")
    public Result<Map<String, Object>> init() {
        log.info("[JDBC] 开始初始化测试表和数据");

        // 创建 users 表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    age INT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        // 插入测试数据（使用 INSERT IGNORE 避免重复插入）
        String insertSql = "INSERT IGNORE INTO users (id, name, email, age) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, 1, "张三", "zhangsan@example.com", 25);
        jdbcTemplate.update(insertSql, 2, "李四", "lisi@example.com", 30);
        jdbcTemplate.update(insertSql, 3, "王五", "wangwu@example.com", 28);
        jdbcTemplate.update(insertSql, 4, "赵六", "zhaoliu@example.com", 35);
        jdbcTemplate.update(insertSql, 5, "孙七", "sunqi@example.com", 22);

        log.info("[JDBC] 测试表和数据初始化完成");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "测试表 users 初始化完成");
        data.put("表名", "users");
        data.put("插入记录数", 5);
        return Result.ok(data);
    }

    // ==================== 用户 CRUD ====================

    /**
     * 查询所有用户
     *
     * <pre>
     * curl http://localhost:8090/demo/db/users
     * </pre>
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");
        log.info("[JDBC] 查询所有用户，共 {} 条", users.size());
        return Result.ok(users);
    }

    /**
     * 查询单个用户
     *
     * <pre>
     * curl http://localhost:8090/demo/db/users/1
     * </pre>
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUser(@PathVariable Long id) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM users WHERE id = ?", id);
        if (users.isEmpty()) {
            log.warn("[JDBC] 用户不存在: id={}", id);
            return Result.fail(404, "用户不存在: id=" + id);
        }
        log.info("[JDBC] 查询用户: id={}", id);
        return Result.ok(users.get(0));
    }

    /**
     * 创建用户
     *
     * <pre>
     * curl -X POST http://localhost:8090/demo/db/users \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"newUser","email":"new@example.com","age":28}'
     * </pre>
     *
     * @param body 用户信息（name, email, age）
     * @return 创建结果
     */
    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        Integer age = (Integer) body.get("age");

        jdbcTemplate.update("INSERT INTO users (name, email, age) VALUES (?, ?, ?)",
                name, email, age);
        log.info("[JDBC] 创建用户: name={}, email={}, age={}", name, email, age);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "用户创建成功");
        data.put("name", name);
        data.put("email", email);
        data.put("age", age);
        return Result.ok(data);
    }

    // ==================== 连接池监控 ====================

    /**
     * 查看 HikariCP 连接池状态
     *
     * <pre>
     * curl http://localhost:8090/demo/db/pool
     * </pre>
     *
     * @return 连接池状态信息
     */
    @GetMapping("/pool")
    public Result<Map<String, Object>> poolStatus() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "HikariCP 连接池状态");
        data.put("activeConnections", poolMXBean.getActiveConnections());
        data.put("idleConnections", poolMXBean.getIdleConnections());
        data.put("totalConnections", poolMXBean.getTotalConnections());
        data.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
        data.put("poolName", hikariDataSource.getPoolName());
        data.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());

        log.info("[JDBC] 连接池状态: active={}, idle={}, total={}",
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getTotalConnections());
        return Result.ok(data);
    }
}

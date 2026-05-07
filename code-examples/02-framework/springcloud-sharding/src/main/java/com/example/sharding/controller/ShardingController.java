package com.example.sharding.controller;

import com.example.sharding.entity.OrderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 分库分表演示控制器
 *
 * 通过 ShardingSphere-JDBC 实现透明分片，应用层无需关心数据路由。
 * 逻辑表 t_order 自动映射到物理表 t_order_0 ~ t_order_3。
 *
 * 测试命令：
 * {@code curl http://localhost:8091/demo/sharding/init}
 * {@code curl -X POST http://localhost:8091/demo/sharding/order -H "Content-Type: application/json" -d '{"userId":1,"amount":99.9,"status":"NEW"}'}
 * {@code curl http://localhost:8091/demo/sharding/orders}
 * {@code curl http://localhost:8091/demo/sharding/orders/1}
 * {@code curl http://localhost:8091/demo/sharding/compare}
 */
@RestController
@RequestMapping("/demo/sharding")
public class ShardingController {

    private static final Logger log = LoggerFactory.getLogger(ShardingController.class);

    private final JdbcTemplate jdbcTemplate;

    public ShardingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 订单行映射器 */
    private static final RowMapper<OrderEntity> ORDER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        OrderEntity order = new OrderEntity();
        order.setOrderId(rs.getLong("order_id"));
        order.setUserId(rs.getLong("user_id"));
        order.setAmount(rs.getBigDecimal("amount"));
        order.setStatus(rs.getString("status"));
        order.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return order;
    };

    /**
     * 初始化分片表和测试数据
     *
     * 创建 t_order_0 ~ t_order_3 四张物理表，并插入测试数据。
     * ShardingSphere 会根据分片规则自动路由到对应的物理表。
     *
     * curl http://localhost:8091/demo/sharding/init
     */
    @GetMapping("/init")
    public Map<String, Object> init() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 循环创建 4 张分片表
        for (int i = 0; i < 4; i++) {
            String tableName = "t_order_" + i;
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "order_id BIGINT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'NEW', " +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            jdbcTemplate.execute(sql);
            log.info("创建分片表: {}", tableName);
        }
        result.put("tables", "t_order_0 ~ t_order_3 创建完成");

        // 插入测试数据（通过逻辑表 t_order，ShardingSphere 自动路由）
        String insertSql = "INSERT INTO t_order (user_id, amount, status, create_time) VALUES (?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (long userId = 1; userId <= 3; userId++) {
            for (int j = 1; j <= 3; j++) {
                BigDecimal amount = BigDecimal.valueOf(userId * 100 + j * 10 + 0.5);
                jdbcTemplate.update(insertSql, userId, amount, "NEW", now);
                count++;
            }
        }
        result.put("insertedRows", count);
        result.put("message", "初始化完成，插入 " + count + " 条测试订单");

        return result;
    }

    /**
     * 插入订单
     *
     * 接收 JSON body，通过 ShardingSphere 自动路由到分片表。
     *
     * curl -X POST http://localhost:8091/demo/sharding/order \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":1,"amount":99.9,"status":"NEW"}'
     */
    @PostMapping("/order")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String status = body.getOrDefault("status", "NEW").toString();

        String sql = "INSERT INTO t_order (user_id, amount, status, create_time) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, amount, status, LocalDateTime.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "订单创建成功");
        result.put("userId", userId);
        result.put("amount", amount);
        result.put("status", status);
        result.put("note", "ShardingSphere 根据 order_id 自动路由到分片表");
        return result;
    }

    /**
     * 查询所有订单（归并结果）
     *
     * ShardingSphere 会查询所有分片表并归并结果。
     *
     * curl http://localhost:8091/demo/sharding/orders
     */
    @GetMapping("/orders")
    public Map<String, Object> listOrders() {
        List<OrderEntity> orders = jdbcTemplate.query(
                "SELECT order_id, user_id, amount, status, create_time FROM t_order ORDER BY order_id",
                ORDER_ROW_MAPPER
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", orders.size());
        result.put("orders", orders);
        result.put("note", "ShardingSphere 自动归并 t_order_0 ~ t_order_3 的查询结果");
        return result;
    }

    /**
     * 按 userId 查询订单
     *
     * curl http://localhost:8091/demo/sharding/orders/1
     */
    @GetMapping("/orders/{userId}")
    public Map<String, Object> listOrdersByUserId(@PathVariable Long userId) {
        List<OrderEntity> orders = jdbcTemplate.query(
                "SELECT order_id, user_id, amount, status, create_time FROM t_order WHERE user_id = ? ORDER BY order_id",
                ORDER_ROW_MAPPER,
                userId
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("total", orders.size());
        result.put("orders", orders);
        return result;
    }

    /**
     * 分库分表方案对比
     *
     * curl http://localhost:8091/demo/sharding/compare
     */
    @GetMapping("/compare")
    public Map<String, Object> compare() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "分库分表方案对比");

        List<Map<String, String>> solutions = new ArrayList<>();

        solutions.add(createSolution(
                "ShardingSphere-JDBC",
                "客户端分片（嵌入式 JAR）",
                "无需额外部署，性能好，直连数据库",
                "应用侵入性较强，每个服务都需要配置",
                "中小型项目，Java 技术栈"
        ));

        solutions.add(createSolution(
                "ShardingSphere-Proxy",
                "服务端代理（独立进程）",
                "对应用透明，支持多语言，统一管理",
                "多一层网络开销，需要独立运维",
                "多语言项目，需要统一数据治理"
        ));

        solutions.add(createSolution(
                "MyCat",
                "数据库中间件（独立进程）",
                "成熟稳定，社区活跃，支持多种数据库",
                "配置复杂，性能一般，社区维护减少",
                "传统项目迁移，MySQL 为主"
        ));

        solutions.add(createSolution(
                "Vitess",
                "云原生分片方案（YouTube 开源）",
                "云原生，水平扩展能力强，Kubernetes 友好",
                "学习曲线陡峭，生态偏向 MySQL",
                "大规模云原生项目，Kubernetes 环境"
        ));

        solutions.add(createSolution(
                "TiDB",
                "分布式 NewSQL 数据库",
                "兼容 MySQL 协议，自动分片，强一致性",
                "资源消耗大，至少 3 节点，运维复杂",
                "大数据量、高并发、强一致性场景"
        ));

        result.put("solutions", solutions);
        result.put("recommendation", "中小项目推荐 ShardingSphere-JDBC，大型项目考虑 TiDB 或 Vitess");
        return result;
    }

    /** 构建方案对比项 */
    private Map<String, String> createSolution(String name, String type, String pros, String cons, String scenario) {
        Map<String, String> solution = new LinkedHashMap<>();
        solution.put("name", name);
        solution.put("type", type);
        solution.put("pros", pros);
        solution.put("cons", cons);
        solution.put("scenario", scenario);
        return solution;
    }
}

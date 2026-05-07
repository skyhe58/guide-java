package com.example.springcloud.mongo;

import com.example.springcloud.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 10.A.21 MongoDB 数据操作 Controller
 *
 * <p>演示 Spring Data MongoDB 的核心功能：
 * <ul>
 *   <li>文档的 CRUD 操作</li>
 *   <li>按名称模糊查询</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 初始化测试数据
 * curl http://localhost:8090/demo/mongo/init
 *
 * # 查询所有用户
 * curl http://localhost:8090/demo/mongo/users
 *
 * # 查询单个用户
 * curl http://localhost:8090/demo/mongo/users/1
 *
 * # 创建用户
 * curl -X POST http://localhost:8090/demo/mongo/users \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"测试用户","email":"test@example.com","age":25,"skills":["Go","Rust"]}'
 *
 * # 按名称模糊查询
 * curl "http://localhost:8090/demo/mongo/users/search?name=张"
 * </pre>
 */
@RestController
@RequestMapping("/demo/mongo")
public class MongoController {

    private static final Logger log = LoggerFactory.getLogger(MongoController.class);

    private final UserMongoRepository userMongoRepository;

    public MongoController(UserMongoRepository userMongoRepository) {
        this.userMongoRepository = userMongoRepository;
    }

    // ==================== 初始化 ====================

    /**
     * 初始化测试数据
     *
     * <p>清空集合后批量插入 5 个测试用户。
     *
     * @return 初始化结果
     */
    @GetMapping("/init")
    public Result<String> init() {
        log.info("[MongoDB] 开始初始化测试数据");

        // 先清空所有数据
        userMongoRepository.deleteAll();

        // 批量插入测试用户
        List<UserDocument> users = new ArrayList<>();
        users.add(new UserDocument("1", "张三", "zhangsan@example.com", 25,
                List.of("Java", "Spring", "MySQL"), LocalDateTime.now()));
        users.add(new UserDocument("2", "李四", "lisi@example.com", 30,
                List.of("Python", "Django", "PostgreSQL"), LocalDateTime.now()));
        users.add(new UserDocument("3", "王五", "wangwu@example.com", 28,
                List.of("Go", "Gin", "Redis"), LocalDateTime.now()));
        users.add(new UserDocument("4", "赵六", "zhaoliu@example.com", 35,
                List.of("JavaScript", "React", "Node.js"), LocalDateTime.now()));
        users.add(new UserDocument("5", "孙七", "sunqi@example.com", 22,
                List.of("Rust", "C++", "Linux"), LocalDateTime.now()));

        userMongoRepository.saveAll(users);
        log.info("[MongoDB] 测试数据初始化完成，共 {} 个用户", users.size());

        return Result.ok("初始化完成，共插入 " + users.size() + " 个用户");
    }

    // ==================== 用户 CRUD ====================

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    public Result<List<UserDocument>> listAll() {
        List<UserDocument> users = userMongoRepository.findAll();
        log.info("[MongoDB] 查询所有用户，共 {} 个", users.size());
        return Result.ok(users);
    }

    /**
     * 查询单个用户
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    public Result<UserDocument> getUser(@PathVariable String id) {
        Optional<UserDocument> user = userMongoRepository.findById(id);
        if (user.isEmpty()) {
            log.warn("[MongoDB] 用户不存在: id={}", id);
            return Result.fail(404, "用户不存在: id=" + id);
        }
        log.info("[MongoDB] 查询用户: id={}", id);
        return Result.ok(user.get());
    }

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建后的用户
     */
    @PostMapping("/users")
    public Result<UserDocument> create(@RequestBody UserDocument user) {
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }
        UserDocument saved = userMongoRepository.save(user);
        log.info("[MongoDB] 创建用户: id={}, name={}", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    /**
     * 按名称模糊查询
     *
     * @param name 用户名关键词
     * @return 匹配的用户列表
     */
    @GetMapping("/users/search")
    public Result<List<UserDocument>> searchByName(@RequestParam String name) {
        List<UserDocument> users = userMongoRepository.findByNameContaining(name);
        log.info("[MongoDB] 按名称搜索: name={}, 命中 {} 个用户", name, users.size());
        return Result.ok(users);
    }
}

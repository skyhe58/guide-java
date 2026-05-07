package com.example.springcloud.session;

import com.example.springcloud.common.Result;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 10.A.25 Session 管理 + JWT 认证实战 Controller
 *
 * <p>演示两种主流的用户会话管理方案：
 * <ul>
 *   <li>Redis Session：服务端存储会话，sessionId 作为凭证</li>
 *   <li>JWT Token：无状态认证，Token 自包含用户信息</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # ===== Redis Session =====
 * # 登录（返回 sessionId）
 * curl -X POST "http://localhost:8090/demo/session/login?username=alice"
 *
 * # 查看 Session 信息（替换为实际 sessionId）
 * curl "http://localhost:8090/demo/session/info?sessionId=xxx"
 *
 * # 登出
 * curl -X POST "http://localhost:8090/demo/session/logout?sessionId=xxx"
 *
 * # ===== JWT Token =====
 * # JWT 登录（返回 Token）
 * curl -X POST "http://localhost:8090/demo/session/jwt/login?username=alice"
 *
 * # JWT 验证（替换为实际 Token）
 * curl "http://localhost:8090/demo/session/jwt/verify?token=xxx"
 *
 * # 方案对比
 * curl http://localhost:8090/demo/session/compare
 * </pre>
 */
@RestController
@RequestMapping("/demo/session")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private static final String SESSION_PREFIX = "session:";
    private static final long SESSION_TTL_MINUTES = 30;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate redisTemplate;

    public SessionController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Redis Session 登录 — 生成 sessionId 存入 Redis
     *
     * @param username 用户名
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String redisKey = SESSION_PREFIX + sessionId;

        // 构建 Session 数据（JSON 格式存储）
        String sessionData = String.format(
                "{\"username\":\"%s\",\"loginTime\":\"%s\",\"sessionId\":\"%s\"}",
                username, LocalDateTime.now().format(FMT), sessionId);

        redisTemplate.opsForValue().set(redisKey, sessionData, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("[Session] 用户登录: username={}, sessionId={}", username, sessionId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "Redis Session 登录成功");
        data.put("sessionId", sessionId);
        data.put("username", username);
        data.put("过期时间", SESSION_TTL_MINUTES + " 分钟");
        data.put("Redis Key", redisKey);
        return Result.ok(data);
    }

    /**
     * 获取当前 Session 信息
     *
     * @param sessionId 会话 ID
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestParam String sessionId) {
        String redisKey = SESSION_PREFIX + sessionId;
        String sessionData = redisTemplate.opsForValue().get(redisKey);

        if (sessionData == null) {
            return Result.fail(401, "Session 不存在或已过期");
        }

        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        log.info("[Session] 查询 Session: sessionId={}", sessionId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("sessionData", sessionData);
        data.put("剩余有效期秒", ttl);
        return Result.ok(data);
    }

    /**
     * 销毁 Session（登出）
     *
     * @param sessionId 会话 ID
     */
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout(@RequestParam String sessionId) {
        String redisKey = SESSION_PREFIX + sessionId;
        Boolean deleted = redisTemplate.delete(redisKey);
        log.info("[Session] 用户登出: sessionId={}, deleted={}", sessionId, deleted);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "Session 已销毁");
        data.put("sessionId", sessionId);
        data.put("是否删除成功", Boolean.TRUE.equals(deleted));
        return Result.ok(data);
    }

    /**
     * JWT Token 登录 — 生成 JWT Token
     *
     * @param username 用户名
     */
    @PostMapping("/jwt/login")
    public Result<Map<String, Object>> jwtLogin(@RequestParam String username) {
        String token = JwtUtil.generateToken(username);
        log.info("[JWT] 用户登录: username={}", username);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "JWT Token 登录成功");
        data.put("username", username);
        data.put("token", token);
        data.put("有效期", "1 小时");
        return Result.ok(data);
    }

    /**
     * JWT Token 验证 — 解析 Token 返回用户信息
     *
     * @param token JWT Token 字符串
     */
    @GetMapping("/jwt/verify")
    public Result<Map<String, Object>> jwtVerify(@RequestParam String token) {
        if (!JwtUtil.isTokenValid(token)) {
            return Result.fail(401, "Token 无效或已过期");
        }

        Claims claims = JwtUtil.parseToken(token);
        log.info("[JWT] Token 验证成功: subject={}", claims.getSubject());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "Token 验证通过");
        data.put("username", claims.getSubject());
        data.put("签发时间", claims.getIssuedAt());
        data.put("过期时间", claims.getExpiration());
        return Result.ok(data);
    }

    /**
     * Session vs JWT 方案对比
     */
    @GetMapping("/compare")
    public Result<List<Map<String, Object>>> compare() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("方案", "Redis Session");
        session.put("存储位置", "服务端（Redis）");
        session.put("凭证", "sessionId（随机字符串）");
        session.put("优点", "服务端可控，可随时踢人下线，安全性高");
        session.put("缺点", "依赖 Redis，有状态，水平扩展需共享 Session");
        session.put("适用场景", "传统 Web 应用，需要服务端控制会话的场景");
        list.add(session);

        Map<String, Object> jwt = new LinkedHashMap<>();
        jwt.put("方案", "JWT Token");
        jwt.put("存储位置", "客户端（Token 自包含信息）");
        jwt.put("凭证", "JWT Token（Base64 编码的 JSON）");
        jwt.put("优点", "无状态，不依赖服务端存储，天然支持分布式");
        jwt.put("缺点", "无法主动失效（需配合黑名单），Token 较大，载荷不宜存敏感信息");
        jwt.put("适用场景", "微服务架构，移动端 API，前后端分离项目");
        list.add(jwt);

        return Result.ok(list);
    }
}

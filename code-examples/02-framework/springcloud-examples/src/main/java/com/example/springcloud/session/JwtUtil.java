package com.example.springcloud.session;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 10.A.25 JWT 工具类
 *
 * <p>基于 io.jsonwebtoken (jjwt 0.12.5) 实现 JWT Token 的生成、解析和验证。
 * <ul>
 *   <li>HS256 签名算法</li>
 *   <li>Token 有效期 1 小时</li>
 *   <li>固定密钥（仅 demo 用途，生产环境应使用配置中心管理密钥）</li>
 * </ul>
 */
public class JwtUtil {

    /** 固定密钥（demo 用途，至少 32 字节以满足 HS256 要求） */
    private static final String SECRET = "SpringCloudDemoSecretKey2024!@#$%^&*()";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** Token 有效期：1 小时 */
    private static final long EXPIRATION_MS = 3600_000L;

    private JwtUtil() {
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @return JWT Token 字符串
     */
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 JWT Token，返回 Claims
     *
     * @param token JWT Token 字符串
     * @return Claims 载荷信息
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     *
     * @param token JWT Token 字符串
     * @return true 表示有效
     */
    public static boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}

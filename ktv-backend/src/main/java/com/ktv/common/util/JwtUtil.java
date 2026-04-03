package com.ktv.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥（必须从配置文件或环境变量设置，无默认值防止源码泄露后被伪造）
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT过期时间（毫秒），默认2小时
     */
    @Value("${jwt.expiration:7200000}")
    private Long expiration;

    /**
     * 启动时校验JWT密钥强度，确保密钥已配置且长度足够
     */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT密钥未配置！请在 application.yml 中设置 jwt.secret 或通过环境变量 JWT_SECRET 注入");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT密钥长度不足，至少需要32个字符，当前仅 " + secret.length() + " 个字符");
        }
        log.info("JWT密钥校验通过，长度：{} 字符", secret.length());
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @return JWT Token
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return generateToken(claims);
    }

    /**
     * 生成JWT Token
     *
     * @param claims 自定义声明
     * @return JWT Token
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(claims.get("username") != null ? claims.get("username").toString() : "")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 从Token中获取Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从Token中获取角色
     *
     * @param token JWT Token
     * @return 角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 验证Token是否过期
     *
     * @param token JWT Token
     * @return true-已过期，false-未过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证Token是否有效（只检查签名和过期时间，不校验用户名）
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public Boolean validateToken(String token) {
        try {
            // parseSignedClaims 会验证签名和过期时间，抛异常说明无效
            getClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证Token是否有效（同时校验用户名）
     * H8修复：增加null检查，防止tokenUsername为null时NPE
     *
     * @param token    JWT Token
     * @param username 用户名
     * @return true-有效，false-无效
     */
    public Boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            return tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取密钥
     *
     * @return SecretKey
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

}

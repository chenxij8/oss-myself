package com.oss.file.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 用于生成、验证和解析 JWT Token。
 *
 * 性能要点：密钥只在启动时构造一次并缓存，避免每个请求重复 Base64 解码与 Key 构造；
 * 过滤器可通过 {@link #parseToken(String)} 一次解析拿到全部声明。
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:FileServerSecurityKeyForJWTTokenGenerationAndValidation2024021201}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /** 缓存的签名密钥，HMAC-SHA 要求至少 256 bit */
    private volatile SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(resolveKeyBytes(secret));
        log.info("JWT 签名密钥已初始化（长度 {} 位）", resolveKeyBytes(secret).length * 8);
    }

    /**
     * 将配置密钥转换为合法长度（>=32 字节）的字节数组。
     * 优先按 Base64 解码；若不是合法 Base64 或解码后不足 32 字节，
     * 则使用原始 UTF-8 字节，必要时用 SHA-256 派生，保证行为稳定可预期。
     */
    private byte[] resolveKeyBytes(String configured) {
        String value = configured == null || configured.isBlank()
                ? "FileServerSecurityKeyForJWTTokenGenerationAndValidation2024021201"
                : configured.trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // 普通字符串密钥，走下面的 UTF-8 / SHA-256 分支
        }
        byte[] raw = value.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化 JWT 密钥", e);
        }
    }

    private SecretKey getSigningKey() {
        SecretKey key = signingKey;
        if (key == null) {
            synchronized (this) {
                if (signingKey == null) {
                    signingKey = Keys.hmacShaKeyFor(resolveKeyBytes(secret));
                }
                key = signingKey;
            }
        }
        return key;
    }

    /**
     * 生成 Token（默认过期时间）
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    /**
     * 生成 Token（自定义过期时间）
     */
    public String generateToken(Map<String, Object> claims, String subject, Long expirationMillis) {
        return createToken(claims, subject, expirationMillis);
    }

    /**
     * 生成 Token（仅用户 ID）
     */
    public String generateToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return generateToken(claims, userId);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationMillis == null ? expiration : expirationMillis));
        return Jwts.builder()
                .claims(claims == null ? new HashMap<>() : claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 一次解析 Token，返回全部声明；签名无效或已过期时返回 null。
     * jjwt 的 parseSignedClaims 本身会校验 exp，无需重复解析。
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Token 中的声明信息
     */
    public Claims getClaimsFromToken(String token) {
        return parseToken(token);
    }

    /**
     * 获取 Token 中的用户 ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 获取 Token 中的某个声明值
     */
    public Object getClaimFromToken(String token, String key) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(key) : null;
    }

    /**
     * 判断 Token 是否已过期（无法解析时视为过期）
     */
    public Boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        Date exp = claims.getExpiration();
        return exp == null || exp.before(new Date());
    }

    /**
     * 校验 Token 是否有效：一次解析完成签名与过期校验
     */
    public Boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 刷新 Token（保留原声明，重新设置签发与过期时间）
     */
    public String refreshToken(String token) {
        Claims old = parseToken(token);
        if (old == null) {
            return null;
        }
        Map<String, Object> claims = new HashMap<>(old);
        claims.remove("exp");
        claims.remove("iat");
        return generateToken(claims, old.getSubject());
    }
}

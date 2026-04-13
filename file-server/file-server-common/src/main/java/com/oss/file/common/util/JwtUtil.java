package com.oss.file.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成、验证和解析JWT Token
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:FileServerSecurityKeyForJWTTokenGenerationAndValidation2024021201}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成Token（默认过期时间）
     * @param claims 声明信息
     * @param subject Token主体（通常是用户ID）
     * @return JWT Token
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    /**
     * 生成Token（自定义过期时间）
     * @param claims 声明信息
     * @param subject Token主体
     * @param expirationMillis 过期时间（毫秒）
     * @return JWT Token
     */
    public String generateToken(Map<String, Object> claims, String subject, Long expirationMillis) {
        return createToken(claims, subject, expirationMillis);
    }

    /**
     * 生成Token（仅用户ID）
     * @param userId 用户ID
     * @return JWT Token
     */
    public String generateToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return generateToken(claims, userId);
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 获取Token中的声明信息
     * @param token JWT Token
     * @return Claims对象
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Token中的用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 获取Token中的声明值
     * @param token JWT Token
     * @param key 声明key
     * @return 声明值
     */
    public Object getClaimFromToken(String token, String key) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get(key) : null;
    }

    /**
     * 验证Token是否过期
     * @param token JWT Token
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return true;
        }
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新Token
     * @param token 旧Token
     * @return 新Token
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            // 移除过期时间，重新设置
            claims.remove("exp");
            claims.remove("iat");
            return generateToken(new HashMap<>(claims), claims.getSubject());
        }
        return null;
    }
}

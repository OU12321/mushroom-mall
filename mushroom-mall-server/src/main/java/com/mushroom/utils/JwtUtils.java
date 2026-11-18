package com.mushroom.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 (登录鉴权)
 */
@Component
public class JwtUtils {

    @Value("${mushroom.jwt-secret}")
    private String secret;

    @Value("${mushroom.jwt-expiration}")
    private int expiration;

    /**
     * 生成 JWT
     * @param userId 用户ID
     * @param role   角色: 0=管理员 1=商家 2=普通用户
     */
    public String generateToken(Long userId, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 3600 * 1000L))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 解析 JWT
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 token 获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object userId = claims.get("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    /**
     * 从 token 获取角色
     */
    public Integer getRole(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object role = claims.get("role");
        return role == null ? null : Integer.valueOf(role.toString());
    }

    /**
     * 校验 token 是否有效
     */
    public boolean isValid(String token) {
        return parseToken(token) != null;
    }
}

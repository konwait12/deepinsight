package com.deepinsight.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌工具类
 * <p>
 * 负责JWT令牌的生成、解析和验证。
 * 使用HS256（HMAC-SHA256）对称加密算法签名令牌。
 *
 * <p>JWT令牌结构：
 * <pre>
 * Header:  {"alg": "HS256", "typ": "JWT"}
 * Payload: {"sub": "用户名", "iat": 签发时间, "exp": 过期时间}
 * Signature: HMAC-SHA256(base64Url(header).base64Url(payload), secret)
 * </pre>
 *
 * <p>安全说明：
 * <ul>
 *   <li>密钥从application.yml的security.jwt.secret读取（至少256位）</li>
 *   <li>令牌过期时间默认24小时，由security.jwt.expiration配置</li>
 *   <li>令牌无状态，登出需前端清除Token</li>
 * </ul>
 *
 * @author DeepInsight Team
 */
@Component
public class JwtUtils {

    /** 从配置文件注入的JWT签名密钥（Base64编码） */
    @Value("${security.jwt.secret}")
    private String secretKey;

    /** 从配置文件注入的JWT过期时间（毫秒） */
    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * 从令牌中提取用户名（Subject声明）
     *
     * @param token JWT令牌字符串
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取指定的声明
     *
     * @param token          JWT令牌字符串
     * @param claimsResolver 声明解析函数（如 Claims::getSubject, Claims::getExpiration）
     * @param <T>            返回类型
     * @return 声明的值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 生成JWT令牌（无额外声明）
     * <p>
     * 令牌包含：sub(用户名)、iat(签发时间)、exp(过期时间)。
     *
     * @param userDetails 用户信息
     * @return JWT令牌字符串
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 生成JWT令牌（可添加自定义声明）
     * <p>
     * 如需要在令牌中添加角色、权限等自定义信息，
     * 通过extraClaims传入，会写入Payload中。
     *
     * @param extraClaims 自定义声明（如角色、权限等）
     * @param userDetails 用户信息
     * @return JWT令牌字符串
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证令牌是否有效
     * <p>
     * 同时检查：令牌中的用户名是否匹配、令牌是否未过期。
     *
     * @param token       JWT令牌字符串
     * @param userDetails 当前用户信息
     * @return true表示令牌有效
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 判断令牌是否已过期
     *
     * @param token JWT令牌字符串
     * @return true表示已过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 从令牌中提取过期时间
     *
     * @param token JWT令牌字符串
     * @return 过期时间
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 解析令牌，提取所有声明
     * <p>
     * 使用签名密钥验证令牌完整性。如果令牌被篡改或签名不匹配，抛出异常。
     *
     * @param token JWT令牌字符串
     * @return 令牌中的所有声明
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取签名密钥
     * <p>
     * 将Base64编码的密钥字符串解码后，转换为HMAC-SHA256签名密钥。
     *
     * @return 签名密钥对象
     */
    private Key getSignInKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be set before using authenticated endpoints.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException ex) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes after Base64 decoding or UTF-8 encoding.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

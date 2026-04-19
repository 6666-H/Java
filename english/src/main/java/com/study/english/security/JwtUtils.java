package com.study.english.security;

import com.study.english.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    public String generate(Long userId, String tenantId, String username, String role, int tokenVersion) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenantId", tenantId)
                .claim("username", username)
                .claim("role", role)
                .claim("tv", tokenVersion)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(Claims claims) {
        String sub = claims.getSubject();
        return sub == null ? null : Long.parseLong(sub);
    }

    public String getTenantId(Claims claims) {
        Object v = claims.get("tenantId");
        if (v == null) return null;
        return v.toString();
    }

    public String getUsername(Claims claims) {
        Object v = claims.get("username");
        return v == null ? null : v.toString();
    }

    public String getRole(Claims claims) {
        Object v = claims.get("role");
        return v == null ? null : v.toString();
    }

    /** 令牌版本（单设备登录校验） */
    public Integer getTokenVersion(Claims claims) {
        Object v = claims.get("tv");
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

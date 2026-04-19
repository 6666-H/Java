package com.study.english.security;

import com.study.english.context.TenantContext;
import com.study.english.entity.User;
import com.study.english.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从 Authorization: Bearer &lt;token&gt; 解析 JWT，将 tenantId/userId/username/role 写入 TenantContext。
 * 校验 token_version 实现单设备登录：仅最新登录设备的 token 有效。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    @Lazy
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7).trim();
                if (!token.isEmpty()) {
                    Claims claims = jwtUtils.parse(token);
                    Long userId = jwtUtils.getUserId(claims);
                    Integer tokenTv = jwtUtils.getTokenVersion(claims);
                    if (userId != null && tokenTv != null) {
                        User user = userService.getById(userId);
                        Integer currentTv = user != null ? user.getTokenVersion() : null;
                        if (currentTv != null && currentTv.equals(tokenTv)) {
                            String tenantId = jwtUtils.getTenantId(claims);
                            String username = jwtUtils.getUsername(claims);
                            String role = jwtUtils.getRole(claims);
                            TenantContext.set(tenantId, userId, username, role);
                        } else {
                            log.debug("Token invalid: userId={} tokenVersion mismatch", userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("JWT parse failed: {}", e.getMessage());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

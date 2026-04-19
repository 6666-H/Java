package com.study.english.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 除登录等白名单外，所有请求必须已通过 JWT 解析出用户（TenantContext.getUserId() != null），否则 401。
 */
@Slf4j
@Component
public class AuthRequiredInterceptor implements HandlerInterceptor {

    private static final List<String> EXCLUDE_PREFIXES = List.of(
            "/api/auth/login",
            "/error",
            "/actuator",
            "/swagger", "/v3/api-docs"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        for (String p : EXCLUDE_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        if (TenantContext.getUserId() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.fail("UNAUTHORIZED", "未登录或登录已过期")));
            return false;
        }
        return true;
    }
}

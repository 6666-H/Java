package com.study.english.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.Tenant;
import com.study.english.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在所有需要租户的业务前：若当前上下文有 tenantId，则校验该租户 status 启用且 expire_time 未过期；
 * 若不满足，返回 403 且 body 为 {"code":"Tenant_Access_Denied","message":"租户已禁用或已过期"}。
 * 超级管理员（无 tenantId）不校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCheckInterceptor implements HandlerInterceptor {

    private static final String CODE_TENANT_ACCESS_DENIED = "Tenant_Access_Denied";

    private final TenantService tenantService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 仅对以下前缀的路径做租户校验；登录等已在 Security 白名单，不会进此拦截器时带 tenant。 */
    private static final List<String> TENANT_REQUIRED_PREFIXES = List.of("/api/", "/tenant/", "/admin/");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true;
        }
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            write403(response, "租户不存在");
            return false;
        }
        if (tenant.getStatus() == null || tenant.getStatus() != Tenant.STATUS_ENABLED) {
            write403(response, "租户已禁用或已过期");
            return false;
        }
        LocalDateTime expire = tenant.getExpireTime();
        if (expire == null || expire.isBefore(LocalDateTime.now())) {
            write403(response, "租户已禁用或已过期");
            return false;
        }
        return true;
    }

    private void write403(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(CODE_TENANT_ACCESS_DENIED, message)));
    }
}

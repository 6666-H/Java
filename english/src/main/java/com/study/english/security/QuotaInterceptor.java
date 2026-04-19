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

/**
 * 权限网关：当任何角色请求时，若带 tenant_id 则校验租户 expire_time 未到期且 status 启用；
 * ORG_ADMIN 创建学生时的名额校验在 UserServiceImpl.createUser 事务内完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaInterceptor implements HandlerInterceptor {

    private static final String CODE_TENANT_ACCESS_DENIED = "Tenant_Access_Denied";

    private final TenantService tenantService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) return true;

        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            write403(response, "账号到期，续签请联系管理员");
            return false;
        }
        if (tenant.getStatus() == null || tenant.getStatus() != Tenant.STATUS_ENABLED) {
            write403(response, "账号到期，续签请联系管理员");
            return false;
        }
        LocalDateTime expire = tenant.getExpireTime();
        if (expire == null || expire.isBefore(LocalDateTime.now())) {
            write403(response, "账号到期，续签请联系管理员");
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

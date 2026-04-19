package com.study.english.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文：使用 ThreadLocal 存储当前请求的 tenantId、userId、role。
 * 在 JWT 解析后由 Filter 设置，在请求结束时清除。
 */
@Slf4j
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String getRole() {
        return ROLE.get();
    }

    /** 是否超级管理员（无租户） */
    public static boolean isSuperAdmin() {
        return getTenantId() == null && "SUPER_ADMIN".equals(getRole());
    }

    /** 设置完整上下文 */
    public static void set(String tenantId, Long userId, String username, String role) {
        setTenantId(tenantId);
        setUserId(userId);
        setUsername(username);
        setRole(role);
    }

    /** 清除上下文，避免线程池复用导致泄漏 */
    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
    }
}

package com.study.english.controller.admin;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.AdminTenantOverviewDto;
import com.study.english.dto.TenantUpdateRequest;
import com.study.english.entity.Tenant;
import com.study.english.entity.TenantBookAuth;
import com.study.english.entity.StudyLog;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudyLogService;
import com.study.english.service.TenantBookAuthService;
import com.study.english.service.TenantService;
import com.study.english.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 超级管理员：租户管控。设置 account_quota（账号上限）、expire_time（到期日期）。
 */
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final TenantService tenantService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final StudyLogService studyLogService;
    private final TenantBookAuthService tenantBookAuthService;

    /**
     * 租户列表。
     *
     * @return Result.data 全部租户列表
     */
    @GetMapping
    public Result<List<TenantRow>> list() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        return Result.ok(buildTenantRows().stream().map(this::toLegacyTenantRow).toList());
    }

    @GetMapping("/overview")
    public Result<AdminTenantOverviewDto> overview() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        List<AdminTenantOverviewDto.TenantRow> rows = buildTenantRows();
        int expiringCount = (int) rows.stream()
                .filter(row -> "EXPIRING".equals(row.getExpireStatus()))
                .count();
        int activeStudentCount = rows.stream()
                .mapToInt(row -> row.getActiveStudents() == null ? 0 : row.getActiveStudents())
                .sum();
        BigDecimal estimatedRevenue = BigDecimal.valueOf(rows.stream()
                .filter(row -> row.getStatus() != null && row.getStatus() == Tenant.STATUS_ENABLED)
                .mapToLong(this::estimateTenantContractValue)
                .sum());
        double totalCountGrowthRate = calculateTenantGrowthRate();
        double activeStudentGrowthRate = calculateActiveStudentGrowthRate();

        return Result.ok(new AdminTenantOverviewDto(
                rows.size(),
                activeStudentCount,
                expiringCount,
                estimatedRevenue,
                totalCountGrowthRate,
                activeStudentGrowthRate,
                rows
        ));
    }

    /**
     * 新增租户，id 为手机号（必填）。
     *
     * @param req 请求体：id（手机号）、name、contactName、contactPhone、accountQuota、expireTime
     * @return Result.data 创建的租户
     */
    @PostMapping
    public Result<Tenant> create(@RequestBody TenantCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (req.getId() == null || req.getId().trim().isEmpty()) {
            throw new BusinessException("租户ID(手机号)不能为空");
        }
        if (tenantService.getById(req.getId()) != null) {
            throw new BusinessException("该手机号已作为租户ID存在");
        }
        Tenant tenant = new Tenant();
        tenant.setId(req.getId().trim());
        tenant.setName(req.getName() != null ? req.getName() : "未命名");
        tenant.setContactName(req.getContactName());
        tenant.setContactPhone(req.getContactPhone());
        tenant.setAccountQuota(req.getAccountQuota() != null ? req.getAccountQuota() : 0);
        tenant.setExpireTime(req.getExpireTime() != null ? req.getExpireTime() : java.time.LocalDateTime.now().plusYears(1));
        tenant.setStatus(Tenant.STATUS_ENABLED);
        tenantService.save(tenant);
        userService.createDefaultOrgAdmin(
                tenant.getId(),
                tenant.getId(),
                req.getContactName() != null && !req.getContactName().trim().isEmpty() ? req.getContactName().trim() : "校长"
        );
        return Result.ok(tenant);
    }

    /**
     * 根据 ID 获取租户详情。
     *
     * @param id 租户 ID（手机号）
     * @return Result.data 租户信息，不存在可能为 null
     */
    @GetMapping("/{id}")
    public Result<Tenant> get(@PathVariable String id) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        return Result.ok(tenantService.getById(id));
    }

    /**
     * 获取指定机构的校长账号信息（含账号、密码明文，供超管查看与告知）。
     *
     * @param id 机构 ID（手机号）
     * @return Result.data { username, realName, passwordPlain }，未设置则 null
     */
    @GetMapping("/{id}/org-admin")
    public Result<OrgAdminInfo> getOrgAdmin(@PathVariable String id) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        User u = userService.getOrgAdminByTenantId(id);
        if (u == null) return Result.ok(null);
        OrgAdminInfo info = new OrgAdminInfo();
        info.setUsername(u.getUsername());
        info.setRealName(u.getRealName());
        info.setPasswordPlain(u.getPasswordPlain());
        info.setPhone(u.getPhone());
        return Result.ok(info);
    }

    /**
     * 更新租户：设置 account_quota、expire_time、name 等。
     *
     * @param id  租户 ID（手机号）
     * @param req 请求体：accountQuota/maxStudentCount、expireTime/serviceEndDate、name、contactName、contactPhone、status
     * @return Result 无 data
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody TenantUpdateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) throw new BusinessException("租户不存在");
        if (req.getAccountQuota() != null) tenant.setAccountQuota(req.getAccountQuota());
        else if (req.getMaxStudentCount() != null) tenant.setAccountQuota(req.getMaxStudentCount());
        if (req.getExpireTime() != null) tenant.setExpireTime(req.getExpireTime());
        else if (req.getServiceEndDate() != null) tenant.setExpireTime(req.getServiceEndDate());
        if (req.getName() != null) tenant.setName(req.getName());
        if (req.getContactName() != null) tenant.setContactName(req.getContactName());
        if (req.getContactPhone() != null) tenant.setContactPhone(req.getContactPhone());
        if (req.getStatus() != null) tenant.setStatus(req.getStatus());
        tenantService.updateById(tenant);
        return Result.ok();
    }

    /**
     * 禁用/启用机构：更新 status（0=禁用，1=启用）。禁用后该机构下校长与学生登录将提示「账号到期，续签请联系管理员」。
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) throw new BusinessException("租户不存在");
        tenant.setStatus(status != null && status == Tenant.STATUS_ENABLED ? Tenant.STATUS_ENABLED : Tenant.STATUS_DISABLED);
        tenantService.updateById(tenant);
        return Result.ok();
    }

    /**
     * 软删除机构。该机构及机构下所有账号均做软删除，登录将提示「账号到期，续签请联系管理员」。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) throw new BusinessException("租户不存在");
        userService.softDeleteUsersByTenantId(id);
        tenantBookAuthService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TenantBookAuth>()
                .eq(TenantBookAuth::getTenantId, id));
        tenantService.softDeleteTenant(id);
        return Result.ok();
    }

    private List<AdminTenantOverviewDto.TenantRow> buildTenantRows() {
        List<AdminTenantOverviewDto.TenantRow> rows = new ArrayList<>();
        for (Tenant tenant : tenantService.list().stream()
                .sorted(Comparator.comparing(Tenant::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList())) {
            int quota = tenant.getAccountQuota() == null ? 0 : tenant.getAccountQuota();
            int usedCount = userService.countStudentsByTenantId(tenant.getId());
            int activeStudents = studyLogService.countActiveStudentsToday(tenant.getId());
            int authorizedBookCount = tenantBookAuthService.listBookIdsByTenantId(tenant.getId()).size();
            long daysRemaining = resolveDaysRemaining(tenant);
            User orgAdmin = userService.getOrgAdminByTenantId(tenant.getId());

            AdminTenantOverviewDto.TenantRow row = new AdminTenantOverviewDto.TenantRow();
            row.setId(tenant.getId());
            row.setName(tenant.getName());
            row.setContactName(tenant.getContactName());
            row.setContactPhone(tenant.getContactPhone());
            row.setAccountQuota(quota);
            row.setUsedCount(usedCount);
            row.setActiveStudents(activeStudents);
            row.setAuthorizedBookCount(authorizedBookCount);
            row.setStatus(tenant.getStatus());
            row.setExpireTime(tenant.getExpireTime());
            row.setDaysRemaining(daysRemaining == Long.MAX_VALUE ? null : daysRemaining);
            row.setUsagePercent(quota <= 0 ? 0 : Math.min(100, (int) Math.round(usedCount * 100.0 / quota)));
            row.setPlanName(resolvePlanName(quota));
            row.setExpireStatus(resolveExpireStatus(tenant, daysRemaining));
            if (orgAdmin != null) {
                row.setOrgAdminUsername(orgAdmin.getUsername());
                row.setOrgAdminPhone(orgAdmin.getPhone());
            }
            rows.add(row);
        }
        return rows;
    }

    private TenantRow toLegacyTenantRow(AdminTenantOverviewDto.TenantRow source) {
        TenantRow row = new TenantRow();
        row.setId(source.getId());
        row.setName(source.getName());
        row.setContactName(source.getContactName());
        row.setContactPhone(source.getContactPhone());
        row.setAccountQuota(source.getAccountQuota());
        row.setExpireTime(source.getExpireTime());
        row.setStatus(source.getStatus());
        row.setUsedCount(source.getUsedCount());
        row.setActiveStudents(source.getActiveStudents());
        row.setAuthorizedBookCount(source.getAuthorizedBookCount());
        row.setOrgAdminUsername(source.getOrgAdminUsername());
        row.setOrgAdminPhone(source.getOrgAdminPhone());
        return row;
    }

    private long resolveDaysRemaining(Tenant tenant) {
        if (tenant == null || tenant.getExpireTime() == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(java.time.LocalDateTime.now(), tenant.getExpireTime()).toDays();
    }

    private String resolveExpireStatus(Tenant tenant, long daysRemaining) {
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() != Tenant.STATUS_ENABLED) {
            return "DISABLED";
        }
        if (daysRemaining < 0) return "EXPIRED";
        if (daysRemaining <= 14) return "EXPIRING";
        return "ACTIVE";
    }

    private String resolvePlanName(int quota) {
        if (quota >= 1000) return "旗舰版套餐";
        if (quota >= 200) return "标准版套餐";
        return "基础版套餐";
    }

    private long estimateTenantContractValue(AdminTenantOverviewDto.TenantRow row) {
        int quota = row.getAccountQuota() == null ? 0 : row.getAccountQuota();
        if (quota >= 1000) return 6999L;
        if (quota >= 500) return 4999L;
        if (quota >= 100) return 2999L;
        return 1999L;
    }

    private double calculateTenantGrowthRate() {
        LocalDateTime currentMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        long currentCount = tenantService.lambdaQuery()
                .ge(Tenant::getCreatedAt, currentMonthStart)
                .count();
        long previousCount = tenantService.lambdaQuery()
                .ge(Tenant::getCreatedAt, previousMonthStart)
                .lt(Tenant::getCreatedAt, currentMonthStart)
                .count();
        return calculateGrowthRate(currentCount, previousCount);
    }

    private double calculateActiveStudentGrowthRate() {
        LocalDate today = LocalDate.now();
        long currentCount = countDistinctActiveStudents(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
        long previousCount = countDistinctActiveStudents(today.minusDays(13).atStartOfDay(), today.minusDays(6).atStartOfDay());
        return calculateGrowthRate(currentCount, previousCount);
    }

    private long countDistinctActiveStudents(LocalDateTime start, LocalDateTime endExclusive) {
        return studyLogService.lambdaQuery()
                .ge(StudyLog::getCreatedAt, start)
                .lt(StudyLog::getCreatedAt, endExclusive)
                .list().stream()
                .map(StudyLog::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    private double calculateGrowthRate(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? 100D : 0D;
        }
        return Math.round(((current - previous) * 10000D) / previous) / 100D;
    }

    /**
     * 为指定租户新增校长账号（ORG_ADMIN）。租户的 account_quota、expire_time 由 PUT /admin/tenants/{id} 维护。
     *
     * @param tenantId 租户 ID（手机号）
     * @param req      请求体：username、password、realName
     * @return Result.data 创建的用户
     */
    @PostMapping("/{tenantId}/org-admin")
    public Result<User> createOrgAdmin(@PathVariable String tenantId, @jakarta.validation.Valid @RequestBody CreateOrgAdminRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Tenant t = tenantService.getById(tenantId);
        if (t == null) throw new BusinessException("租户不存在");
        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        if (username.isEmpty()) throw new BusinessException("校长账号用户名不能为空");
        if (userService.getByTenantAndUsername(tenantId, username) != null) {
            throw new BusinessException("该机构下校长账号用户名已存在，请使用其他用户名");
        }
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPasswordPlain(req.getPassword());
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setRole(User.ROLE_ORG_ADMIN);
        user.setStatus(User.STATUS_ENABLED);
        userService.save(user);
        return Result.ok(user);
    }

    @PutMapping("/{tenantId}/org-admin")
    public Result<OrgAdminInfo> updateOrgAdmin(@PathVariable String tenantId, @RequestBody UpdateOrgAdminRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Tenant t = tenantService.getById(tenantId);
        if (t == null) throw new BusinessException("租户不存在");
        User user = userService.updateOrgAdminByTenant(tenantId, req.getUsername(), req.getPassword(), req.getRealName(), req.getPhone());
        OrgAdminInfo info = new OrgAdminInfo();
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setPasswordPlain(user.getPasswordPlain());
        info.setPhone(user.getPhone());
        return Result.ok(info);
    }

    @lombok.Data
    public static class CreateOrgAdminRequest {
        @jakarta.validation.constraints.NotBlank private String username;
        @jakarta.validation.constraints.NotBlank private String password;
        private String realName;
        private String phone;
    }

    @lombok.Data
    public static class UpdateOrgAdminRequest {
        private String username;
        private String password;
        private String realName;
        private String phone;
    }

    @lombok.Data
    public static class OrgAdminInfo {
        private String username;
        private String realName;
        private String passwordPlain;
        private String phone;
    }

    @lombok.Data
    public static class TenantCreateRequest {
        /** 租户ID = 手机号（必填） */
        private String id;
        private String name;
        private String contactName;
        private String contactPhone;
        private Integer accountQuota;
        private java.time.LocalDateTime expireTime;
    }

    @lombok.Data
    public static class TenantRow {
        private String id;
        private String name;
        private String contactName;
        private String contactPhone;
        private Integer accountQuota;
        private java.time.LocalDateTime expireTime;
        private Integer status;
        private Integer usedCount;
        private Integer activeStudents;
        private Integer authorizedBookCount;
        private String orgAdminUsername;
        private String orgAdminPhone;
    }
}

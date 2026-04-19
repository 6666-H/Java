package com.study.english.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.AdminAuthorizationOverviewDto;
import com.study.english.dto.TenantBookAuthUpdateRequest;
import com.study.english.entity.Book;
import com.study.english.entity.Tenant;
import com.study.english.entity.TenantBookAuth;
import com.study.english.exception.BusinessException;
import com.study.english.service.BookService;
import com.study.english.service.TenantBookAuthService;
import com.study.english.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 超级管理员：租户书本授权（tenant_book_auth），将指定 book_id 授权给指定 tenant_id。
 */
@RestController
@RequestMapping("/api/admin/tenant-book-auth")
@RequiredArgsConstructor
public class AdminTenantBookAuthController {

    private final TenantBookAuthService tenantBookAuthService;
    private final TenantService tenantService;
    private final BookService bookService;

    /**
     * 将书本授权给指定租户。
     *
     * @param tenantId 租户 ID（手机号）
     * @param bookId   书本 ID
     * @return Result 无 data
     */
    @PostMapping
    public Result<Void> grant(@RequestParam String tenantId, @RequestParam Long bookId) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        tenantBookAuthService.grantBookToTenant(tenantId, bookId);
        return Result.ok();
    }

    /**
     * 查询指定租户已授权的书本 ID 列表。
     *
     * @param tenantId 租户 ID（手机号）
     * @return Result.data 书本 ID 列表
     */
    @GetMapping("/list")
    public Result<List<Long>> listByTenant(@RequestParam String tenantId) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可查询");
        }
        return Result.ok(tenantBookAuthService.listBookIdsByTenantId(tenantId));
    }

    @GetMapping("/overview")
    public Result<AdminAuthorizationOverviewDto> overview(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可查询");
        }

        long safePage = Math.max(1, page);
        long safePageSize = Math.min(50, Math.max(10, pageSize));

        List<Tenant> tenants = tenantService.list();
        Map<String, Tenant> tenantMap = tenants.stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity()));
        Map<Long, Book> bookMap = bookService.listBooksByTenantId(null).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        String normalizedStatus = status == null ? "ALL" : status.trim().toUpperCase();
        Set<String> matchedTenantIds = tenants.stream()
                .filter(tenant -> matchesStatus(resolveStatus(tenant), normalizedStatus))
                .map(Tenant::getId)
                .collect(Collectors.toSet());

        long totalCount = tenantBookAuthService.count();
        long activeCount = countByStatus(tenants, "ACTIVE");
        long expiringCount = countByStatus(tenants, "EXPIRING");
        double growthRate = calculateAuthorizationGrowthRate();

        if (!"ALL".equals(normalizedStatus) && matchedTenantIds.isEmpty()) {
            return Result.ok(new AdminAuthorizationOverviewDto(
                    (int) totalCount,
                    (int) activeCount,
                    (int) expiringCount,
                    growthRate,
                    0,
                    safePage,
                    safePageSize,
                    List.of()
            ));
        }

        LambdaQueryWrapper<TenantBookAuth> countQuery = buildOverviewQuery(tenantId, bookId, normalizedStatus, matchedTenantIds);
        long filteredCount = tenantBookAuthService.count(countQuery);
        if (filteredCount == 0) {
            return Result.ok(new AdminAuthorizationOverviewDto(
                    (int) totalCount,
                    (int) activeCount,
                    (int) expiringCount,
                    growthRate,
                    0,
                    safePage,
                    safePageSize,
                    List.of()
            ));
        }

        Page<TenantBookAuth> authPage = tenantBookAuthService.page(
                new Page<>(safePage, safePageSize),
                buildOverviewQuery(tenantId, bookId, normalizedStatus, matchedTenantIds)
                        .orderByDesc(TenantBookAuth::getCreatedAt)
        );

        List<AdminAuthorizationOverviewDto.Record> records = authPage.getRecords().stream()
                .map(auth -> toRecord(auth, tenantMap.get(auth.getTenantId()), bookMap.get(auth.getBookId())))
                .filter(record -> record.getTenantId() != null && record.getBookId() != null)
                .toList();

        return Result.ok(new AdminAuthorizationOverviewDto(
                (int) totalCount,
                (int) activeCount,
                (int) expiringCount,
                growthRate,
                filteredCount,
                safePage,
                safePageSize,
                records
        ));
    }

    /**
     * 撤销指定租户的书本授权。
     */
    @DeleteMapping
    public Result<Void> revoke(@RequestParam String tenantId, @RequestParam Long bookId) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        tenantBookAuthService.revokeBookFromTenant(tenantId, bookId);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> replace(@RequestBody TenantBookAuthUpdateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (req == null || req.getTenantId() == null || req.getTenantId().isBlank()) {
            throw new BusinessException("租户ID不能为空");
        }
        tenantBookAuthService.replaceBooksForTenant(req.getTenantId().trim(), req.getBookIds());
        return Result.ok();
    }

    private AdminAuthorizationOverviewDto.Record toRecord(TenantBookAuth auth, Tenant tenant, Book book) {
        if (auth == null || tenant == null || book == null) {
            return new AdminAuthorizationOverviewDto.Record();
        }
        String status = resolveStatus(tenant);
        long daysRemaining = resolveDaysRemaining(tenant);
        String label = (book.getGrade() != null && !book.getGrade().isBlank() ? book.getGrade() : "教材") +
                (book.getVersionName() != null && !book.getVersionName().isBlank() ? " · " + book.getVersionName() : "");
        return new AdminAuthorizationOverviewDto.Record(
                tenant.getId() + "-" + book.getId(),
                tenant.getId(),
                tenant.getName(),
                book.getId(),
                book.getName(),
                label,
                "年度授权",
                status,
                auth.getCreatedAt(),
                tenant.getExpireTime(),
                daysRemaining
        );
    }

    private String resolveStatus(Tenant tenant) {
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() != Tenant.STATUS_ENABLED) {
            return "DISABLED";
        }
        long daysRemaining = resolveDaysRemaining(tenant);
        if (daysRemaining < 0) return "EXPIRED";
        if (daysRemaining <= 30) return "EXPIRING";
        return "ACTIVE";
    }

    private long resolveDaysRemaining(Tenant tenant) {
        if (tenant == null || tenant.getExpireTime() == null) return Long.MAX_VALUE;
        return java.time.Duration.between(LocalDateTime.now(), tenant.getExpireTime()).toDays();
    }

    private boolean matchesStatus(String recordStatus, String filterStatus) {
        return filterStatus == null || filterStatus.isBlank() || "ALL".equals(filterStatus) || filterStatus.equals(recordStatus);
    }

    private long countByStatus(List<Tenant> tenants, String status) {
        Set<String> tenantIds = tenants.stream()
                .filter(tenant -> status.equals(resolveStatus(tenant)))
                .map(Tenant::getId)
                .collect(Collectors.toSet());
        if (tenantIds.isEmpty()) {
            return 0;
        }
        return tenantBookAuthService.count(new LambdaQueryWrapper<TenantBookAuth>()
                .in(TenantBookAuth::getTenantId, tenantIds));
    }

    private LambdaQueryWrapper<TenantBookAuth> buildOverviewQuery(
            String tenantId,
            Long bookId,
            String status,
            Set<String> matchedTenantIds) {
        LambdaQueryWrapper<TenantBookAuth> queryWrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && !tenantId.isBlank()) {
            queryWrapper.eq(TenantBookAuth::getTenantId, tenantId.trim());
        }
        if (bookId != null) {
            queryWrapper.eq(TenantBookAuth::getBookId, bookId);
        }
        if (!"ALL".equals(status)) {
            queryWrapper.in(TenantBookAuth::getTenantId, matchedTenantIds);
        }
        return queryWrapper;
    }

    private double calculateAuthorizationGrowthRate() {
        LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        long currentCount = tenantBookAuthService.lambdaQuery()
                .ge(TenantBookAuth::getCreatedAt, currentMonthStart)
                .count();
        long previousCount = tenantBookAuthService.lambdaQuery()
                .ge(TenantBookAuth::getCreatedAt, previousMonthStart)
                .lt(TenantBookAuth::getCreatedAt, currentMonthStart)
                .count();
        return calculateGrowthRate(currentCount, previousCount);
    }

    private double calculateGrowthRate(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? 100D : 0D;
        }
        return Math.round(((current - previous) * 10000D) / previous) / 100D;
    }
}

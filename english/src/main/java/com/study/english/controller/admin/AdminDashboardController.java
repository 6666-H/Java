package com.study.english.controller.admin;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.AdminDashboardDto;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.StudyLog;
import com.study.english.entity.Tenant;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final TenantService tenantService;
    private final StudyLogService studyLogService;
    private final StudentWordProgressService studentWordProgressService;

    @GetMapping("/dashboard")
    public Result<AdminDashboardDto> dashboard() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可查看");
        }

        List<Tenant> tenants = tenantService.list();
        int totalTenants = tenants.size();
        int enabledTenants = (int) tenants.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == Tenant.STATUS_ENABLED)
                .count();

        LocalDate today = LocalDate.now();
        LocalDateTime start7Days = today.minusDays(6).atStartOfDay();

        List<StudyLog> recentLogs = studyLogService.lambdaQuery()
                .ge(StudyLog::getCreatedAt, start7Days)
                .list();
        Map<String, Set<Long>> tenantActiveMap = new HashMap<>();
        for (StudyLog log : recentLogs) {
            if (log.getTenantId() == null || log.getUserId() == null) continue;
            tenantActiveMap.computeIfAbsent(log.getTenantId(), key -> new java.util.HashSet<>()).add(log.getUserId());
        }

        int activeTenants = tenantActiveMap.size();
        int todayActiveStudents = (int) studyLogService.lambdaQuery()
                .ge(StudyLog::getCreatedAt, today.atStartOfDay())
                .list().stream()
                .map(StudyLog::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet())
                .size();

        int totalMasteredWords = studentWordProgressService.lambdaQuery()
                .eq(StudentWordProgress::getStatus, StudentWordProgress.STATUS_MASTERED)
                .count().intValue();

        Map<String, Tenant> tenantMap = tenants.stream().collect(Collectors.toMap(Tenant::getId, item -> item));
        List<AdminDashboardDto.TenantActiveRankDto> tenantRanks = tenantActiveMap.entrySet().stream()
                .map(entry -> new AdminDashboardDto.TenantActiveRankDto(
                        entry.getKey(),
                        tenantMap.containsKey(entry.getKey()) ? tenantMap.get(entry.getKey()).getName() : entry.getKey(),
                        entry.getValue().size()))
                .sorted(Comparator.comparingInt(AdminDashboardDto.TenantActiveRankDto::getActiveStudents).reversed())
                .limit(8)
                .toList();

        List<String> trendLabels = new ArrayList<>();
        List<Integer> trendValues = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            trendLabels.add(day.toString());
            int count = (int) studyLogService.lambdaQuery()
                    .ge(StudyLog::getCreatedAt, day.atStartOfDay())
                    .lt(StudyLog::getCreatedAt, day.plusDays(1).atStartOfDay())
                    .list().stream()
                    .map(StudyLog::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet())
                    .size();
            trendValues.add(count);
        }

        return Result.ok(new AdminDashboardDto(
                totalTenants,
                Math.min(activeTenants, enabledTenants),
                todayActiveStudents,
                totalMasteredWords,
                trendLabels,
                trendValues,
                tenantRanks
        ));
    }
}

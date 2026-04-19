package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.TenantInfoDto;
import com.study.english.dto.TenantQuotaDto;
import com.study.english.dto.TenantStatsDto;
import com.study.english.entity.Tenant;
import com.study.english.entity.StudyLog;
import com.study.english.entity.StudyTask;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.StudyTaskService;
import com.study.english.service.TenantService;
import com.study.english.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 租户管理员：名额实时展示、有效期自查（剩余 7 天显著提示）。
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantDashboardController {

    private final TenantService tenantService;
    private final UserService userService;
    private final StudyLogService studyLogService;
    private final StudentWordProgressService studentWordProgressService;
    private final StudyTaskService studyTaskService;

    /**
     * 当前租户名额：已用/总名额。名额满时前端可据此禁用创建学生按钮。
     *
     * @return Result.data { usedCount, totalQuota }，含 isFull() 方法
     */
    @GetMapping("/quota")
    public Result<TenantQuotaDto> quota() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) throw new BusinessException("租户不存在");
        int used = userService.countStudentsByTenantId(tenantId);
        int total = tenant.getAccountQuota() != null ? tenant.getAccountQuota() : 0;
        return Result.ok(new TenantQuotaDto(used, total));
    }

    /**
     * 当前租户信息：含到期日、剩余天数、是否即将到期。用于后台显著提示服务即将到期。
     *
     * @return Result.data { id, name, expireTime, daysRemaining, expiringSoon, status }
     */
    @GetMapping("/info")
    public Result<TenantInfoDto> info() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) throw new BusinessException("租户不存在");
        LocalDateTime expire = tenant.getExpireTime();
        long daysRemaining = expire != null ? ChronoUnit.DAYS.between(LocalDateTime.now(), expire) : 0;
        boolean expiringSoon = daysRemaining >= 0 && daysRemaining <= 7;
        TenantInfoDto dto = new TenantInfoDto(
                tenant.getId(),
                tenant.getName(),
                expire,
                daysRemaining,
                expiringSoon,
                tenant.getStatus()
        );
        return Result.ok(dto);
    }

    /**
     * 数据统计看板：学生总数、今日活跃数、累计学习单词、近 7 天活跃趋势等。
     *
     * @return Result.data { totalStudents, activeToday, totalWordsLearned, activeTrend }
     */
    @GetMapping("/stats")
    public Result<TenantStatsDto> stats() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        int totalStudents = userService.countStudentsByTenantId(tenantId);
        int activeToday = studyLogService.countActiveStudentsToday(tenantId);
        int totalWordsLearned = studentWordProgressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .ge(StudentWordProgress::getRepetitions, 2)
                .count()
                .intValue();
        int inactiveStudents = userService.listInactiveStudents(tenantId, 7).size();
        double studentGrowthRate = calculateStudentGrowthRate(tenantId);
        double activeStudentDelta = calculateActiveStudentDelta(tenantId);
        List<Map<String, Object>> taskRows = studyTaskService.listTasksForTenant(tenantId);
        double taskCompletionRate = calculateTaskCompletionRate(taskRows);
        double taskCompletionRateDelta = calculateTaskCompletionRateDelta(tenantId);
        long totalLearningMinutes = calculateTotalLearningMinutes(tenantId);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(13);
        List<Map<String, Object>> rawTrend = studyLogService.getActiveTrend(tenantId, startDate, today);
        Map<String, Integer> trendMap = new HashMap<>();
        for (Map<String, Object> row : rawTrend) {
            if (row == null || row.get("day") == null) continue;
            trendMap.put(String.valueOf(row.get("day")), ((Number) row.get("activeCount")).intValue());
        }
        List<String> trendLabels = new ArrayList<>();
        List<Integer> activeTrend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            activeTrend.add(trendMap.getOrDefault(day.toString(), 0));
        }
        List<Integer> trendValues = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate day = startDate.plusDays(i);
            trendLabels.add(day.toString());
            trendValues.add(trendMap.getOrDefault(day.toString(), 0));
        }
        List<TenantStatsDto.ActivityItemDto> recentActivities = buildRecentActivities(tenantId, taskRows);

        return Result.ok(new TenantStatsDto(
                totalStudents,
                activeToday,
                totalWordsLearned,
                activeTrend,
                inactiveStudents,
                studentGrowthRate,
                activeStudentDelta,
                taskCompletionRate,
                taskCompletionRateDelta,
                totalLearningMinutes,
                trendLabels,
                trendValues,
                recentActivities
        ));
    }

    private double calculateStudentGrowthRate(String tenantId) {
        LocalDateTime currentMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        long currentCount = userService.lambdaQuery()
                .eq(User::getTenantId, tenantId)
                .eq(User::getRole, User.ROLE_STUDENT)
                .ge(User::getCreatedAt, currentMonthStart)
                .count();
        long previousCount = userService.lambdaQuery()
                .eq(User::getTenantId, tenantId)
                .eq(User::getRole, User.ROLE_STUDENT)
                .ge(User::getCreatedAt, previousMonthStart)
                .lt(User::getCreatedAt, currentMonthStart)
                .count();
        return calculateGrowthRate(currentCount, previousCount);
    }

    private double calculateActiveStudentDelta(String tenantId) {
        LocalDate today = LocalDate.now();
        long currentCount = countDistinctActiveStudents(tenantId, today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
        long previousCount = countDistinctActiveStudents(tenantId, today.minusDays(13).atStartOfDay(), today.minusDays(6).atStartOfDay());
        return calculateGrowthRate(currentCount, previousCount);
    }

    private long countDistinctActiveStudents(String tenantId, LocalDateTime start, LocalDateTime endExclusive) {
        return studyLogService.lambdaQuery()
                .eq(StudyLog::getTenantId, tenantId)
                .ge(StudyLog::getCreatedAt, start)
                .lt(StudyLog::getCreatedAt, endExclusive)
                .list().stream()
                .map(StudyLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private double calculateTaskCompletionRate(List<Map<String, Object>> taskRows) {
        if (taskRows == null || taskRows.isEmpty()) {
            return 0D;
        }
        long completed = taskRows.stream()
                .filter(item -> "已完成".equals(item.get("status")))
                .count();
        return round2(completed * 100D / taskRows.size());
    }

    private double calculateTaskCompletionRateDelta(String tenantId) {
        LocalDateTime currentWindowStart = LocalDate.now().minusDays(13).atStartOfDay();
        LocalDateTime previousWindowStart = currentWindowStart.minusDays(14);
        LocalDateTime currentWindowMid = LocalDate.now().minusDays(6).atStartOfDay();
        double currentRate = calculateTaskCompletionRateForWindow(tenantId, currentWindowMid, LocalDate.now().plusDays(1).atStartOfDay());
        double previousRate = calculateTaskCompletionRateForWindow(tenantId, previousWindowStart, currentWindowMid);
        if (previousRate <= 0D) {
            return currentRate > 0D ? 100D : 0D;
        }
        return round2(((currentRate - previousRate) * 100D) / previousRate);
    }

    private double calculateTaskCompletionRateForWindow(String tenantId, LocalDateTime start, LocalDateTime endExclusive) {
        List<StudyTask> tasks = studyTaskService.lambdaQuery()
                .eq(StudyTask::getTenantId, tenantId)
                .ge(StudyTask::getCreatedAt, start)
                .lt(StudyTask::getCreatedAt, endExclusive)
                .list();
        if (tasks.isEmpty()) {
            return 0D;
        }
        long completed = tasks.stream()
                .filter(task -> StudyTask.STATUS_ACTIVE.equalsIgnoreCase(task.getStatus()))
                .count();
        return round2(completed * 100D / tasks.size());
    }

    private long calculateTotalLearningMinutes(String tenantId) {
        long totalLogs = studyLogService.lambdaQuery()
                .eq(StudyLog::getTenantId, tenantId)
                .count();
        return Math.max(0, totalLogs);
    }

    private List<TenantStatsDto.ActivityItemDto> buildRecentActivities(String tenantId, List<Map<String, Object>> taskRows) {
        List<TenantStatsDto.ActivityItemDto> items = new ArrayList<>();
        if (taskRows != null) {
            items.addAll(taskRows.stream()
                    .limit(4)
                    .map(item -> new TenantStatsDto.ActivityItemDto(
                            "task-" + String.valueOf(item.get("id")),
                            String.format("%s%s%s",
                                    String.valueOf(item.getOrDefault("studentName", "学生")),
                                    "已完成".equals(item.get("status")) ? " 完成了 " : " 更新了 ",
                                    String.valueOf(item.getOrDefault("unitName", "学习任务"))),
                            formatRelativeTime(item.get("assignedAt")),
                            initials(item.get("studentName")),
                            "已完成".equals(item.get("status")) ? "green" : "orange",
                            toDateTime(item.get("assignedAt"))
                    ))
                    .toList());
        }
        if (items.size() < 4) {
            List<User> students = userService.listByTenantId(tenantId, User.ROLE_STUDENT).stream()
                    .filter(item -> item.getLastActiveAt() != null)
                    .sorted(Comparator.comparing(User::getLastActiveAt, Comparator.reverseOrder()))
                    .limit(4)
                    .toList();
            for (User student : students) {
                if (items.stream().anyMatch(item -> ("student-" + student.getId()).equals(item.getKey()))) {
                    continue;
                }
                items.add(new TenantStatsDto.ActivityItemDto(
                        "student-" + student.getId(),
                        String.format("%s 完成了今日学习", student.getRealName() != null ? student.getRealName() : student.getUsername()),
                        formatRelativeTime(student.getLastActiveAt()),
                        initials(student.getRealName() != null ? student.getRealName() : student.getUsername()),
                        "blue",
                        student.getLastActiveAt()
                ));
                if (items.size() >= 4) {
                    break;
                }
            }
        }
        items.sort(Comparator.comparing(TenantStatsDto.ActivityItemDto::getSortTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return items.stream().limit(4).toList();
    }

    private String initials(Object value) {
        String text = String.valueOf(value == null ? "学" : value).trim();
        return text.length() <= 2 ? text : text.substring(0, 2);
    }

    private String formatRelativeTime(Object value) {
        LocalDateTime time = toDateTime(value);
        if (time == null) {
            return "刚刚";
        }
        long minutes = Math.max(1, ChronoUnit.MINUTES.between(time, LocalDateTime.now()));
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        long hours = Math.max(1, ChronoUnit.HOURS.between(time, LocalDateTime.now()));
        if (hours < 24) {
            return hours + " 小时前";
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(time, LocalDateTime.now()));
        return days + " 天前";
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return null;
    }

    private double calculateGrowthRate(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? 100D : 0D;
        }
        return round2(((current - previous) * 100D) / previous);
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }
}

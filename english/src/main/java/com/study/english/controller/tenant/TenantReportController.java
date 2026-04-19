package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.TenantReportRowDto;
import com.study.english.dto.TenantReportSummaryDto;
import com.study.english.entity.Book;
import com.study.english.entity.ErrorLog;
import com.study.english.entity.StudentUnitMode;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.StudyLog;
import com.study.english.entity.StudyStageResult;
import com.study.english.entity.Unit;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.StudentUnitModeMapper;
import com.study.english.service.BookService;
import com.study.english.service.ErrorLogService;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.StudyStageResultService;
import com.study.english.service.UnitService;
import com.study.english.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenant/report")
@RequiredArgsConstructor
public class TenantReportController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserService userService;
    private final StudyLogService studyLogService;
    private final ErrorLogService errorLogService;
    private final StudentWordProgressService studentWordProgressService;
    private final StudyStageResultService studyStageResultService;
    private final StudentUnitModeMapper studentUnitModeMapper;
    private final BookService bookService;
    private final UnitService unitService;

    @GetMapping
    public Result<List<TenantReportRowDto>> list(
            @RequestParam(required = false) String gradeClass,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        return Result.ok(buildReportRows(tenantId, gradeClass, keyword, startDate, endDate));
    }

    @GetMapping("/summary")
    public Result<TenantReportSummaryDto> summary(
            @RequestParam(required = false) String gradeClass,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");

        LocalDate safeEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate safeStartDate = startDate != null ? startDate : safeEndDate.minusDays(29);
        List<TenantReportRowDto> rows = buildReportRows(tenantId, gradeClass, keyword, safeStartDate, safeEndDate);
        List<TenantReportRowDto> previousRows = buildReportRows(
                tenantId,
                gradeClass,
                keyword,
                safeStartDate.minusDays(ChronoUnit.DAYS.between(safeStartDate, safeEndDate) + 1),
                safeStartDate.minusDays(1)
        );

        long averageMastered = rows.isEmpty() ? 0 : Math.round(rows.stream().mapToInt(TenantReportRowDto::getMasteredCount).average().orElse(0));
        double averageAccuracy = rows.isEmpty() ? 0D : round2(rows.stream().mapToDouble(this::rowAccuracy).average().orElse(0));
        double averageDurationMinutes = calculateAverageDurationMinutes(tenantId, rows, safeStartDate, safeEndDate);
        int activeStudentCount = (int) rows.stream().filter(row -> row.getLastStudyAt() != null && !row.getLastStudyAt().isBlank()).count();
        double averageProgress = rows.isEmpty() ? 0D : round2(rows.stream().mapToDouble(this::rowProgress).average().orElse(0));

        double previousAverageMastered = previousRows.isEmpty() ? 0D : previousRows.stream().mapToInt(TenantReportRowDto::getMasteredCount).average().orElse(0);
        double previousAverageAccuracy = previousRows.isEmpty() ? 0D : previousRows.stream().mapToDouble(this::rowAccuracy).average().orElse(0);
        int previousActiveStudentCount = (int) previousRows.stream().filter(row -> row.getLastStudyAt() != null && !row.getLastStudyAt().isBlank()).count();

        List<String> chartLabels = new ArrayList<>();
        List<Integer> chartMasteredValues = new ArrayList<>();
        List<Integer> chartDurationValues = new ArrayList<>();
        buildChartSeries(tenantId, safeEndDate.minusDays(6), safeEndDate, chartLabels, chartMasteredValues, chartDurationValues);

        String focusTopic = resolveFocusTopic(rows);
        String suggestion = resolveSuggestion(rows, focusTopic);

        return Result.ok(new TenantReportSummaryDto(
                rows.size(),
                calculateGrowthRate(rows.size(), previousRows.size()),
                averageProgress,
                resolveProgressLabel(averageProgress),
                averageMastered,
                calculateGrowthRate(averageMastered, previousAverageMastered),
                averageAccuracy,
                calculateGrowthRate(averageAccuracy, previousAverageAccuracy),
                round2(averageDurationMinutes),
                round2(averageDurationMinutes) + " min/d",
                activeStudentCount,
                calculateGrowthRate(activeStudentCount, previousActiveStudentCount),
                chartLabels,
                chartMasteredValues,
                chartDurationValues,
                buildInsightSummary(rows, focusTopic),
                focusTopic,
                suggestion
        ));
    }

    private List<TenantReportRowDto> buildReportRows(
            String tenantId,
            String gradeClass,
            String keyword,
            LocalDate startDate,
            LocalDate endDate) {
        List<User> students = userService.listByTenantId(tenantId, User.ROLE_STUDENT);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        students = students.stream()
                .filter(item -> gradeClass == null || gradeClass.isBlank() || gradeClass.equals(item.getGradeClass()))
                .filter(item -> safeKeyword.isBlank()
                        || containsText(item.getRealName(), safeKeyword)
                        || containsText(item.getUsername(), safeKeyword)
                        || containsText(item.getStudentNo(), safeKeyword))
                .toList();
        if (students.isEmpty()) return List.of();

        Set<Long> studentIds = students.stream().map(User::getId).collect(Collectors.toSet());
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        List<StudyLog> studyLogs = studyLogService.lambdaQuery()
                .eq(StudyLog::getTenantId, tenantId)
                .in(StudyLog::getUserId, studentIds)
                .ge(start != null, StudyLog::getCreatedAt, start)
                .le(end != null, StudyLog::getCreatedAt, end)
                .list();
        List<ErrorLog> errorLogs = errorLogService.lambdaQuery()
                .eq(ErrorLog::getTenantId, tenantId)
                .in(ErrorLog::getUserId, studentIds)
                .ge(start != null, ErrorLog::getCreatedAt, start)
                .le(end != null, ErrorLog::getCreatedAt, end)
                .list();
        List<StudentWordProgress> progressList = studentWordProgressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .in(StudentWordProgress::getUserId, studentIds)
                .eq(StudentWordProgress::getStatus, StudentWordProgress.STATUS_MASTERED)
                .ge(start != null, StudentWordProgress::getUpdatedAt, start)
                .le(end != null, StudentWordProgress::getUpdatedAt, end)
                .list();
        List<StudentUnitMode> unitModes = studentUnitModeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentUnitMode>()
                        .eq(StudentUnitMode::getTenantId, tenantId)
                        .in(StudentUnitMode::getUserId, studentIds)
                        .ge(start != null, StudentUnitMode::getCreatedAt, start)
                        .le(end != null, StudentUnitMode::getCreatedAt, end)
        );

        int totalUnits = resolveTenantUnitCount(tenantId);
        Map<Long, List<StudyLog>> studyLogMap = studyLogs.stream().collect(Collectors.groupingBy(StudyLog::getUserId));
        Map<Long, Long> errorCountMap = errorLogs.stream().collect(Collectors.groupingBy(ErrorLog::getUserId, Collectors.counting()));
        Map<Long, Long> masteredCountMap = progressList.stream().collect(Collectors.groupingBy(StudentWordProgress::getUserId, Collectors.counting()));
        Map<Long, List<StudentUnitMode>> modeMap = unitModes.stream().collect(Collectors.groupingBy(StudentUnitMode::getUserId));

        List<TenantReportRowDto> rows = new ArrayList<>();
        for (User student : students) {
            List<StudyLog> userLogs = studyLogMap.getOrDefault(student.getId(), List.of());
            Set<LocalDate> learningDays = userLogs.stream()
                    .map(StudyLog::getCreatedAt)
                    .filter(java.util.Objects::nonNull)
                    .map(LocalDateTime::toLocalDate)
                    .collect(Collectors.toCollection(HashSet::new));
            LocalDateTime lastStudyAt = userLogs.stream()
                    .map(StudyLog::getCreatedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(student.getLastActiveAt());

            TenantReportRowDto row = new TenantReportRowDto();
            row.setStudentId(student.getId());
            row.setRealName(student.getRealName());
            row.setUsername(student.getUsername());
            row.setStudentNo(student.getStudentNo());
            row.setGradeClass(student.getGradeClass());
            row.setLearningDays(learningDays.size());
            row.setStreakDays(calculateStreak(learningDays));
            row.setMasteredCount(masteredCountMap.getOrDefault(student.getId(), 0L).intValue());
            row.setTotalErrors(errorCountMap.getOrDefault(student.getId(), 0L).intValue());
            row.setFlashcardRate(calculateModeRate(modeMap.get(student.getId()), StudentUnitMode.MODE_FLASHCARD, totalUnits));
            row.setEngChRate(calculateModeRate(modeMap.get(student.getId()), StudentUnitMode.MODE_ENG_CH, totalUnits));
            row.setChEngRate(calculateModeRate(modeMap.get(student.getId()), StudentUnitMode.MODE_CH_ENG, totalUnits));
            row.setSpellRate(calculateModeRate(modeMap.get(student.getId()), StudentUnitMode.MODE_SPELL, totalUnits));
            row.setLastStudyAt(lastStudyAt != null ? lastStudyAt.format(DATE_TIME_FORMATTER) : "");
            rows.add(row);
        }

        rows.sort(Comparator.comparingInt(TenantReportRowDto::getMasteredCount).reversed()
                .thenComparing(TenantReportRowDto::getLastStudyAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    private boolean containsText(String raw, String keyword) {
        return raw != null && raw.toLowerCase().contains(keyword);
    }

    private int resolveTenantUnitCount(String tenantId) {
        List<Book> books = bookService.listBooksByTenantId(tenantId);
        if (books.isEmpty()) return 0;
        Set<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toSet());
        return unitService.lambdaQuery().in(Unit::getBookId, bookIds).count().intValue();
    }

    private int calculateStreak(Set<LocalDate> learningDays) {
        if (learningDays.isEmpty()) return 0;
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        while (learningDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private double calculateModeRate(List<StudentUnitMode> modes, String mode, int totalUnits) {
        if (modes == null || modes.isEmpty() || totalUnits <= 0) return 0;
        long completedUnits = modes.stream()
                .filter(item -> mode.equalsIgnoreCase(item.getMode()))
                .map(StudentUnitMode::getUnitId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        return Math.round((completedUnits * 10000.0) / totalUnits) / 100.0;
    }

    private double rowAccuracy(TenantReportRowDto row) {
        return round2((row.getFlashcardRate() + row.getEngChRate() + row.getChEngRate() + row.getSpellRate()) / 4D);
    }

    private double rowProgress(TenantReportRowDto row) {
        return rowAccuracy(row);
    }

    private double calculateAverageDurationMinutes(String tenantId, List<TenantReportRowDto> rows, LocalDate startDate, LocalDate endDate) {
        if (rows.isEmpty()) {
            return 0D;
        }
        Set<String> usernames = rows.stream()
                .map(TenantReportRowDto::getUsername)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
        if (usernames.isEmpty()) {
            return 0D;
        }
        Map<String, Long> userIdMap = userService.listByTenantId(tenantId, User.ROLE_STUDENT).stream()
                .filter(user -> usernames.contains(user.getUsername()))
                .collect(Collectors.toMap(User::getUsername, User::getId, (left, right) -> left));
        if (userIdMap.isEmpty()) {
            return 0D;
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        List<StudyStageResult> results = studyStageResultService.lambdaQuery()
                .eq(StudyStageResult::getTenantId, tenantId)
                .in(StudyStageResult::getUserId, userIdMap.values())
                .ge(StudyStageResult::getCreatedAt, start)
                .le(StudyStageResult::getCreatedAt, end)
                .list();
        if (results.isEmpty()) {
            return 0D;
        }
        double totalMinutes = results.stream()
                .mapToInt(item -> item.getDurationSeconds() == null ? 0 : item.getDurationSeconds())
                .sum() / 60D;
        return totalMinutes / rows.size();
    }

    private void buildChartSeries(
            String tenantId,
            LocalDate startDate,
            LocalDate endDate,
            List<String> labels,
            List<Integer> masteredValues,
            List<Integer> durationValues) {
        Map<LocalDate, Integer> masteredMap = new HashMap<>();
        Map<LocalDate, Integer> durationMap = new HashMap<>();
        List<StudyStageResult> results = studyStageResultService.lambdaQuery()
                .eq(StudyStageResult::getTenantId, tenantId)
                .ge(StudyStageResult::getCreatedAt, startDate.atStartOfDay())
                .le(StudyStageResult::getCreatedAt, endDate.atTime(LocalTime.MAX))
                .list();
        for (StudyStageResult result : results) {
            if (result.getCreatedAt() == null) continue;
            LocalDate day = result.getCreatedAt().toLocalDate();
            masteredMap.merge(day, result.getStabilizedCount() == null ? 0 : result.getStabilizedCount(), Integer::sum);
            durationMap.merge(day, result.getDurationSeconds() == null ? 0 : Math.max(0, result.getDurationSeconds() / 60), Integer::sum);
        }
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            labels.add(day.getMonthValue() + "/" + day.getDayOfMonth());
            masteredValues.add(masteredMap.getOrDefault(day, 0));
            durationValues.add(durationMap.getOrDefault(day, 0));
        }
    }

    private String resolveFocusTopic(List<TenantReportRowDto> rows) {
        if (rows.isEmpty()) {
            return "暂无重点薄弱项";
        }
        double flashcard = rows.stream().mapToDouble(TenantReportRowDto::getFlashcardRate).average().orElse(0);
        double engCh = rows.stream().mapToDouble(TenantReportRowDto::getEngChRate).average().orElse(0);
        double chEng = rows.stream().mapToDouble(TenantReportRowDto::getChEngRate).average().orElse(0);
        double spell = rows.stream().mapToDouble(TenantReportRowDto::getSpellRate).average().orElse(0);
        Map<String, Double> scores = new HashMap<>();
        scores.put("看词识义", flashcard);
        scores.put("英译中", engCh);
        scores.put("中译英", chEng);
        scores.put("拼写训练", spell);
        return scores.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("综合训练");
    }

    private String resolveSuggestion(List<TenantReportRowDto> rows, String focusTopic) {
        if (rows.isEmpty()) {
            return "当前时间范围内暂无足够数据，建议先安排一轮基础学习任务。";
        }
        int errorCount = rows.stream().mapToInt(TenantReportRowDto::getTotalErrors).sum();
        return "建议围绕“" + focusTopic + "”增加针对性练习，当前时间范围内累计记录到 " + errorCount + " 次错误反馈。";
    }

    private String buildInsightSummary(List<TenantReportRowDto> rows, String focusTopic) {
        if (rows.isEmpty()) {
            return "当前暂无可生成洞察的学习记录。";
        }
        TenantReportRowDto topStudent = rows.stream()
                .max(Comparator.comparingDouble(this::rowAccuracy))
                .orElse(null);
        String studentName = topStudent == null ? "学生整体" : (topStudent.getRealName() != null ? topStudent.getRealName() : topStudent.getUsername());
        return studentName + " 在当前时间范围内表现更稳定，建议下一阶段重点加强“" + focusTopic + "”训练。";
    }

    private String resolveProgressLabel(double averageProgress) {
        if (averageProgress >= 85) return "优秀";
        if (averageProgress >= 70) return "良好";
        if (averageProgress >= 50) return "稳步提升";
        return "待加强";
    }

    private double calculateGrowthRate(double current, double previous) {
        if (previous <= 0D) {
            return current > 0D ? 100D : 0D;
        }
        return round2(((current - previous) * 100D) / previous);
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }
}

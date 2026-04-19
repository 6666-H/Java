package com.study.english.controller.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.LearningProgressRecordDto;
import com.study.english.dto.PageResult;
import com.study.english.entity.Book;
import com.study.english.entity.ErrorLog;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.StudyLog;
import com.study.english.entity.Unit;
import com.study.english.entity.User;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.service.BookService;
import com.study.english.service.ErrorLogService;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.UnitService;
import com.study.english.service.UserService;
import com.study.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校长端：按学生、日期查看学习进度与错误日志，支持按学习类型、记录类型、错误类型筛选。
 */
@RestController
@RequestMapping("/api/tenant/learning-progress")
@RequiredArgsConstructor
public class TenantLearningProgressController {

    private final StudentWordProgressService studentWordProgressService;
    private final StudyLogService studyLogService;
    private final ErrorLogService errorLogService;
    private final WordService wordService;
    private final UnitService unitService;
    private final BookService bookService;
    private final UserService userService;

    @GetMapping
    public Result<PageResult<LearningProgressRecordDto>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "all") String recordType,
            @RequestParam(required = false, defaultValue = "all") String studyType,
            @RequestParam(required = false, defaultValue = "all") String errorType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可查看");
        }

        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(10, pageSize));
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();
        boolean includeProgress = "all".equalsIgnoreCase(recordType) || "progress".equalsIgnoreCase(recordType);
        boolean includeError = "all".equalsIgnoreCase(recordType) || "error".equalsIgnoreCase(recordType);

        List<StudyLog> studyLogs = Collections.emptyList();
        List<ErrorLog> errorLogs = Collections.emptyList();
        List<StudentWordProgress> progressList = Collections.emptyList();
        Set<String> studyLogKeys = new HashSet<>();

        if (includeProgress || includeError) {
            Boolean onlyErrors = "error".equalsIgnoreCase(recordType) ? Boolean.TRUE : null;
            studyLogs = studyLogService.listByTenantAndRange(tenantId, studentId, start, end, studyType, onlyErrors);
            for (StudyLog log : studyLogs) {
                LocalDate day = log.getCreatedAt() != null ? log.getCreatedAt().toLocalDate() : null;
                studyLogKeys.add(key(log.getUserId(), log.getWordId(), day));
            }
        }

        if (includeError) {
            errorLogs = errorLogService.listByTenantAndRange(tenantId, studentId, start, end, errorType);
        }

        if (includeProgress) {
            LambdaQueryWrapper<StudentWordProgress> query = new LambdaQueryWrapper<>();
            query.eq(StudentWordProgress::getTenantId, tenantId)
                    .ge(StudentWordProgress::getLastStudyAt, start)
                    .le(StudentWordProgress::getLastStudyAt, end)
                    .isNotNull(StudentWordProgress::getLastStudyAt)
                    .orderByDesc(StudentWordProgress::getLastStudyAt);
            if (studentId != null) {
                query.eq(StudentWordProgress::getUserId, studentId);
            }
            progressList = studentWordProgressService.list(query);
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> wordIds = new HashSet<>();
        studyLogs.forEach(log -> {
            if (log.getUserId() != null) userIds.add(log.getUserId());
            if (log.getWordId() != null) wordIds.add(log.getWordId());
        });
        errorLogs.forEach(log -> {
            if (log.getUserId() != null) userIds.add(log.getUserId());
            if (log.getWordId() != null) wordIds.add(log.getWordId());
        });
        progressList.forEach(progress -> {
            if (progress.getUserId() != null) userIds.add(progress.getUserId());
            if (progress.getWordId() != null) wordIds.add(progress.getWordId());
        });

        Map<Long, User> userMap = loadUserMap(userIds);
        Map<Long, Word> wordMap = loadWordMap(wordIds);
        Map<Long, Unit> unitMap = loadUnitMap(wordMap.values().stream()
                .map(Word::getUnitId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet()));
        Map<Long, Book> bookMap = loadBookMap(unitMap.values().stream()
                .map(Unit::getBookId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet()));

        List<LearningProgressRecordDto> records = new ArrayList<>();

        for (StudyLog log : studyLogs) {
            boolean isError = StudyLog.FEEDBACK_DONT_KNOW.equalsIgnoreCase(log.getFeedbackType())
                    || StudyLog.FEEDBACK_SPELLING_ERROR.equalsIgnoreCase(log.getFeedbackType());
            if (isError && !includeError) continue;
            if (!isError && !includeProgress) continue;
            if (isError && errorType != null && !"all".equalsIgnoreCase(errorType)) {
                if ("DONT_KNOW".equalsIgnoreCase(errorType) && !StudyLog.FEEDBACK_DONT_KNOW.equalsIgnoreCase(log.getFeedbackType())) continue;
                if ("SPELLING_ERROR".equalsIgnoreCase(errorType) && !StudyLog.FEEDBACK_SPELLING_ERROR.equalsIgnoreCase(log.getFeedbackType())) continue;
            }
            LearningProgressRecordDto dto = buildDtoFromStudyLog(log, wordMap, unitMap, bookMap, userMap);
            if (dto == null) continue;
            dto.setRecordType(isError ? "error" : "progress");
            dto.setErrorType(isError ? log.getFeedbackType() : null);
            records.add(dto);
        }

        for (ErrorLog log : errorLogs) {
            LearningProgressRecordDto dto = buildDtoFromErrorLog(log, wordMap, unitMap, bookMap, userMap);
            if (dto == null) continue;
            dto.setRecordType("error");
            dto.setErrorType(log.getErrorType());
            dto.setStudyType(null);
            records.add(dto);
        }

        for (StudentWordProgress progress : progressList) {
            LocalDate day = progress.getLastStudyAt() != null ? progress.getLastStudyAt().toLocalDate() : null;
            if (studyLogKeys.contains(key(progress.getUserId(), progress.getWordId(), day))) continue;
            LearningProgressRecordDto dto = buildDtoFromProgress(progress, wordMap, unitMap, bookMap, userMap);
            if (dto == null) continue;
            dto.setRecordType("progress");
            dto.setStudyType(null);
            dto.setErrorType(null);
            records.add(dto);
        }

        records.sort(Comparator.comparing(LearningProgressRecordDto::getLastStudyAt, Comparator.nullsLast(Comparator.reverseOrder())));
        long total = records.size();
        long fromIndex = Math.min((safePage - 1) * safePageSize, total);
        long toIndex = Math.min(fromIndex + safePageSize, total);
        List<LearningProgressRecordDto> pageList = records.subList((int) fromIndex, (int) toIndex);
        return Result.ok(PageResult.of(pageList, total, safePage, safePageSize));
    }

    private String key(Long userId, Long wordId, LocalDate date) {
        return userId + ":" + wordId + ":" + (date != null ? date : "");
    }

    private Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        return userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, item -> item));
    }

    private Map<Long, Word> loadWordMap(Set<Long> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) return Collections.emptyMap();
        return wordService.listByIds(wordIds).stream().collect(Collectors.toMap(Word::getId, item -> item));
    }

    private Map<Long, Unit> loadUnitMap(Set<Long> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) return Collections.emptyMap();
        return unitService.listByIds(unitIds).stream().collect(Collectors.toMap(Unit::getId, item -> item));
    }

    private Map<Long, Book> loadBookMap(Set<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return Collections.emptyMap();
        return bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, item -> item));
    }

    private LearningProgressRecordDto buildDtoFromStudyLog(StudyLog log,
                                                           Map<Long, Word> wordMap,
                                                           Map<Long, Unit> unitMap,
                                                           Map<Long, Book> bookMap,
                                                           Map<Long, User> userMap) {
        Word word = wordMap.get(log.getWordId());
        if (word == null) return null;
        Unit unit = word.getUnitId() != null ? unitMap.get(word.getUnitId()) : null;
        Book book = unit != null && unit.getBookId() != null ? bookMap.get(unit.getBookId()) : null;
        User user = userMap.get(log.getUserId());
        LearningProgressRecordDto dto = new LearningProgressRecordDto();
        dto.setUserId(log.getUserId());
        dto.setUsername(user != null ? user.getUsername() : null);
        dto.setRealName(user != null ? user.getRealName() : null);
        dto.setStudyDate(log.getCreatedAt() != null ? log.getCreatedAt().toLocalDate() : null);
        dto.setBookName(book != null ? book.getName() : null);
        dto.setUnitName(unit != null ? unit.getName() : null);
        dto.setWord(word.getWord());
        dto.setWordId(word.getId());
        dto.setLastStudyAt(log.getCreatedAt());
        dto.setStudyType(log.getMode());
        dto.setUserInput(log.getUserInput());
        return dto;
    }

    private LearningProgressRecordDto buildDtoFromErrorLog(ErrorLog log,
                                                           Map<Long, Word> wordMap,
                                                           Map<Long, Unit> unitMap,
                                                           Map<Long, Book> bookMap,
                                                           Map<Long, User> userMap) {
        Word word = wordMap.get(log.getWordId());
        if (word == null) return null;
        Unit unit = word.getUnitId() != null ? unitMap.get(word.getUnitId()) : null;
        Book book = unit != null && unit.getBookId() != null ? bookMap.get(unit.getBookId()) : null;
        User user = userMap.get(log.getUserId());
        LearningProgressRecordDto dto = new LearningProgressRecordDto();
        dto.setUserId(log.getUserId());
        dto.setUsername(user != null ? user.getUsername() : null);
        dto.setRealName(user != null ? user.getRealName() : null);
        dto.setStudyDate(log.getCreatedAt() != null ? log.getCreatedAt().toLocalDate() : null);
        dto.setBookName(book != null ? book.getName() : null);
        dto.setUnitName(unit != null ? unit.getName() : null);
        dto.setWord(word.getWord());
        dto.setWordId(word.getId());
        dto.setLastStudyAt(log.getCreatedAt());
        return dto;
    }

    private LearningProgressRecordDto buildDtoFromProgress(StudentWordProgress progress,
                                                           Map<Long, Word> wordMap,
                                                           Map<Long, Unit> unitMap,
                                                           Map<Long, Book> bookMap,
                                                           Map<Long, User> userMap) {
        Word word = wordMap.get(progress.getWordId());
        if (word == null) return null;
        Unit unit = word.getUnitId() != null ? unitMap.get(word.getUnitId()) : null;
        Book book = unit != null && unit.getBookId() != null ? bookMap.get(unit.getBookId()) : null;
        User user = userMap.get(progress.getUserId());
        LearningProgressRecordDto dto = new LearningProgressRecordDto();
        dto.setUserId(progress.getUserId());
        dto.setUsername(user != null ? user.getUsername() : null);
        dto.setRealName(user != null ? user.getRealName() : null);
        dto.setStudyDate(progress.getLastStudyAt() != null ? progress.getLastStudyAt().toLocalDate() : null);
        dto.setBookName(book != null ? book.getName() : null);
        dto.setUnitName(unit != null ? unit.getName() : null);
        dto.setWord(word.getWord());
        dto.setWordId(word.getId());
        dto.setLastStudyAt(progress.getLastStudyAt());
        return dto;
    }
}

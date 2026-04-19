package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.StudyLog;
import com.study.english.mapper.StudyLogMapper;
import com.study.english.service.StudyLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudyLogServiceImpl extends ServiceImpl<StudyLogMapper, StudyLog> implements StudyLogService {

    @Override
    public void logStudy(String tenantId, Long userId, Long wordId, String mode, String feedbackType, String userInput) {
        StudyLog log = new StudyLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setWordId(wordId);
        log.setMode(mode != null && !mode.isEmpty() ? normalizeMode(mode) : null);
        log.setFeedbackType(feedbackType != null ? feedbackType.toUpperCase() : "KNOW");
        log.setUserInput(userInput != null && !userInput.isBlank() ? truncate(userInput, 500) : null);
        log.setCreatedAt(LocalDateTime.now());
        save(log);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    @Override
    public List<StudyLog> listByTenantAndRange(String tenantId, Long userId, LocalDateTime start, LocalDateTime end,
                                               String studyType, Boolean onlyErrors) {
        if (tenantId == null || start == null || end == null) return Collections.emptyList();
        LambdaQueryWrapper<StudyLog> w = new LambdaQueryWrapper<>();
        w.eq(StudyLog::getTenantId, tenantId)
                .ge(StudyLog::getCreatedAt, start)
                .le(StudyLog::getCreatedAt, end)
                .orderByDesc(StudyLog::getCreatedAt);
        if (userId != null) w.eq(StudyLog::getUserId, userId);
        if (studyType != null && !studyType.isEmpty() && !"all".equalsIgnoreCase(studyType)) {
            w.eq(StudyLog::getMode, studyType);
        }
        if (Boolean.TRUE.equals(onlyErrors)) {
            w.in(StudyLog::getFeedbackType, StudyLog.FEEDBACK_DONT_KNOW, StudyLog.FEEDBACK_SPELLING_ERROR);
        }
        return list(w);
    }

    @Override
    public int countActiveStudentsToday(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return 0;
        return baseMapper.countActiveStudentsToday(tenantId);
    }

    @Override
    public List<Map<String, Object>> getActiveTrend(String tenantId, LocalDate startDate, LocalDate endDate) {
        if (tenantId == null || tenantId.isBlank() || startDate == null || endDate == null) {
            return Collections.emptyList();
        }
        return baseMapper.selectActiveTrend(tenantId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return null;
        String u = mode.toUpperCase().replace("-", "_");
        if ("FLASHCARD".equals(u)) return StudyLog.MODE_FLASHCARD;
        if ("ENG_CH".equals(u)) return StudyLog.MODE_ENG_CH;
        if ("CH_ENG".equals(u)) return StudyLog.MODE_CH_ENG;
        if ("SPELL".equals(u)) return StudyLog.MODE_SPELL;
        return u;
    }
}

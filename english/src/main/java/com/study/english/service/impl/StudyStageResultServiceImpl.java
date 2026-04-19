package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.dto.SaveStageResultRequest;
import com.study.english.entity.StudyStageResult;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.StudyStageResultMapper;
import com.study.english.service.StudyStageResultService;
import com.study.english.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudyStageResultServiceImpl extends ServiceImpl<StudyStageResultMapper, StudyStageResult> implements StudyStageResultService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserService userService;

    public StudyStageResultServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void saveResult(String tenantId, Long userId, SaveStageResultRequest request) {
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        if (request.getUnitId() == null) throw new BusinessException("unitId 不能为空");
        StudyStageResult result = new StudyStageResult();
        result.setTenantId(tenantId);
        result.setUserId(userId);
        result.setUnitId(request.getUnitId());
        result.setStage(normalizeStage(request.getStage()));
        result.setBookName(request.getBookName());
        result.setUnitName(request.getUnitName());
        result.setTotalQuestions(request.getTotalQuestions());
        result.setCorrectAttempts(request.getCorrectAttempts());
        result.setWrongAttempts(request.getWrongAttempts());
        result.setFirstRoundCorrect(request.getFirstRoundCorrect());
        result.setStabilizedCount(request.getStabilizedCount());
        result.setDurationSeconds(request.getDurationSeconds());
        result.setStarReward(Boolean.TRUE.equals(request.getStarReward()) ? 1 : 0);
        result.setCreatedAt(LocalDateTime.now());
        save(result);
    }

    @Override
    public List<Map<String, Object>> listStudentResults(String tenantId, Long userId, int limit) {
        return toRows(list(new LambdaQueryWrapper<StudyStageResult>()
                .eq(StudyStageResult::getTenantId, tenantId)
                .eq(StudyStageResult::getUserId, userId)
                .orderByDesc(StudyStageResult::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)))), false);
    }

    @Override
    public List<Map<String, Object>> listTenantResults(String tenantId, Long studentId, LocalDate startDate, LocalDate endDate, int limit) {
        LambdaQueryWrapper<StudyStageResult> query = new LambdaQueryWrapper<StudyStageResult>()
                .eq(StudyStageResult::getTenantId, tenantId)
                .eq(studentId != null, StudyStageResult::getUserId, studentId)
                .ge(startDate != null, StudyStageResult::getCreatedAt, startDate != null ? startDate.atStartOfDay() : null)
                .le(endDate != null, StudyStageResult::getCreatedAt, endDate != null ? endDate.atTime(LocalTime.MAX) : null)
                .orderByDesc(StudyStageResult::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return toRows(list(query), true);
    }

    private List<Map<String, Object>> toRows(List<StudyStageResult> list, boolean includeUser) {
        if (list.isEmpty()) return List.of();
        Set<Long> userIds = list.stream().map(StudyStageResult::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = includeUser
                ? userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, item -> item))
                : Map.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StudyStageResult item : list) {
            User user = includeUser ? userMap.get(item.getUserId()) : null;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("userId", item.getUserId());
            row.put("username", user != null ? user.getUsername() : "");
            row.put("realName", user != null ? user.getRealName() : "");
            row.put("unitId", item.getUnitId());
            row.put("bookName", item.getBookName() == null ? "" : item.getBookName());
            row.put("unitName", item.getUnitName() == null ? "" : item.getUnitName());
            row.put("stage", item.getStage() == null ? "" : item.getStage());
            row.put("totalQuestions", item.getTotalQuestions() == null ? 0 : item.getTotalQuestions());
            row.put("correctAttempts", item.getCorrectAttempts() == null ? 0 : item.getCorrectAttempts());
            row.put("wrongAttempts", item.getWrongAttempts() == null ? 0 : item.getWrongAttempts());
            row.put("firstRoundCorrect", item.getFirstRoundCorrect() == null ? 0 : item.getFirstRoundCorrect());
            row.put("stabilizedCount", item.getStabilizedCount() == null ? 0 : item.getStabilizedCount());
            row.put("durationSeconds", item.getDurationSeconds() == null ? 0 : item.getDurationSeconds());
            row.put("starReward", item.getStarReward() != null && item.getStarReward() == 1);
            row.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().format(FORMATTER) : "");
            rows.add(row);
        }
        return rows;
    }

    private String normalizeStage(String stage) {
        return stage == null ? "flashcard" : stage.trim().toLowerCase();
    }
}

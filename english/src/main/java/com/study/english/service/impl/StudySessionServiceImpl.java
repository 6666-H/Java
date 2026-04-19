package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.english.entity.StudySession;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.StudySessionMapper;
import com.study.english.service.StudySessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StudySessionServiceImpl extends ServiceImpl<StudySessionMapper, StudySession> implements StudySessionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> getSessionPayload(String tenantId, Long userId, Long unitId, String stage) {
        StudySession session = getOne(new LambdaQueryWrapper<StudySession>()
                .eq(StudySession::getTenantId, tenantId)
                .eq(StudySession::getUserId, userId)
                .eq(StudySession::getUnitId, unitId)
                .eq(StudySession::getStage, normalizeStage(stage))
                .last("LIMIT 1"));
        if (session == null) return null;
        Map<String, Object> data = new HashMap<>();
        data.put("words", readJson(session.getWordsJson()));
        data.put("queue", readJson(session.getQueueJson()));
        data.put("errorIds", readJson(session.getErrorIdsJson()));
        data.put("userInputMap", readJson(session.getUserInputJson()));
        data.put("firstAttemptSeenIds", readJson(session.getFirstAttemptSeenJson()));
        data.put("firstAttemptCorrectIds", readJson(session.getFirstAttemptCorrectJson()));
        data.put("initialCount", session.getInitialCount());
        data.put("answeredCount", session.getAnsweredCount());
        data.put("firstRoundCorrectCount", session.getFirstRoundCorrect());
        data.put("correctAttemptsCount", session.getCorrectAttempts());
        data.put("wrongAttemptsCount", session.getWrongAttempts());
        data.put("startedAt", session.getStartedAt() != null ? session.getStartedAt().toString() : null);
        data.put("timestamp", session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null);
        return data;
    }

    @Override
    public void saveSessionPayload(String tenantId, Long userId, Long unitId, String stage, Map<String, Object> payload) {
        StudySession session = getOne(new LambdaQueryWrapper<StudySession>()
                .eq(StudySession::getTenantId, tenantId)
                .eq(StudySession::getUserId, userId)
                .eq(StudySession::getUnitId, unitId)
                .eq(StudySession::getStage, normalizeStage(stage))
                .last("LIMIT 1"));
        if (session == null) {
            session = new StudySession();
            session.setTenantId(tenantId);
            session.setUserId(userId);
            session.setUnitId(unitId);
            session.setStage(normalizeStage(stage));
            session.setCreatedAt(LocalDateTime.now());
        }
        session.setWordsJson(writeJson(payload.get("words")));
        session.setQueueJson(writeJson(payload.get("queue")));
        session.setErrorIdsJson(writeJson(payload.get("errorIds")));
        session.setUserInputJson(writeJson(payload.get("userInputMap")));
        session.setFirstAttemptSeenJson(writeJson(payload.get("firstAttemptSeenIds")));
        session.setFirstAttemptCorrectJson(writeJson(payload.get("firstAttemptCorrectIds")));
        session.setInitialCount(asInt(payload.get("initialCount")));
        session.setAnsweredCount(asInt(payload.get("answeredCount")));
        session.setFirstRoundCorrect(asInt(payload.get("firstRoundCorrectCount")));
        session.setCorrectAttempts(asInt(payload.get("correctAttemptsCount")));
        session.setWrongAttempts(asInt(payload.get("wrongAttemptsCount")));
        session.setStartedAt(parseDateTime(payload.get("startedAt")));
        session.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(session);
    }

    @Override
    public void clearSession(String tenantId, Long userId, Long unitId, String stage) {
        remove(new LambdaQueryWrapper<StudySession>()
                .eq(StudySession::getTenantId, tenantId)
                .eq(StudySession::getUserId, userId)
                .eq(StudySession::getUnitId, unitId)
                .eq(StudySession::getStage, normalizeStage(stage)));
    }

    private Object readJson(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return objectMapper.readValue(value, new TypeReference<Object>() {});
        } catch (Exception ex) {
            throw new BusinessException("学习会话数据读取失败");
        }
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException("学习会话数据保存失败");
        }
    }

    private Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeStage(String stage) {
        return stage == null ? "flashcard" : stage.trim().toLowerCase();
    }
}

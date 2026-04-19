package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.StudySession;

import java.util.Map;

public interface StudySessionService extends IService<StudySession> {
    Map<String, Object> getSessionPayload(String tenantId, Long userId, Long unitId, String stage);

    void saveSessionPayload(String tenantId, Long userId, Long unitId, String stage, Map<String, Object> payload);

    void clearSession(String tenantId, Long userId, Long unitId, String stage);
}

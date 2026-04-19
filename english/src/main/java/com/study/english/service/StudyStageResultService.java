package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.dto.SaveStageResultRequest;
import com.study.english.entity.StudyStageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StudyStageResultService extends IService<StudyStageResult> {
    void saveResult(String tenantId, Long userId, SaveStageResultRequest request);

    List<Map<String, Object>> listStudentResults(String tenantId, Long userId, int limit);

    List<Map<String, Object>> listTenantResults(String tenantId, Long studentId, LocalDate startDate, LocalDate endDate, int limit);
}

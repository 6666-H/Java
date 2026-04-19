package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.StudyLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StudyLogService extends IService<StudyLog> {

    /**
     * 记录一次学习反馈（含学习类型、反馈类型、学生具体操作如选题/拼写内容）。
     */
    void logStudy(String tenantId, Long userId, Long wordId, String mode, String feedbackType, String userInput);

    /** 校长端：按租户、时间范围、可选学生、学习类型、是否仅错误 查询 */
    List<StudyLog> listByTenantAndRange(String tenantId, Long userId, LocalDateTime start, LocalDateTime end,
                                        String studyType, Boolean onlyErrors);

    int countActiveStudentsToday(String tenantId);

    List<Map<String, Object>> getActiveTrend(String tenantId, LocalDate startDate, LocalDate endDate);
}

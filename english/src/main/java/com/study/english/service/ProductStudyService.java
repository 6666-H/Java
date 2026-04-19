package com.study.english.service;

import com.study.english.dto.StudyQueueItemDto;
import com.study.english.dto.UnitProgressDetailDto;

import java.util.List;
import java.util.Map;

/**
 * 教材/单元模式 - 学习队列、答题、统计
 */
public interface ProductStudyService {

    /**
     * 单元学习队列：复习优先 + 新词（每日限量 20），同一词当日最多 2 次
     */
    List<StudyQueueItemDto> getUnitQueue(String tenantId, Long userId, Long unitId, int limit);

    /**
     * 全局复习队列：仅到期词
     */
    List<StudyQueueItemDto> getReviewQueue(String tenantId, Long userId, int limit);

    /**
     * 提交答题（产品记忆算法）
     */
    Map<String, Object> submitAnswer(String tenantId,
                                     Long userId,
                                     Long wordId,
                                     boolean isCorrect,
                                     String studyMode,
                                     String userInput);

    /**
     * 单元进度
     */
    UnitProgressDetailDto getUnitProgress(String tenantId, Long userId, Long unitId);

    /**
     * 今日统计
     */
    Map<String, Object> getTodayStats(String tenantId, Long userId);

    /**
     * 概览统计
     */
    Map<String, Object> getOverviewStats(String tenantId, Long userId);
}

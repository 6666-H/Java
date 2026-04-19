package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.ErrorReviewComplete;

import java.util.Map;
import java.util.Set;

/**
 * 错题复习完成记录：用于智能归纳/历史错题划分。
 */
public interface ErrorReviewCompleteService extends IService<ErrorReviewComplete> {

    /** 标记某日错题复习已完成；errorType 为空时表示本日全部类型完成。 */
    void markComplete(String tenantId, Long userId, String dateStr, String errorType);

    /** 获取用户已完成的复习日期和类型集合。 */
    Map<String, Set<String>> getCompletedTypesByDate(String tenantId, Long userId);
}

package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.StudentUnitCompletion;

import java.time.LocalDateTime;

/**
 * 单元完成记录。
 */
public interface StudentUnitCompletionService extends IService<StudentUnitCompletion> {

    /**
     * 获取单元完成时间，未完成返回 null。
     */
    LocalDateTime getCompletedAt(String tenantId, Long userId, Long unitId);
}

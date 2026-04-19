package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.StudentUnitCompletion;
import com.study.english.mapper.StudentUnitCompletionMapper;
import com.study.english.service.StudentUnitCompletionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StudentUnitCompletionServiceImpl extends ServiceImpl<StudentUnitCompletionMapper, StudentUnitCompletion>
        implements StudentUnitCompletionService {

    @Override
    public LocalDateTime getCompletedAt(String tenantId, Long userId, Long unitId) {
        StudentUnitCompletion c = getOne(new LambdaQueryWrapper<StudentUnitCompletion>()
                .eq(StudentUnitCompletion::getTenantId, tenantId)
                .eq(StudentUnitCompletion::getUserId, userId)
                .eq(StudentUnitCompletion::getUnitId, unitId));
        return c != null ? c.getCompletedAt() : null;
    }
}

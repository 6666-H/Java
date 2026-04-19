package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.ErrorReviewComplete;
import com.study.english.mapper.ErrorReviewCompleteMapper;
import com.study.english.service.ErrorReviewCompleteService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
    public class ErrorReviewCompleteServiceImpl extends ServiceImpl<ErrorReviewCompleteMapper, ErrorReviewComplete>
        implements ErrorReviewCompleteService {

    @Override
    public void markComplete(String tenantId, Long userId, String dateStr, String errorType) {
        if (tenantId == null || userId == null || dateStr == null || dateStr.isEmpty()) return;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            String normalizedType = normalizeType(errorType);
            LambdaQueryWrapper<ErrorReviewComplete> w = new LambdaQueryWrapper<>();
            w.eq(ErrorReviewComplete::getTenantId, tenantId)
                    .eq(ErrorReviewComplete::getUserId, userId)
                    .eq(ErrorReviewComplete::getReviewDate, date)
                    .eq(ErrorReviewComplete::getErrorType, normalizedType);
            if (count(w) > 0) return; // 已存在，幂等
            ErrorReviewComplete rec = new ErrorReviewComplete();
            rec.setTenantId(tenantId);
            rec.setUserId(userId);
            rec.setReviewDate(date);
            rec.setErrorType(normalizedType);
            save(rec);
        } catch (DateTimeParseException ignored) {}
    }

    @Override
    public Map<String, Set<String>> getCompletedTypesByDate(String tenantId, Long userId) {
        if (tenantId == null || userId == null) return Map.of();
        List<Map<String, Object>> rows = baseMapper.selectCompletedTypesByDate(tenantId, userId);
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> row : rows) {
            String reviewDate = row.get("reviewDate") != null ? String.valueOf(row.get("reviewDate")) : "";
            String errorType = row.get("errorType") != null ? String.valueOf(row.get("errorType")) : ErrorReviewComplete.TYPE_ALL;
            if (reviewDate.isEmpty()) continue;
            result.computeIfAbsent(reviewDate, key -> new java.util.LinkedHashSet<>()).add(normalizeType(errorType));
        }
        return result;
    }

    private static String normalizeType(String errorType) {
        if (errorType == null || errorType.isBlank()) return ErrorReviewComplete.TYPE_ALL;
        return errorType.trim().toUpperCase();
    }
}

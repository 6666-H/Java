package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.ErrorLog;
import com.study.english.entity.ErrorReviewComplete;
import com.study.english.mapper.ErrorLogMapper;
import com.study.english.mapper.StudentWordProgressMapper;
import com.study.english.service.ErrorLogService;
import com.study.english.service.ErrorReviewCompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorLogServiceImpl extends ServiceImpl<ErrorLogMapper, ErrorLog> implements ErrorLogService {

    private final ErrorReviewCompleteService errorReviewCompleteService;
    private final StudentWordProgressMapper studentWordProgressMapper;

    @Override
    public void logError(String tenantId, Long userId, Long wordId, String errorType) {
        if (tenantId == null || userId == null || wordId == null || errorType == null) return;
        LocalDateTime now = LocalDateTime.now();
        ErrorLog log = new ErrorLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setWordId(wordId);
        log.setErrorType(errorType);
        log.setCreatedAt(now);
        save(log);
    }

    @Override
    public List<String> getErrorDates(String tenantId, Long userId, int days) {
        if (tenantId == null || userId == null || days <= 0) return Collections.emptyList();
        return baseMapper.selectErrorDates(tenantId, userId, days);
    }

    @Override
    public List<Map<String, Object>> getErrorsByDate(String tenantId, Long userId, String date) {
        if (tenantId == null || userId == null || date == null || date.isEmpty()) return Collections.emptyList();
        return baseMapper.selectErrorsByDate(tenantId, userId, date);
    }

    @Override
    public List<Map<String, Object>> getErrorsByDateGrouped(String tenantId, Long userId, String date) {
        List<Map<String, Object>> flat = getErrorsByDate(tenantId, userId, date);
        if (flat == null || flat.isEmpty()) return Collections.emptyList();
        Map<String, List<Map<String, Object>>> byType = new java.util.LinkedHashMap<>();
        for (Map<String, Object> item : flat) {
            String type = item.get("errorType") != null ? String.valueOf(item.get("errorType")) : "FLASHCARD";
            if (item.get("error_type") != null) type = String.valueOf(item.get("error_type"));
            byType.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(item);
        }
        return byType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("type", e.getKey());
                    m.put("items", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Integer.compare(
                        ((List<?>) b.get("items")).size(),
                        ((List<?>) a.get("items")).size()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getWordIdsByDate(String tenantId, Long userId, String date) {
        if (tenantId == null || userId == null || date == null || date.isEmpty()) return Collections.emptyList();
        return baseMapper.selectWordIdsByDate(tenantId, userId, date);
    }

    @Override
    public List<Map<String, Object>> getErrorStatsByDate(String tenantId, Long userId, int days, String search) {
        if (tenantId == null || userId == null || days <= 0) return Collections.emptyList();
        List<Map<String, Object>> rows = baseMapper.selectErrorStatsByDate(tenantId, userId, days);
        Map<String, Map<String, Object>> byDate = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String d = row.get("date") != null ? String.valueOf(row.get("date")) : "";
            String type = row.get("errorType") != null ? String.valueOf(row.get("errorType")) : "OTHER";
            int cnt = row.get("cnt") != null ? ((Number) row.get("cnt")).intValue() : 0;
            byDate.putIfAbsent(d, new java.util.HashMap<>());
            Map<String, Object> dayMap = byDate.get(d);
            dayMap.put("date", d);
            @SuppressWarnings("unchecked")
            Map<String, Integer> typeCounts = (Map<String, Integer>) dayMap.get("typeCounts");
            if (typeCounts == null) {
                typeCounts = new java.util.HashMap<>();
                dayMap.put("typeCounts", typeCounts);
            }
            typeCounts.put(type, cnt);
            dayMap.put("totalCount", (Integer) dayMap.getOrDefault("totalCount", 0) + cnt);
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>(byDate.values());
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim();
            result = result.stream().filter(m -> String.valueOf(m.get("date")).contains(q)).collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getErrorStatsByDateFiltered(String tenantId, Long userId, int days,
            String startDate, String endDate, Long bookId, Long unitId) {
        if (tenantId == null || userId == null || days <= 0) return Collections.emptyList();
        List<Map<String, Object>> rows = baseMapper.selectErrorStatsByDateFiltered(
                tenantId, userId, days, startDate, endDate, bookId, unitId);
        Map<String, Set<String>> completedTypesByDate = errorReviewCompleteService.getCompletedTypesByDate(tenantId, userId);
        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String date = row.get("date") != null ? String.valueOf(row.get("date")) : "";
            String errorType = row.get("errorType") != null ? String.valueOf(row.get("errorType")).toUpperCase() : ErrorLog.TYPE_DONT_KNOW;
            int count = row.get("cnt") != null ? ((Number) row.get("cnt")).intValue() : 0;
            Map<String, Object> item = byDate.computeIfAbsent(date, key -> {
                Map<String, Object> init = new LinkedHashMap<>();
                init.put("date", key);
                init.put("count", 0);
                init.put("types", new LinkedHashSet<String>());
                return init;
            });
            item.put("count", ((Integer) item.get("count")) + count);
            @SuppressWarnings("unchecked")
            Set<String> types = (Set<String>) item.get("types");
            types.add(errorType);
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> item : byDate.values()) {
            String date = String.valueOf(item.get("date"));
            @SuppressWarnings("unchecked")
            Set<String> types = (Set<String>) item.get("types");
            Set<String> completedTypes = completedTypesByDate.getOrDefault(date, Set.of());
            boolean completed = completedTypes.contains(ErrorReviewComplete.TYPE_ALL) || completedTypes.containsAll(types);
            item.put("completed", completed);
            item.remove("types");
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getSmartSummaryList(String tenantId, Long userId, int days,
            String startDate, String endDate, Long bookId, Long unitId,
            String search, int page, int pageSize) {
        List<Map<String, Object>> all = getErrorStatsByDateFiltered(
                tenantId, userId, days, startDate, endDate, bookId, unitId);
        List<Map<String, Object>> pending = new java.util.ArrayList<>();
        for (Map<String, Object> r : all) {
            String date = r.get("date") != null ? String.valueOf(r.get("date")) : "";
            int cnt = r.get("count") != null ? ((Number) r.get("count")).intValue() : 0;
            if (cnt <= 0 || Boolean.TRUE.equals(r.get("completed"))) continue;
            if (search != null && !search.trim().isEmpty() && !date.contains(search.trim())) continue;
            pending.add(r);
        }
        int total = pending.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> list = pending.subList(from, to);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("hasPending", total > 0);
        result.put("firstDate", total > 0 ? pending.get(0).get("date") : null);
        return result;
    }

    @Override
    public Map<String, Object> getHistoryErrorList(String tenantId, Long userId, int days,
            String startDate, String endDate, Long bookId, Long unitId,
            String search, int page, int pageSize) {
        List<Map<String, Object>> all = getErrorStatsByDateFiltered(
                tenantId, userId, days, startDate, endDate, bookId, unitId);
        List<Map<String, Object>> completed = new java.util.ArrayList<>();
        for (Map<String, Object> r : all) {
            String date = r.get("date") != null ? String.valueOf(r.get("date")) : "";
            int cnt = r.get("count") != null ? ((Number) r.get("count")).intValue() : 0;
            if (cnt <= 0 || !Boolean.TRUE.equals(r.get("completed"))) continue;
            if (search != null && !search.trim().isEmpty() && !date.contains(search.trim())) continue;
            completed.add(r);
        }
        int total = completed.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> list = completed.subList(from, to);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    @Override
    public List<ErrorLog> listByTenantAndRange(String tenantId, Long userId, LocalDateTime start, LocalDateTime end, String errorType) {
        if (tenantId == null || start == null || end == null) return Collections.emptyList();
        LambdaQueryWrapper<ErrorLog> w = new LambdaQueryWrapper<>();
        w.eq(ErrorLog::getTenantId, tenantId)
                .ge(ErrorLog::getCreatedAt, start)
                .le(ErrorLog::getCreatedAt, end)
                .orderByDesc(ErrorLog::getCreatedAt);
        if (userId != null) w.eq(ErrorLog::getUserId, userId);
        if (errorType != null && !errorType.isEmpty() && !"all".equalsIgnoreCase(errorType)) {
            w.eq(ErrorLog::getErrorType, errorType);
        }
        return list(w);
    }

    @Override
    public int countWeakWordsForDate(String tenantId, Long userId, String date, String errorType) {
        if (tenantId == null || userId == null || date == null || date.isBlank()) return 0;
        String normalized = errorType == null || errorType.isBlank() ? null : errorType.trim().toUpperCase();
        return studentWordProgressMapper.countWeakWordsForDate(tenantId, userId, date, normalized);
    }
}

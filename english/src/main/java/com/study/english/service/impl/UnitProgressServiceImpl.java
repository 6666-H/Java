package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.dto.BookProgressStatsDto;
import com.study.english.dto.UnitProgressDto;
import com.study.english.entity.Unit;
import com.study.english.entity.Word;
import com.study.english.mapper.StudentWordProgressMapper;
import com.study.english.service.StudentUnitCompletionService;
import com.study.english.service.UnitProgressService;
import com.study.english.service.UnitService;
import com.study.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UnitProgressServiceImpl implements UnitProgressService {

    private final UnitService unitService;
    private final WordService wordService;
    private final StudentWordProgressMapper progressMapper;
    private final StudentUnitCompletionService unitCompletionService;

    @Override
    public List<UnitProgressDto> getUnitProgressList(String tenantId, Long userId, Long bookId) {
        List<Unit> units = unitService.lambdaQuery()
                .eq(Unit::getBookId, bookId)
                .orderByAsc(Unit::getSortOrder)
                .list();
        List<UnitProgressDto> result = new ArrayList<>();
        for (Unit u : units) {
            int total = (int) wordService.count(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, u.getId()));
            int mastered = progressMapper.countMasteredInUnit(tenantId, userId, u.getId());
            int error = progressMapper.countErrorInUnit(tenantId, userId, u.getId());
            int percent = total > 0 ? (mastered * 100 / total) : 0;
            LocalDateTime completedAt = unitCompletionService.getCompletedAt(tenantId, userId, u.getId());
            boolean completed = total > 0 && mastered >= total;
            result.add(new UnitProgressDto(u.getId(), u.getName(), u.getSortOrder(),
                    total, mastered, error, percent, completed, completedAt));
        }
        return result;
    }

    @Override
    public BookProgressStatsDto getBookProgressStats(String tenantId, Long userId, Long bookId) {
        List<UnitProgressDto> units = getUnitProgressList(tenantId, userId, bookId);
        int totalUnits = units.size();
        int completedUnits = (int) units.stream().filter(UnitProgressDto::isCompleted).count();
        int bookPercent = totalUnits > 0 ? (completedUnits * 100 / totalUnits) : 0;
        int totalMastered = units.stream().mapToInt(UnitProgressDto::getMasteredCount).sum();
        int totalError = units.stream().mapToInt(UnitProgressDto::getErrorCount).sum();
        Map<String, Object> sums = progressMapper.sumCorrectWrongInBook(tenantId, userId, bookId);
        long correctSum = sums != null && sums.get("correctSum") != null
                ? ((Number) sums.get("correctSum")).longValue() : 0;
        long wrongSum = sums != null && sums.get("wrongSum") != null
                ? ((Number) sums.get("wrongSum")).longValue() : 0;
        int avgAccuracy = (correctSum + wrongSum) > 0
                ? (int) (correctSum * 100 / (correctSum + wrongSum)) : 0;
        return new BookProgressStatsDto(bookPercent, totalMastered, totalError, avgAccuracy);
    }
}

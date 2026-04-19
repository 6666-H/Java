package com.study.english.service;

import com.study.english.dto.BookProgressStatsDto;
import com.study.english.dto.UnitProgressDto;

import java.util.List;

/**
 * 单元/书本进度统计（用于版本→年级→册别→单元 流程）。
 */
public interface UnitProgressService {

    List<UnitProgressDto> getUnitProgressList(String tenantId, Long userId, Long bookId);

    BookProgressStatsDto getBookProgressStats(String tenantId, Long userId, Long bookId);
}

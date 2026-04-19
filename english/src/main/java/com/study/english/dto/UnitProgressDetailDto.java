package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教材/单元模式 - 单元进度详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitProgressDetailDto {

    private String status;      // not_started / learning / completed
    private Double progress;    // 0~1
    private Integer learnedCount;
    private Integer totalCount;
    private LocalDateTime completedAt;
}

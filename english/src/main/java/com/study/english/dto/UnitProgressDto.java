package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单元进度：总词数、已掌握、错词、完成度、完成时间。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitProgressDto {

    private Long unitId;
    private String unitName;
    private Integer sortOrder;
    /** 总词数 */
    private int totalCount;
    /** 已掌握（至少答对1次） */
    private int masteredCount;
    /** 错词数（重点/强化池） */
    private int errorCount;
    /** 完成度 0-100 */
    private int completionPercent;
    /** 是否已完成 */
    private boolean completed;
    /** 完成时间 */
    private LocalDateTime completedAt;
}

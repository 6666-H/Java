package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 书本/年级统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookProgressStatsDto {

    /** 书本完成率 0-100 */
    private int bookCompletionPercent;
    /** 总掌握单词数 */
    private int totalMasteredCount;
    /** 错词总数 */
    private int totalErrorCount;
    /** 单元平均正确率 0-100 */
    private int avgAccuracyPercent;
}

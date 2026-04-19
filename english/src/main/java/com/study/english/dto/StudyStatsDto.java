package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本单元学习统计：待复习错题数、待学习新词数、总词数、是否全部完成。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyStatsDto {

    /** 今日待复习错题数（本单元） */
    private int reviewCount;
    /** 待学习新词数（本单元） */
    private int newCount;
    /** 本单元总词数 */
    private int totalCount;
    /**
     * 本单元是否全部完成：每个功能都完成且错题也学完了。
     * 即：新词数=0 且 待复习错题数=0。
     */
    private boolean completed;
}


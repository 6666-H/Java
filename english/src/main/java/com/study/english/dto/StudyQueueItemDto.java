package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 教材/单元模式 - 学习队列项（中文→选英文，四选一）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyQueueItemDto {

    private Long wordId;
    private String word;
    private String phonetic;
    private String meaning;
    /** 4 个英文选项（含正确项），已打乱 */
    private List<String> options;
    /** 正确选项在 options 中的索引 0-3 */
    private Integer correctIndex;
    /** 是否复习词（true=已到期复习，false=新词） */
    private Boolean isReview;
}

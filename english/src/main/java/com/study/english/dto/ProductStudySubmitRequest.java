package com.study.english.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教材/单元模式 - 答题提交（简化：仅 wordId + isCorrect）
 */
@Data
public class ProductStudySubmitRequest {

    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    /** true=答对，false=答错 */
    @NotNull(message = "答题结果不能为空")
    private Boolean isCorrect;
}

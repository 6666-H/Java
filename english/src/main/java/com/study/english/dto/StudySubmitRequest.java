package com.study.english.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学习反馈提交：单词ID + 反馈类型（认识/不认识/拼写错误）
 */
@Data
public class StudySubmitRequest {

    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    /** KNOW=认识, DONT_KNOW=不认识, SPELLING_ERROR=拼写错误 */
    @NotNull(message = "反馈类型不能为空")
    private String feedbackType;

    /** 学习类型（可选）：flashcard/eng_ch/ch_eng/spell，对应 看词识义/看英选中/看中选英/拼写 */
    private String mode;

    /** 错误类型=练习类型（可选）：FLASHCARD/ENG_TO_CH/CH_TO_ENG/SPELLING_ERROR，非认识/不认识；不传时由 mode 推导 */
    private String errorType;

    /** 学生具体操作： eng_ch/ch_eng=选错的选项, spell=拼写输入，错时传 */
    private String userInput;
}

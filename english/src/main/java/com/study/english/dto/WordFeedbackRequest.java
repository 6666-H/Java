package com.study.english.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 单词反馈请求：认识/不认识。
 */
@Data
public class WordFeedbackRequest {

    /** 单词 ID（必填） */
    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    /** true=认识，false=不认识（必填） */
    @NotNull(message = "反馈不能为空")
    private Boolean known;
}

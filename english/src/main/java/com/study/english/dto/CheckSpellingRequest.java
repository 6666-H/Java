package com.study.english.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 拼写校验请求。
 */
@Data
public class CheckSpellingRequest {

    /** 单词 ID（必填） */
    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    /** 用户输入的拼写内容，可为空 */
    private String userInput;
}

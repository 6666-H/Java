package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼写校验结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckSpellingResult {

    /** 是否拼写正确 */
    private boolean correct;
    /** 提示信息：正确时为 "拼写正确"，错误时为具体提示 */
    private String message;

    public static CheckSpellingResult ok() {
        return new CheckSpellingResult(true, "拼写正确");
    }

    public static CheckSpellingResult fail(String message) {
        return new CheckSpellingResult(false, message != null ? message : "拼写错误");
    }
}

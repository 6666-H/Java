package com.study.english.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记本单元某类型练习已完成。
 */
@Data
public class CompleteModeRequest {

    @NotNull(message = "单元ID不能为空")
    private Long unitId;

    /** 学习类型：flashcard / eng_ch / ch_eng / spell */
    @NotNull(message = "类型不能为空")
    private String mode;
}

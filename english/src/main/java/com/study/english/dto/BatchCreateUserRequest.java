package com.study.english.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 批量创建用户请求。
 */
@Data
public class BatchCreateUserRequest {

    /** 用户名前缀，如 test 生成 test01、test02、... */
    @NotBlank(message = "前缀不能为空")
    private String prefix;

    /** 创建数量，1-500 */
    @NotNull
    @Min(1)
    @Max(500)
    private Integer count;

    /** 统一密码（必填） */
    @NotBlank(message = "密码不能为空")
    private String password;
}

package com.study.english.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 创建用户请求（单个）。
 */
@Data
public class CreateUserRequest {

    /** 用户名，2-32 位字母数字下划线 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{2,32}$", message = "用户名 2-32 位字母数字下划线")
    private String username;

    /** 密码（必填） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 学号/编号 */
    private String studentNo;

    /** 年级/班级 */
    private String gradeClass;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 角色：STUDENT / ORG_ADMIN（必填） */
    @NotNull(message = "角色不能为空")
    private String role;
}

package com.study.english.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 */
@Data
public class LoginRequest {

    /** 用户名（必填） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（必填） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 租户ID=手机号，学生端必填，校长/管理端不传 */
    private String tenantId;

    /** 期望角色：STUDENT | ORG_ADMIN | SUPER_ADMIN，用于登录入口校验 */
    private String role;
}

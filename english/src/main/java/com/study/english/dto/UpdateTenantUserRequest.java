package com.study.english.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTenantUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String realName;

    private String studentNo;

    private String gradeClass;

    private Integer status;
}

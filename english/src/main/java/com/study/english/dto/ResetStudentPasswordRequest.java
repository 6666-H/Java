package com.study.english.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetStudentPasswordRequest {
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}

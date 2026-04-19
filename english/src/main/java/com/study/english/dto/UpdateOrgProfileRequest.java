package com.study.english.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrgProfileRequest {
    @NotBlank
    private String username;
    private String realName;
    private String phone;
}

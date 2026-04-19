package com.study.english.dto;

import lombok.Data;

@Data
public class OrgProfileDto {
    private Long userId;
    private String tenantId;
    private String username;
    private String realName;
    private String phone;
    private String passwordPlain;
}

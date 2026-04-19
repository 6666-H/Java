package com.study.english.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantSettingsDto {
    private String tenantId;
    private String tenantName;
    private String contactPhone;
    private String address;
    private String logoUrl;
    private String interfaceLanguage;
    private String timezone;
    private Boolean emailReportEnabled;
    private Boolean newStudentNotifyEnabled;
    private Boolean mfaEnabled;
    private Long daysRemaining;
    private LocalDateTime expireTime;
    private LocalDateTime updatedAt;
}

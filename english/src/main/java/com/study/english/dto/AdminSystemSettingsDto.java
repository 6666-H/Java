package com.study.english.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminSystemSettingsDto {
    private String systemName;
    private String supportEmail;
    private String footerText;
    private String logoUrl;
    private Boolean strongPasswordEnabled;
    private Boolean mfaEnabled;
    private Integer loginFailureLimit;
    private Integer sessionTimeoutMinutes;
    private LocalDateTime updatedAt;
}

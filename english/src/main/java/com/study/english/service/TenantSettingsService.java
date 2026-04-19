package com.study.english.service;

import com.study.english.dto.TenantSettingsDto;
import org.springframework.web.multipart.MultipartFile;

public interface TenantSettingsService {
    TenantSettingsDto getSettings(String tenantId);

    TenantSettingsDto saveSettings(String tenantId, TenantSettingsDto settings);

    TenantSettingsDto saveLogo(String tenantId, MultipartFile file) throws Exception;
}

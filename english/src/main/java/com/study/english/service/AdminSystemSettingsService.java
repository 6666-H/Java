package com.study.english.service;

import com.study.english.dto.AdminSystemSettingsDto;
import org.springframework.web.multipart.MultipartFile;

public interface AdminSystemSettingsService {
    AdminSystemSettingsDto getSettings();

    AdminSystemSettingsDto saveSettings(AdminSystemSettingsDto settings);

    AdminSystemSettingsDto saveLogo(MultipartFile file) throws Exception;
}

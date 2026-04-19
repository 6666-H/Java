package com.study.english.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.english.dto.AdminSystemSettingsDto;
import com.study.english.service.AdminSystemSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AdminSystemSettingsServiceImpl implements AdminSystemSettingsService {

    private static final Path SETTINGS_PATH = Paths.get("data", "admin-system-settings.json");

    private final ObjectMapper objectMapper;

    public AdminSystemSettingsServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AdminSystemSettingsDto getSettings() {
        try {
            if (Files.exists(SETTINGS_PATH)) {
                return mergeWithDefaults(objectMapper.readValue(Files.readString(SETTINGS_PATH), AdminSystemSettingsDto.class));
            }
        } catch (Exception ignored) {
        }
        return defaultSettings();
    }

    @Override
    public AdminSystemSettingsDto saveSettings(AdminSystemSettingsDto settings) {
        AdminSystemSettingsDto merged = mergeWithDefaults(settings);
        merged.setUpdatedAt(LocalDateTime.now());
        writeSettings(merged);
        return merged;
    }

    @Override
    public AdminSystemSettingsDto saveLogo(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("Logo 文件不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件");
        }

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        AdminSystemSettingsDto settings = getSettings();
        settings.setLogoUrl("data:" + contentType + ";base64," + base64);
        return saveSettings(settings);
    }

    private void writeSettings(AdminSystemSettingsDto settings) {
        try {
            Files.createDirectories(SETTINGS_PATH.getParent());
            Files.writeString(SETTINGS_PATH, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings));
        } catch (Exception ex) {
            throw new IllegalStateException("保存系统设置失败", ex);
        }
    }

    private AdminSystemSettingsDto mergeWithDefaults(AdminSystemSettingsDto settings) {
        AdminSystemSettingsDto defaults = defaultSettings();
        if (settings == null) return defaults;
        if (settings.getSystemName() != null) defaults.setSystemName(settings.getSystemName());
        if (settings.getSupportEmail() != null) defaults.setSupportEmail(settings.getSupportEmail());
        if (settings.getFooterText() != null) defaults.setFooterText(settings.getFooterText());
        if (settings.getLogoUrl() != null) defaults.setLogoUrl(settings.getLogoUrl());
        if (settings.getStrongPasswordEnabled() != null) defaults.setStrongPasswordEnabled(settings.getStrongPasswordEnabled());
        if (settings.getMfaEnabled() != null) defaults.setMfaEnabled(settings.getMfaEnabled());
        if (settings.getLoginFailureLimit() != null) defaults.setLoginFailureLimit(settings.getLoginFailureLimit());
        if (settings.getSessionTimeoutMinutes() != null) defaults.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
        defaults.setUpdatedAt(settings.getUpdatedAt() != null ? settings.getUpdatedAt() : defaults.getUpdatedAt());
        return defaults;
    }

    private AdminSystemSettingsDto defaultSettings() {
        AdminSystemSettingsDto dto = new AdminSystemSettingsDto();
        dto.setSystemName("永升教育系统");
        dto.setSupportEmail("support@yongsheng.edu.cn");
        dto.setFooterText("© 2024 永升教育科技（北京）有限公司。保留所有权利。");
        dto.setLogoUrl("");
        dto.setStrongPasswordEnabled(Boolean.TRUE);
        dto.setMfaEnabled(Boolean.FALSE);
        dto.setLoginFailureLimit(5);
        dto.setSessionTimeoutMinutes(30);
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}

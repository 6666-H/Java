package com.study.english.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.english.dto.TenantSettingsDto;
import com.study.english.entity.Tenant;
import com.study.english.exception.BusinessException;
import com.study.english.service.TenantService;
import com.study.english.service.TenantSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class TenantSettingsServiceImpl implements TenantSettingsService {

    private static final Path SETTINGS_DIR = Paths.get("data", "tenant-settings");

    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    public TenantSettingsServiceImpl(TenantService tenantService, ObjectMapper objectMapper) {
        this.tenantService = tenantService;
        this.objectMapper = objectMapper;
    }

    @Override
    public TenantSettingsDto getSettings(String tenantId) {
        Tenant tenant = requiredTenant(tenantId);
        TenantSettingsStore store = readStore(tenantId);
        TenantSettingsDto dto = defaults(tenant);
        dto.setAddress(store.address != null ? store.address : dto.getAddress());
        dto.setLogoUrl(store.logoUrl != null ? store.logoUrl : dto.getLogoUrl());
        dto.setInterfaceLanguage(store.interfaceLanguage != null ? store.interfaceLanguage : dto.getInterfaceLanguage());
        dto.setTimezone(store.timezone != null ? store.timezone : dto.getTimezone());
        dto.setEmailReportEnabled(store.emailReportEnabled != null ? store.emailReportEnabled : dto.getEmailReportEnabled());
        dto.setNewStudentNotifyEnabled(store.newStudentNotifyEnabled != null ? store.newStudentNotifyEnabled : dto.getNewStudentNotifyEnabled());
        dto.setMfaEnabled(store.mfaEnabled != null ? store.mfaEnabled : dto.getMfaEnabled());
        dto.setUpdatedAt(store.updatedAt != null ? store.updatedAt : dto.getUpdatedAt());
        return dto;
    }

    @Override
    public TenantSettingsDto saveSettings(String tenantId, TenantSettingsDto settings) {
        Tenant tenant = requiredTenant(tenantId);
        tenant.setName(trimToNull(settings.getTenantName()) != null ? settings.getTenantName().trim() : tenant.getName());
        tenant.setContactPhone(trimToNull(settings.getContactPhone()));
        tenantService.updateById(tenant);

        TenantSettingsStore store = readStore(tenantId);
        store.address = trimToNull(settings.getAddress());
        store.logoUrl = trimToNull(settings.getLogoUrl());
        store.interfaceLanguage = trimToNull(settings.getInterfaceLanguage());
        store.timezone = trimToNull(settings.getTimezone());
        store.emailReportEnabled = settings.getEmailReportEnabled();
        store.newStudentNotifyEnabled = settings.getNewStudentNotifyEnabled();
        store.mfaEnabled = settings.getMfaEnabled();
        store.updatedAt = LocalDateTime.now();
        writeStore(tenantId, store);
        return getSettings(tenantId);
    }

    @Override
    public TenantSettingsDto saveLogo(String tenantId, MultipartFile file) throws Exception {
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

        TenantSettingsStore store = readStore(tenantId);
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        store.logoUrl = "data:" + contentType + ";base64," + base64;
        store.updatedAt = LocalDateTime.now();
        writeStore(tenantId, store);
        return getSettings(tenantId);
    }

    private Tenant requiredTenant(String tenantId) {
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) throw new BusinessException("租户不存在");
        return tenant;
    }

    private TenantSettingsDto defaults(Tenant tenant) {
        TenantSettingsDto dto = new TenantSettingsDto();
        dto.setTenantId(tenant.getId());
        dto.setTenantName(tenant.getName());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setAddress("北京市朝阳区建国路 88 号 永盛大厦 12 层");
        dto.setLogoUrl("");
        dto.setInterfaceLanguage("zh-CN");
        dto.setTimezone("Asia/Shanghai");
        dto.setEmailReportEnabled(Boolean.TRUE);
        dto.setNewStudentNotifyEnabled(Boolean.FALSE);
        dto.setMfaEnabled(Boolean.FALSE);
        dto.setExpireTime(tenant.getExpireTime());
        long daysRemaining = tenant.getExpireTime() == null ? 0 : ChronoUnit.DAYS.between(LocalDateTime.now(), tenant.getExpireTime());
        dto.setDaysRemaining(daysRemaining);
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private TenantSettingsStore readStore(String tenantId) {
        Path path = resolvePath(tenantId);
        try {
            if (Files.exists(path)) {
                return objectMapper.readValue(Files.readString(path), TenantSettingsStore.class);
            }
        } catch (Exception ignored) {
        }
        return new TenantSettingsStore();
    }

    private void writeStore(String tenantId, TenantSettingsStore store) {
        try {
            Files.createDirectories(SETTINGS_DIR);
            Files.writeString(resolvePath(tenantId), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(store));
        } catch (Exception ex) {
            throw new IllegalStateException("保存机构设置失败", ex);
        }
    }

    private Path resolvePath(String tenantId) {
        return SETTINGS_DIR.resolve(tenantId + ".json");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class TenantSettingsStore {
        public String address;
        public String logoUrl;
        public String interfaceLanguage;
        public String timezone;
        public Boolean emailReportEnabled;
        public Boolean newStudentNotifyEnabled;
        public Boolean mfaEnabled;
        public LocalDateTime updatedAt;
    }
}

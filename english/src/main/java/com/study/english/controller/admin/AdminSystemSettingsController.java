package com.study.english.controller.admin;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.AdminSystemSettingsDto;
import com.study.english.exception.BusinessException;
import com.study.english.service.AdminSystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSystemSettingsController {

    private final AdminSystemSettingsService adminSystemSettingsService;

    @GetMapping
    public Result<AdminSystemSettingsDto> getSettings() {
        ensureSuperAdmin();
        return Result.ok(adminSystemSettingsService.getSettings());
    }

    @PutMapping
    public Result<AdminSystemSettingsDto> saveSettings(@RequestBody AdminSystemSettingsDto settings) {
        ensureSuperAdmin();
        return Result.ok(adminSystemSettingsService.saveSettings(settings));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminSystemSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) throws Exception {
        ensureSuperAdmin();
        return Result.ok(adminSystemSettingsService.saveLogo(file));
    }

    private void ensureSuperAdmin() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
    }
}

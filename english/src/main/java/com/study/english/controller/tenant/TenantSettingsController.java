package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.TenantSettingsDto;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.TenantSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tenant/settings")
@RequiredArgsConstructor
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;

    @GetMapping
    public Result<TenantSettingsDto> getSettings() {
        return Result.ok(tenantSettingsService.getSettings(requiredTenantId()));
    }

    @PutMapping
    public Result<TenantSettingsDto> saveSettings(@RequestBody TenantSettingsDto settings) {
        return Result.ok(tenantSettingsService.saveSettings(requiredTenantId(), settings));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TenantSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(tenantSettingsService.saveLogo(requiredTenantId(), file));
    }

    private String requiredTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        String role = TenantContext.getRole();
        if (!User.ROLE_ORG_ADMIN.equals(role) && !User.ROLE_TENANT_ADMIN.equals(role)) {
            throw new BusinessException("仅校长/租户管理员可操作");
        }
        return tenantId;
    }
}

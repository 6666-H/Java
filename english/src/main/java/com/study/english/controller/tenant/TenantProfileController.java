package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.OrgProfileDto;
import com.study.english.dto.UpdateOrgProfileRequest;
import com.study.english.dto.UpdatePasswordRequest;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/profile")
@RequiredArgsConstructor
public class TenantProfileController {

    private final UserService userService;

    @GetMapping
    public Result<OrgProfileDto> getProfile() {
        Long userId = TenantContext.getUserId();
        String tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) throw new BusinessException("未登录");
        User user = userService.getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) throw new BusinessException("账号不存在");
        OrgProfileDto dto = userService.toOrgProfile(user);
        return Result.ok(dto);
    }

    @PutMapping
    public Result<OrgProfileDto> updateProfile(@Valid @RequestBody UpdateOrgProfileRequest req) {
        Long userId = TenantContext.getUserId();
        String tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) throw new BusinessException("未登录");
        return Result.ok(userService.updateOrgProfile(tenantId, userId, req));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest req) {
        Long userId = TenantContext.getUserId();
        String tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) throw new BusinessException("未登录");
        userService.updateOrgPassword(tenantId, userId, req);
        return Result.ok();
    }
}

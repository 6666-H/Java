package com.study.english.controller;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.LoginRequest;
import com.study.english.dto.LoginResponse;
import com.study.english.dto.UpdatePasswordRequest;
import com.study.english.entity.Tenant;
import com.study.english.entity.User;
import com.study.english.security.JwtUtils;
import org.springframework.util.StringUtils;
import com.study.english.service.TenantService;
import com.study.english.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 认证接口：登录。校长/学生账号随机构有效期；到期或机构被禁用时提示「账号到期，续签请联系管理员」。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String MSG_ACCOUNT_EXPIRED = "账号到期，续签请联系管理员";
    private static final String MSG_ROLE_MISMATCH = "您选择的登录入口与账号身份不符，请选择正确的入口登录";

    private final UserService userService;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 登录。
     *
     * @param req 请求体：username（必填）、password（必填）、tenantId（可选，超级管理员可不传）
     * @return Result.data 含 token、userId、tenantId、username、role；失败返回 AUTH_FAIL
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String tenantId = req.getTenantId();
        if (tenantId != null && (tenantId.isBlank() || "null".equalsIgnoreCase(tenantId))) {
            tenantId = null;
        }
        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        String password = req.getPassword() != null ? req.getPassword() : "";
        String requestedRole = StringUtils.hasText(req.getRole()) ? req.getRole().trim() : null;

        if ((User.ROLE_STUDENT.equals(requestedRole) || User.ROLE_ORG_ADMIN.equals(requestedRole))
                && (tenantId == null || tenantId.isBlank())) {
            return Result.fail("AUTH_FAIL", "请填写机构代码");
        }
        User user;
        if (User.ROLE_STUDENT.equals(requestedRole)) {
            user = userService.getByTenantAndUsername(tenantId.trim(), username);
        } else if (User.ROLE_ORG_ADMIN.equals(requestedRole)) {
            user = userService.getByTenantAndUsername(tenantId.trim(), username);
        } else if (User.ROLE_SUPER_ADMIN.equals(requestedRole)) {
            user = userService.getByUsernameAndRole(username, User.ROLE_SUPER_ADMIN);
        } else {
            user = userService.getByTenantAndUsername(tenantId, username);
        }

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return Result.fail("AUTH_FAIL", "用户名或密码错误");
        }
        if (requestedRole != null && !requestedRole.equals(user.getRole())) {
            return Result.fail("AUTH_FAIL", MSG_ROLE_MISMATCH);
        }
        if (user.getStatus() != null && user.getStatus() != User.STATUS_ENABLED) {
            return Result.fail("AUTH_FAIL", "账号已禁用");
        }
        if (user.getTenantId() != null && !user.getTenantId().isBlank()) {
            Tenant tenant = tenantService.getById(user.getTenantId());
            if (tenant == null) {
                return Result.fail("AUTH_FAIL", MSG_ACCOUNT_EXPIRED);
            }
            if (tenant.getStatus() == null || tenant.getStatus() != Tenant.STATUS_ENABLED) {
                return Result.fail("AUTH_FAIL", MSG_ACCOUNT_EXPIRED);
            }
            LocalDateTime expire = tenant.getExpireTime();
            if (expire != null && expire.isBefore(LocalDateTime.now())) {
                return Result.fail("AUTH_FAIL", MSG_ACCOUNT_EXPIRED);
            }
        }
        int tokenVersion = userService.incrementTokenVersion(user.getId());
        String token = jwtUtils.generate(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getRole(),
                tokenVersion);
        return Result.ok(new LoginResponse(
                token,
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getRole(),
                user.getMustChangePwd() != null && user.getMustChangePwd() == 1,
                user.getNickname(),
                user.getAvatar()));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest req) {
        Long userId = TenantContext.getUserId();
        if (userId == null) return Result.fail("AUTH_FAIL", "未登录");
        userService.updateOwnPassword(TenantContext.getTenantId(), userId, req);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = TenantContext.getUserId();
        if (userId != null) {
            userService.incrementTokenVersion(userId);
        }
        return Result.ok();
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh() {
        Long userId = TenantContext.getUserId();
        if (userId == null) return Result.fail("AUTH_FAIL", "未登录");
        User user = userService.getById(userId);
        if (user == null) return Result.fail("AUTH_FAIL", "账号不存在");
        int tokenVersion = userService.incrementTokenVersion(userId);
        String token = jwtUtils.generate(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getRole(),
                tokenVersion);
        return Result.ok(new LoginResponse(
                token,
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getRole(),
                user.getMustChangePwd() != null && user.getMustChangePwd() == 1,
                user.getNickname(),
                user.getAvatar()));
    }
}

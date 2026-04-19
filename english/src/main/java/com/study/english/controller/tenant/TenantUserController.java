package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.BatchCreateUserRequest;
import com.study.english.dto.CreateUserRequest;
import com.study.english.dto.ResetStudentPasswordRequest;
import com.study.english.dto.UpdateTenantUserRequest;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理员：在额度内创建学生/子账号。tenantId 从 JWT 上下文取。
 */
@RestController
@RequestMapping("/api/tenant/users")
@RequiredArgsConstructor
public class TenantUserController {

    private final UserService userService;

    /**
     * 获取当前租户下的用户列表。支持 role 筛选（如 STUDENT 仅返回学生）。
     */
    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String role) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可查看");
        }
        List<User> list = userService.listByTenantId(tenantId, role);
        list.forEach(u -> { u.setPasswordHash(null); u.setPasswordPlain(null); });
        return Result.ok(list);
    }

    /**
     * 在额度内创建单个学生或子账号。
     *
     * @param req 请求体：username（2-32位字母数字下划线）、password、realName、role（STUDENT/ORG_ADMIN）
     * @return Result.data 创建成功的用户
     */
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody CreateUserRequest req) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("租户上下文缺失");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole()) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅校长/租户管理员可创建用户");
        }
        if (!User.ROLE_STUDENT.equals(req.getRole()) && !User.ROLE_ORG_ADMIN.equals(req.getRole())) {
            req.setRole(User.ROLE_STUDENT);
        }
        return Result.ok(userService.createUser(tenantId, req));
    }

    /**
     * 批量创建学生账号，如 test01、test02...。
     *
     * @param req 请求体：prefix（用户名前缀）、count（1-500）、password（统一密码）
     * @return Result.data 创建成功的用户列表
     */
    @PostMapping("/batch")
    public Result<List<User>> batchCreate(@Valid @RequestBody BatchCreateUserRequest req) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("租户上下文缺失");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole()) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅校长/租户管理员可批量创建");
        }
        return Result.ok(userService.batchCreateUsers(tenantId, req));
    }

    /**
     * 软删除学生账号。仅可删除本机构下的学生，不可删除校长账号。
     */
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("租户上下文缺失");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole()) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅校长/租户管理员可删除学生");
        }
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!tenantId.equals(user.getTenantId())) throw new BusinessException("无权删除其他机构的账号");
        if (User.ROLE_ORG_ADMIN.equals(user.getRole())) throw new BusinessException("不可删除校长账号");
        userService.softDeleteUser(userId);
        return Result.ok();
    }

    @PutMapping("/{userId}")
    public Result<User> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateTenantUserRequest req) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("租户上下文缺失");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole()) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅校长/租户管理员可更新学生");
        }
        return Result.ok(userService.updateStudent(tenantId, userId, req));
    }

    @PutMapping("/{userId}/password")
    public Result<Void> resetPassword(@PathVariable Long userId, @Valid @RequestBody ResetStudentPasswordRequest req) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("租户上下文缺失");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole()) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅校长/租户管理员可重置密码");
        }
        userService.resetUserPassword(tenantId, TenantContext.getUserId(), userId, req.getNewPassword());
        return Result.ok();
    }
}

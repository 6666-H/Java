package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 校长端：数据大屏 - 连续 N 天未打卡学生，方便提醒续费或学习。
 */
@RestController
@RequestMapping("/api/tenant/students")
@RequiredArgsConstructor
public class TenantStudentsController {

    private final UserService userService;

    /**
     * 连续 N 天未打卡的学生列表，便于提醒续费或学习。
     *
     * @param days 未活跃天数阈值，默认 7
     * @return Result.data 用户列表（含 lastActiveAt）
     */
    @GetMapping("/inactive")
    public Result<List<User>> inactive(@RequestParam(defaultValue = "7") int days) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可查看");
        }
        return Result.ok(userService.listInactiveStudents(tenantId, days));
    }
}

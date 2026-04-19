package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.CreateStudyTaskRequest;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudyTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/tasks")
@RequiredArgsConstructor
public class TenantTaskController {

    private final StudyTaskService studyTaskService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        String tenantId = TenantContext.getTenantId();
        requireOrgRole();
        return Result.ok(studyTaskService.listTasksForTenant(tenantId));
    }

    @PostMapping
    public Result<List<?>> create(@Valid @RequestBody CreateStudyTaskRequest req) {
        String tenantId = TenantContext.getTenantId();
        requireOrgRole();
        return Result.ok(studyTaskService.createTasks(tenantId, TenantContext.getUserId(), req));
    }

    @DeleteMapping("/{taskId}")
    public Result<Void> cancel(@PathVariable Long taskId) {
        String tenantId = TenantContext.getTenantId();
        requireOrgRole();
        studyTaskService.cancelTask(tenantId, taskId);
        return Result.ok();
    }

    private void requireOrgRole() {
        if (TenantContext.getTenantId() == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长可操作");
        }
    }
}

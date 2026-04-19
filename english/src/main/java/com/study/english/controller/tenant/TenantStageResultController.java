package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudyStageResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/stage-results")
@RequiredArgsConstructor
public class TenantStageResultController {

    private final StudyStageResultService studyStageResultService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "100") int limit) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可查看");
        }
        return Result.ok(studyStageResultService.listTenantResults(tenantId, studentId, startDate, endDate, limit));
    }
}

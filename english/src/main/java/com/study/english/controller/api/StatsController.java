package com.study.english.controller.api;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.exception.BusinessException;
import com.study.english.service.ProductStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 教材/单元模式 - 统计 API
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final ProductStudyService productStudyService;

    @GetMapping("/today")
    public Result<Map<String, Object>> today() {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(productStudyService.getTodayStats(tenantId, userId));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(productStudyService.getOverviewStats(tenantId, userId));
    }
}

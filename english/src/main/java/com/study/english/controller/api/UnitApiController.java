package com.study.english.controller.api;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.UnitProgressDto;
import com.study.english.entity.Unit;
import com.study.english.exception.BusinessException;
import com.study.english.service.ProductStudyService;
import com.study.english.service.UnitProgressService;
import com.study.english.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitApiController {

    private final UnitService unitService;
    private final UnitProgressService unitProgressService;
    private final ProductStudyService productStudyService;

    @GetMapping
    public Result<List<Unit>> listByBook(@RequestParam Long bookId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        List<Unit> units = unitService.lambdaQuery()
                .eq(Unit::getBookId, bookId)
                .orderByAsc(Unit::getSortOrder)
                .list();
        return Result.ok(units);
    }

    @GetMapping("/{id}/progress")
    public Result<com.study.english.dto.UnitProgressDetailDto> progressByUnit(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(productStudyService.getUnitProgress(tenantId, userId, id));
    }

    @GetMapping("/progress")
    public Result<List<UnitProgressDto>> progressByBook(@RequestParam Long bookId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(unitProgressService.getUnitProgressList(tenantId, userId, bookId));
    }
}

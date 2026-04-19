package com.study.english.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.Unit;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.service.UnitService;
import com.study.english.service.WordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 超级管理员：单元的查看与增删改（按书本）。
 */
@RestController
@RequestMapping("/api/admin/units")
@RequiredArgsConstructor
public class AdminUnitController {

    private final UnitService unitService;
    private final WordService wordService;

    @GetMapping
    public Result<List<Unit>> list(@RequestParam Long bookId) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        List<Unit> list = unitService.list(new LambdaQueryWrapper<Unit>()
                .eq(Unit::getBookId, bookId)
                .orderByAsc(Unit::getSortOrder));
        return Result.ok(list);
    }

    @PostMapping
    public Result<Unit> create(@RequestBody UnitCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (req.getBookId() == null) throw new BusinessException("bookId 不能为空");
        Unit unit = new Unit();
        unit.setBookId(req.getBookId());
        unit.setName(req.getName() != null ? req.getName() : "未命名");
        unit.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        unitService.save(unit);
        return Result.ok(unit);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UnitCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Unit unit = unitService.getById(id);
        if (unit == null) throw new BusinessException("单元不存在");
        if (req.getName() != null) unit.setName(req.getName());
        if (req.getSortOrder() != null) unit.setSortOrder(req.getSortOrder());
        unitService.updateById(unit);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (unitService.getById(id) == null) throw new BusinessException("单元不存在");
        wordService.remove(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, id));
        unitService.removeById(id);
        return Result.ok();
    }

    @Data
    public static class UnitCreateRequest {
        private Long bookId;
        private String name;
        private Integer sortOrder;
    }
}

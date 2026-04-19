package com.study.english.controller.admin;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.SyncRequest;
import com.study.english.exception.BusinessException;
import com.study.english.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final SyncService syncService;

    /**
     * JSON 批量同步词库：Book -> Unit -> Word，存在则更新、不存在则插入（幂等）。
     *
     * @param req 请求体：books 数组，每本书含 units，每单元含 words。word 为唯一标识。
     * @return Result.data { booksCount, unitsCount, wordsCount } 同步数量统计
     */
    @PostMapping
    public Result<SyncService.SyncResult> sync(@Valid @RequestBody SyncRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可执行资源同步");
        }
        return Result.ok(syncService.sync(req));
    }
}

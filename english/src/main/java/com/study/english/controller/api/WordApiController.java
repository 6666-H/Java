package com.study.english.controller.api;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.WordFeedbackRequest;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudentWordProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学习端：下一个单词（艾宾浩斯优先级）、反馈更新。
 */
@RestController
@RequestMapping("/api/word")
@RequiredArgsConstructor
public class WordApiController {

    private final StudentWordProgressService progressService;

    /**
     * 获取本单元下一个要学的单词（艾宾浩斯优先级：先错题，再新词）。
     *
     * @param unitId 单元 ID
     * @return Result.data 下一个单词，学完则可能为 null
     */
    @GetMapping("/next")
    public Result<Word> next(@RequestParam Long unitId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        Word word = progressService.getNextWord(tenantId, userId, unitId);
        return Result.ok(word);
    }

    /**
     * 单词反馈：认识/不认识。认识则提升 mastery_level 并设置 next_review_time；不认识则重置。
     *
     * @param req 请求体：wordId（必填）、known（true=认识，false=不认识）
     * @return Result 无 data
     */
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody WordFeedbackRequest req) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        progressService.submitFeedback(tenantId, userId, req.getWordId(), Boolean.TRUE.equals(req.getKnown()));
        return Result.ok();
    }
}

package com.study.english.controller.api;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生端：/api/words/next 优先推送过期错题；/api/words/check 拼写实时比对（每输入一字母可调）。
 */
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordsController {

    private final StudentWordProgressService progressService;
    private final WordService wordService;

    /**
     * 获取本单元下一个要学的单词：优先 next_review_time <= 当前时间的错题，再新词。
     *
     * @param unitId 单元 ID
     * @return Result.data 下一个单词，若本单元已学完则可能为 null
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
     * 拼写实时比对：忽略大小写与前后空格。每输入一字母可调用。
     *
     * @param wordId 单词 ID
     * @param input  用户当前输入内容，可为空
     * @return Result.data { match: 是否为正确单词的前缀或全词, complete: 是否完整匹配, correct: 同 complete }
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam Long wordId, @RequestParam(required = false) String input) {
        Word word = wordService.getById(wordId);
        if (word == null) throw new BusinessException("单词不存在");
        String expected = word.getWord() == null ? "" : word.getWord().trim().toLowerCase();
        String actual = (input == null ? "" : input.trim().toLowerCase());
        boolean complete = expected.equals(actual);
        boolean match = expected.startsWith(actual);
        return Result.ok(Map.<String, Object>of("match", match, "complete", complete, "correct", complete));
    }
}

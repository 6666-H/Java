package com.study.english.controller.api;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.CompleteModeRequest;
import com.study.english.dto.CheckSpellingRequest;
import com.study.english.dto.CheckSpellingResult;
import com.study.english.dto.StudyQuestionOptionsDto;
import com.study.english.dto.StudyQueueItemDto;
import com.study.english.dto.StudySubmitResult;
import com.study.english.dto.StudyStatsDto;
import com.study.english.dto.StudySubmitRequest;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.service.ErrorLogService;
import com.study.english.service.ErrorReviewCompleteService;
import com.study.english.service.ProductStudyService;
import com.study.english.service.StudentWordModeProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.StudentUnitModeService;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 背单词核心：学习反馈、错题队列、智能复习列表、拼写校验。
 */
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudentWordProgressService progressService;
    private final ErrorLogService errorLogService;
    private final ErrorReviewCompleteService errorReviewCompleteService;
    private final WordService wordService;
    private final StudentUnitModeService unitModeService;
    private final StudyLogService studyLogService;
    private final StudentWordModeProgressService modeProgressService;
    private final ProductStudyService productStudyService;

    /**
     * 学习反馈提交。
     * 1) 教材/单元模式：body 含 isCorrect 时走产品记忆算法，返回 userWord
     * 2) 原有模式：body 含 feedbackType 时走原逻辑
     */
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        Object wordIdObj = body.get("wordId");
        if (wordIdObj == null) throw new BusinessException("wordId 不能为空");
        Long wordId = wordIdObj instanceof Number ? ((Number) wordIdObj).longValue() : Long.parseLong(wordIdObj.toString());

        Object isCorrectObj = body.get("isCorrect");
        if (isCorrectObj != null) {
            boolean isCorrect = Boolean.TRUE.equals(isCorrectObj) || "true".equalsIgnoreCase(String.valueOf(isCorrectObj));
            String studyMode = resolveJourneyStudyMode(body);
            String userInput = body.get("userInput") == null ? null : String.valueOf(body.get("userInput"));
            return Result.ok(productStudyService.submitAnswer(tenantId, userId, wordId, isCorrect, studyMode, userInput));
        }
        StudySubmitRequest req = new StudySubmitRequest();
        req.setWordId(wordId);
        req.setFeedbackType((String) body.get("feedbackType"));
        req.setMode((String) body.get("mode"));
        req.setErrorType((String) body.get("errorType"));
        req.setUserInput((String) body.get("userInput"));
        if (req.getFeedbackType() == null) throw new BusinessException("feedbackType 或 isCorrect 不能为空");
        StudySubmitResult submitResult = progressService.submitStudyFeedback(
                tenantId, userId, req.getWordId(), req.getFeedbackType(), req.getErrorType(), req.getMode());
        studyLogService.logStudy(tenantId, userId, req.getWordId(), req.getMode(), req.getFeedbackType(), req.getUserInput());
        return Result.ok(submitResult);
    }

    private String resolveJourneyStudyMode(Map<String, Object> body) {
        String stageKey = body.get("stageKey") == null ? "" : String.valueOf(body.get("stageKey")).trim().toLowerCase();
        String questionType = body.get("questionType") == null ? "" : String.valueOf(body.get("questionType")).trim().toLowerCase();
        String mode = body.get("mode") == null ? "" : String.valueOf(body.get("mode")).trim().toLowerCase();
        if (!stageKey.isBlank() && !questionType.isBlank()) {
            return "journey." + stageKey + "." + questionType;
        }
        if (!questionType.isBlank()) {
            return "journey." + questionType;
        }
        if (!mode.isBlank()) {
            return mode;
        }
        return "journey.quiz";
    }

    /**
     * 标记本单元某类型练习已完成。只有四种类型都完成且错题复习完，本单元才算完成。
     *
     * @param req 请求体：unitId（必填）、mode（flashcard/eng_ch/ch_eng/spell）
     */
    @PostMapping("/complete_mode")
    public Result<Void> completeMode(@Valid @RequestBody CompleteModeRequest req) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        int incomplete = modeProgressService.countIncompleteWords(tenantId, userId, req.getUnitId(), req.getMode());
        if (incomplete > 0 && !modeProgressService.hasModeProgressRecords(tenantId, userId, req.getUnitId(), req.getMode())) {
            modeProgressService.backfillModeCompleted(tenantId, userId, req.getUnitId(), req.getMode());
            incomplete = modeProgressService.countIncompleteWords(tenantId, userId, req.getUnitId(), req.getMode());
        }
        if (incomplete > 0) {
            throw new BusinessException("当前学习类型仍有未完成单词，暂不能标记完成");
        }
        unitModeService.recordModeComplete(tenantId, userId, req.getUnitId(), req.getMode());
        return Result.ok();
    }

    /**
     * 智能复习列表：优先错题，再今日复习，最后新词。
     *
     * @param unitId 单元 ID
     * @param limit  返回数量上限，默认 20
     * @return Result.data 单词列表，按优先级排序
     */
    @GetMapping("/next_list")
    public Result<List<Word>> nextList(@RequestParam Long unitId,
                                       @RequestParam(required = false, defaultValue = "20") int limit,
                                       @RequestParam(required = false) String mode) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        List<Word> list = progressService.getNextList(tenantId, userId, unitId, limit, mode);
        return Result.ok(list);
    }

    @GetMapping("/question_options")
    public Result<StudyQuestionOptionsDto> questionOptions(@RequestParam Long wordId,
                                                           @RequestParam String mode) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(progressService.getQuestionOptions(tenantId, userId, wordId, mode));
    }

    /**
     * 拼写校验：忽略大小写与前后空格。不一致则增加该词错误权重并记入 error_log。
     *
     * @param req 请求体：wordId（必填）、userInput（用户输入）
     * @return Result.data correct=true 拼写正确，false 时 message 为错误提示
     */
    @PostMapping("/check_spelling")
    public Result<CheckSpellingResult> checkSpelling(@Valid @RequestBody CheckSpellingRequest req) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        CheckSpellingResult result = progressService.checkSpelling(tenantId, userId, req.getWordId(),
                req.getUserInput() != null ? req.getUserInput() : "");
        return Result.ok(result);
    }

    /**
     * 本单元四种学习类型完成状态：{ flashcard, eng_ch, ch_eng, spell } 均为 true 时表示本单元新词全部学完。
     *
     * @param unitId 单元 ID
     * @return Result.data 四个布尔字段，分别表示看词识义、看英语选中文、看中文选英语、看中文拼写是否已完成
     */
    /**
     * 本单元各学习类型未完成/全部数量：{ flashcard: {total, incomplete}, eng_ch, ch_eng, spell }
     */
    @GetMapping("/mode_stats")
    public Result<Map<String, Map<String, Integer>>> modeStats(@RequestParam Long unitId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(modeProgressService.getModeStats(tenantId, userId, unitId));
    }

    @GetMapping("/unit_mode_completion")
    public Result<Map<String, Boolean>> unitModeCompletion(@RequestParam Long unitId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(unitModeService.getModeCompletion(tenantId, userId, unitId));
    }

    /**
     * 本单元今日任务统计：待复习错题数、待学习新词数、总词数、是否全部完成。
     *
     * @param unitId 单元 ID
     * @return Result.data reviewCount 待复习错题数，newCount 待学习新词数，totalCount 总词数，completed 是否全部完成（新词学完且错题也学完）
     */
    @GetMapping("/stats")
    public Result<StudyStatsDto> stats(@RequestParam Long unitId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(progressService.getUnitStats(tenantId, userId, unitId));
    }

    /**
     * 近期错题列表（按最近错误时间倒序）。
     *
     * @param days  统计最近 N 天内的错题，默认 7
     * @param limit 返回数量上限，默认 50
     * @return Result.data 错题单词列表
     */
    @GetMapping("/recent_errors")
    public Result<List<Word>> recentErrors(@RequestParam(defaultValue = "7") int days, @RequestParam(defaultValue = "50") int limit) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        List<Word> list = progressService.getRecentErrorWords(tenantId, userId, days, limit);
        return Result.ok(list);
    }

    @GetMapping("/weak_words")
    public Result<List<Word>> weakWords(@RequestParam(required = false) Long unitId,
                                        @RequestParam(defaultValue = "20") int limit) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(progressService.getWeakWords(tenantId, userId, unitId, limit));
    }

    /**
     * 学习热力图：过去 30 天每天的复习/学习单词数，含 total、consecutiveDays。
     *
     * @return Result.data { items: [{date, count}], total, consecutiveDays }
     */
    @GetMapping("/heatmap")
    public Result<java.util.Map<String, Object>> heatmap() {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(progressService.getStudyHeatmapWithStats(tenantId, userId));
    }

    /**
     * 已掌握单词列表（分页），用于数据看板点击「累计掌握单词」查看。
     *
     * @return Result.data { list: Word[], total }
     */
    @GetMapping("/mastered_words")
    public Result<java.util.Map<String, Object>> masteredWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(progressService.getMasteredWords(tenantId, userId, page, pageSize));
    }

    /**
     * 按日期统计错题数量与类型，用于错题列表。每项含 date、totalCount、typeCounts。
     *
     * @param days   统计最近 N 天，默认 90
     * @param search 搜索关键词（日期包含），可选
     * @return Result.data [{ date, totalCount, typeCounts: { DONT_KNOW: n, ... } }, ...]，日期倒序
     */
    @GetMapping("/error_stats_by_date")
    public Result<List<Map<String, Object>>> errorStatsByDate(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(required = false) String search) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getErrorStatsByDate(tenantId, userId, days, search));
    }

    /**
     * 按日期统计错题数量（支持时间、书本、单元筛选），用于首页错误归纳表格。
     *
     * @param days      默认统计天数，当未传 startDate/endDate 时生效
     * @param startDate 开始日期 yyyy-MM-dd，可选
     * @param endDate   结束日期 yyyy-MM-dd，可选
     * @param bookId    书本 ID，可选
     * @param unitId    单元 ID，可选
     * @return Result.data [{ date, count }, ...]，日期倒序
     */
    @GetMapping("/error_stats_filtered")
    public Result<List<Map<String, Object>>> errorStatsFiltered(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long unitId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getErrorStatsByDateFiltered(
                tenantId, userId, days, startDate, endDate, bookId, unitId));
    }

    /**
     * 智能归纳列表（待完成）：后端分页+搜索，前端仅展示。
     * @return Result.data { list, total, hasPending, firstDate }
     */
    @GetMapping("/smart_summary_list")
    public Result<Map<String, Object>> smartSummaryList(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getSmartSummaryList(
                tenantId, userId, days, startDate, endDate, bookId, unitId, search, page, pageSize));
    }

    /**
     * 历史错题列表（已完成）：后端分页+搜索，前端仅展示。
     * @return Result.data { list, total }
     */
    @GetMapping("/history_error_list")
    public Result<Map<String, Object>> historyErrorList(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getHistoryErrorList(
                tenantId, userId, days, startDate, endDate, bookId, unitId, search, page, pageSize));
    }

    /**
     * 有错题的日期列表（用于错题本按日期展示）。
     *
     * @param days 统计最近 N 天，默认 30
     * @return Result.data 日期字符串列表，格式 yyyy-MM-dd，倒序
     */
    @GetMapping("/error_dates")
    public Result<List<String>> errorDates(@RequestParam(defaultValue = "30") int days) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getErrorDates(tenantId, userId, days));
    }

    /**
     * 某日错题对应的完整单词列表（用于重复训练）。
     *
     * @param date 日期，格式 yyyy-MM-dd
     * @return Result.data 单词列表，按错误时间倒序
     */
    @GetMapping("/words_by_error_date")
    public Result<List<Word>> wordsByErrorDate(@RequestParam String date) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        List<Long> wordIds = errorLogService.getWordIdsByDate(tenantId, userId, date);
        if (wordIds == null || wordIds.isEmpty()) return Result.ok(new ArrayList<>());
        List<Word> words = wordService.listByIds(wordIds);
        words.sort((a, b) -> Integer.compare(wordIds.indexOf(a.getId()), wordIds.indexOf(b.getId())));
        return Result.ok(words);
    }

    /**
     * 标记某日错题复习已完成（智能归纳完成时调用）。
     *
     * @param date 日期，格式 yyyy-MM-dd
     */
    @PostMapping("/error_review_complete")
    public Result<Void> markErrorReviewComplete(@RequestParam String date,
                                                @RequestParam(required = false) String errorType) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        int weakCount = errorLogService.countWeakWordsForDate(tenantId, userId, date, errorType);
        if (weakCount > 0) {
            throw new BusinessException("当前仍有未掌握的错题，完成条件未满足");
        }
        errorReviewCompleteService.markComplete(tenantId, userId, date, errorType);
        return Result.ok();
    }

    /**
     * 某日的错题明细（按类型分组由前端处理）。
     *
     * @param date 日期，格式 yyyy-MM-dd
     * @return Result.data [{ wordId, word, meaning, errorType }, ...]
     */
    @GetMapping("/errors_by_date")
    public Result<List<Map<String, Object>>> errorsByDate(@RequestParam String date) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getErrorsByDate(tenantId, userId, date));
    }

    /**
     * 某日错题按类型分组（后端处理，前端仅展示）。
     *
     * @param date 日期，格式 yyyy-MM-dd
     * @return Result.data [{ type, items: [...] }]，按数量降序
     */
    @GetMapping("/errors_by_date_grouped")
    public Result<List<Map<String, Object>>> errorsByDateGrouped(@RequestParam String date) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(errorLogService.getErrorsByDateGrouped(tenantId, userId, date));
    }

    /**
     * 教材/单元模式 - 学习队列：复习优先 + 新词（每日20），中文→选英文四选一
     */
    @GetMapping("/queue")
    public Result<Map<String, Object>> queue(@RequestParam Long unitId, @RequestParam(required = false, defaultValue = "50") int limit) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        List<StudyQueueItemDto> items = productStudyService.getUnitQueue(tenantId, userId, unitId, limit);
        return Result.ok(Map.of("items", items, "total", items.size()));
    }

    /**
     * 教材/单元模式 - 全局复习队列
     */
    @GetMapping("/review-queue")
    public Result<Map<String, Object>> reviewQueue(@RequestParam(required = false, defaultValue = "50") int limit) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        List<StudyQueueItemDto> items = productStudyService.getReviewQueue(tenantId, userId, limit);
        return Result.ok(Map.of("items", items, "total", items.size()));
    }

    /**
     * 本单元全部单词（用于「复习本单元」：重新学习本单元单词，不分类型，顺序由前端随机）。
     *
     * @param unitId 单元 ID
     * @return Result.data 该单元下所有单词列表
     */
    @GetMapping("/unit_words")
    public Result<List<Word>> unitWords(@RequestParam Long unitId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        List<Word> list = wordService.list(new LambdaQueryWrapper<Word>()
                .eq(Word::getUnitId, unitId)
                .orderByAsc(Word::getSortOrder));
        return Result.ok(list != null ? list : new ArrayList<>());
    }

    @GetMapping("/incomplete_words")
    public Result<List<Word>> incompleteWords(@RequestParam Long unitId,
                                              @RequestParam String mode,
                                              @RequestParam(required = false, defaultValue = "100") int limit) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(modeProgressService.getIncompleteWords(tenantId, userId, unitId, mode, limit));
    }
}

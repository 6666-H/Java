package com.study.english.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.StudentLastStudyRequest;
import com.study.english.dto.StudentProfileUpdateRequest;
import com.study.english.dto.StudentStatsDto;
import com.study.english.dto.StudentUnitWordDto;
import com.study.english.dto.StudentWordbookItemDto;
import com.study.english.dto.SaveStageResultRequest;
import com.study.english.dto.UpdatePasswordRequest;
import com.study.english.entity.Book;
import com.study.english.entity.Notification;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.Unit;
import com.study.english.entity.User;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.service.BookService;
import com.study.english.service.NotificationService;
import com.study.english.service.ProductStudyService;
import com.study.english.service.StudySessionService;
import com.study.english.service.StudyStageResultService;
import com.study.english.service.StudyLogService;
import com.study.english.service.StudyTaskService;
import com.study.english.service.UnitService;
import com.study.english.service.UserService;
import com.study.english.service.WordService;
import com.study.english.service.StudentWordProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudyTaskService studyTaskService;
    private final NotificationService notificationService;
    private final ProductStudyService productStudyService;
    private final StudentWordProgressService studentWordProgressService;
    private final WordService wordService;
    private final UnitService unitService;
    private final BookService bookService;
    private final UserService userService;
    private final StudyLogService studyLogService;
    private final StudySessionService studySessionService;
    private final StudyStageResultService studyStageResultService;

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("tasks", studyTaskService.listStudentTasks(tenantId, userId));
        data.put("today", productStudyService.getTodayStats(tenantId, userId));
        data.put("overview", productStudyService.getOverviewStats(tenantId, userId));
        data.put("unreadNotificationCount", notificationService.countUnread(tenantId, userId));
        return Result.ok(data);
    }

    @GetMapping("/wordbook")
    public Result<Map<String, Object>> wordbook(@RequestParam(defaultValue = "all") String tab) {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        LambdaQueryWrapper<StudentWordProgress> query = new LambdaQueryWrapper<StudentWordProgress>()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId);
        if ("learning".equalsIgnoreCase(tab)) {
            query.eq(StudentWordProgress::getStatus, StudentWordProgress.STATUS_LEARNING);
        } else if ("mastered".equalsIgnoreCase(tab)) {
            query.eq(StudentWordProgress::getStatus, StudentWordProgress.STATUS_MASTERED);
        }
        List<StudentWordProgress> progressList = studentWordProgressService.list(query.orderByDesc(StudentWordProgress::getUpdatedAt));
        if (progressList.isEmpty()) return Result.ok(Map.of("learning", List.of(), "mastered", List.of()));

        Map<Long, Word> wordMap = wordService.listByIds(progressList.stream().map(StudentWordProgress::getWordId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Word::getId, item -> item));
        Set<Long> unitIds = wordMap.values().stream().map(Word::getUnitId).collect(Collectors.toSet());
        Map<Long, Unit> unitMap = unitService.listByIds(unitIds).stream().collect(Collectors.toMap(Unit::getId, item -> item));
        Set<Long> bookIds = unitMap.values().stream().map(Unit::getBookId).collect(Collectors.toSet());
        Map<Long, Book> bookMap = bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, item -> item));

        List<StudentWordbookItemDto> learning = new ArrayList<>();
        List<StudentWordbookItemDto> mastered = new ArrayList<>();
        for (StudentWordProgress progress : progressList) {
            Word word = wordMap.get(progress.getWordId());
            if (word == null) continue;
            Unit unit = unitMap.get(word.getUnitId());
            Book book = unit != null ? bookMap.get(unit.getBookId()) : null;
            StudentWordbookItemDto dto = new StudentWordbookItemDto();
            dto.setWordId(word.getId());
            dto.setWord(word.getWord());
            dto.setPhonetic(word.getPhonetic());
            dto.setPos(word.getPos());
            dto.setMeaning(word.getMeaning());
            dto.setExampleEn(word.getExampleSentence());
            dto.setExampleZh(word.getExampleZh());
            dto.setUnitId(unit != null ? unit.getId() : null);
            dto.setUnitName(unit != null ? unit.getName() : null);
            dto.setBookId(book != null ? book.getId() : null);
            dto.setBookName(book != null ? book.getName() : null);
            dto.setStatus(progress.getStatus() != null && progress.getStatus() == StudentWordProgress.STATUS_MASTERED ? "mastered" : "learning");
            if (progress.getStatus() != null && progress.getStatus() == StudentWordProgress.STATUS_MASTERED) mastered.add(dto);
            else learning.add(dto);
        }
        return Result.ok(Map.of("learning", learning, "mastered", mastered));
    }

    @GetMapping("/units/{unitId}/words")
    public Result<List<StudentUnitWordDto>> unitWords(@PathVariable Long unitId) {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        return Result.ok(buildUnitWordList(tenantId, userId, unitId));
    }

    @GetMapping("/units/{unitId}/journey")
    public Result<Map<String, Object>> unitJourney(@PathVariable Long unitId,
                                                   @RequestParam(defaultValue = "5") int newLimit,
                                                   @RequestParam(defaultValue = "8") int reviewLimit,
                                                   @RequestParam(defaultValue = "3") int errorLimit) {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        return Result.ok(buildUnitJourney(tenantId, userId, unitId, newLimit, reviewLimit, errorLimit));
    }

    @GetMapping("/units/{unitId}/session/{stage}")
    public Result<Map<String, Object>> session(@PathVariable Long unitId, @PathVariable String stage) {
        Map<String, Object> data = studySessionService.getSessionPayload(requiredTenantId(), requiredUserId(), unitId, stage);
        return Result.ok(data == null ? Map.of() : data);
    }

    @PutMapping("/units/{unitId}/session/{stage}")
    public Result<Void> saveSession(@PathVariable Long unitId, @PathVariable String stage, @RequestBody Map<String, Object> payload) {
        studySessionService.saveSessionPayload(requiredTenantId(), requiredUserId(), unitId, stage, payload);
        return Result.ok();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/units/{unitId}/session/{stage}")
    public Result<Void> clearSession(@PathVariable Long unitId, @PathVariable String stage) {
        studySessionService.clearSession(requiredTenantId(), requiredUserId(), unitId, stage);
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<StudentStatsDto> stats() {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        Map<String, Object> heatmap = productStudyService.getOverviewStats(tenantId, userId);
        int streakDays = ((Number) heatmap.getOrDefault("streakDays", 0)).intValue();
        int masteredCount = studentWordProgressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .eq(StudentWordProgress::getStatus, StudentWordProgress.STATUS_MASTERED)
                .count().intValue();
        int learningDays = studyLogService.lambdaQuery()
                .eq(com.study.english.entity.StudyLog::getTenantId, tenantId)
                .eq(com.study.english.entity.StudyLog::getUserId, userId)
                .select(com.study.english.entity.StudyLog::getCreatedAt)
                .list().stream()
                .map(item -> item.getCreatedAt() != null ? item.getCreatedAt().toLocalDate() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()).size();
        return Result.ok(new StudentStatsDto(streakDays, learningDays, masteredCount));
    }

    @GetMapping("/results")
    public Result<List<Map<String, Object>>> results(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(studyStageResultService.listStudentResults(requiredTenantId(), requiredUserId(), limit));
    }

    @PutMapping("/results")
    public Result<Void> saveResult(@RequestBody SaveStageResultRequest request) {
        studyStageResultService.saveResult(requiredTenantId(), requiredUserId(), request);
        return Result.ok();
    }

    @GetMapping("/notifications")
    public Result<List<Notification>> notifications() {
        return Result.ok(notificationService.listByUser(requiredTenantId(), requiredUserId()));
    }

    @PutMapping("/notifications")
    public Result<Void> markNotificationsRead() {
        notificationService.markAllRead(requiredTenantId(), requiredUserId());
        return Result.ok();
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        User user = userService.getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) throw new BusinessException("账号不存在");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName() == null ? "" : user.getRealName());
        data.put("nickname", user.getNickname() == null ? "" : user.getNickname());
        data.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());
        data.put("studentNo", user.getStudentNo() == null ? "" : user.getStudentNo());
        data.put("gradeClass", user.getGradeClass() == null ? "" : user.getGradeClass());
        data.put("phone", user.getPhone() == null ? "" : user.getPhone());
        data.put("createdAt", user.getCreatedAt());
        data.put("mustChangePwd", user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
        return Result.ok(data);
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody StudentProfileUpdateRequest req) {
        User user = userService.updateStudentProfile(requiredTenantId(), requiredUserId(), req);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nickname", user.getNickname() == null ? "" : user.getNickname());
        data.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());
        data.put("phone", user.getPhone() == null ? "" : user.getPhone());
        return Result.ok(data);
    }

    @GetMapping("/last-study")
    public Result<Map<String, Object>> lastStudy() {
        User user = userService.getById(requiredUserId());
        if (user == null || !requiredTenantId().equals(user.getTenantId())) throw new BusinessException("账号不存在");
        return Result.ok(buildLastStudyPayload(user));
    }

    @PutMapping("/last-study")
    public Result<Map<String, Object>> updateLastStudy(@RequestBody StudentLastStudyRequest req) {
        String tenantId = requiredTenantId();
        Long userId = requiredUserId();
        Long unitId = req == null ? null : req.getUnitId();
        Long bookId = req == null ? null : req.getBookId();

        if (unitId != null) {
            Unit unit = unitService.getById(unitId);
            if (unit == null) throw new BusinessException("单元不存在");
            unitId = unit.getId();
            bookId = unit.getBookId();
        } else if (bookId != null) {
            Book book = bookService.getById(bookId);
            if (book == null) throw new BusinessException("课本不存在");
            bookId = book.getId();
        }

        StudentLastStudyRequest payload = new StudentLastStudyRequest();
        payload.setBookId(bookId);
        payload.setUnitId(unitId);
        User user = userService.updateStudentLastStudy(tenantId, userId, payload);
        return Result.ok(buildLastStudyPayload(user));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest req) {
        userService.updateOwnPassword(requiredTenantId(), requiredUserId(), req);
        return Result.ok();
    }

    private Map<String, Object> buildUnitJourney(String tenantId,
                                                 Long userId,
                                                 Long unitId,
                                                 int newLimit,
                                                 int reviewLimit,
                                                 int errorLimit) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) throw new BusinessException("单元不存在");
        Book book = unit.getBookId() == null ? null : bookService.getById(unit.getBookId());

        List<StudentUnitWordDto> allWords = buildUnitWordList(tenantId, userId, unit, book);
        List<StudentUnitWordDto> newPool = allWords.stream()
                .filter(this::isNewWord)
                .toList();
        List<StudentUnitWordDto> errorPool = allWords.stream()
                .filter(this::isWeakWord)
                .sorted(weakWordComparator())
                .toList();
        List<StudentUnitWordDto> reviewPool = allWords.stream()
                .filter(this::isReviewWord)
                .sorted(reviewWordComparator())
                .toList();
        List<StudentUnitWordDto> masteredPool = allWords.stream()
                .filter(dto -> "mastered".equals(dto.getReviewState()))
                .sorted(Comparator.comparingInt((StudentUnitWordDto dto) -> safeInt(dto.getCorrectCount())).reversed())
                .toList();

        Set<Long> usedWordIds = new HashSet<>();
        List<StudentUnitWordDto> newWords = takeUnique(newPool, usedWordIds, Math.max(0, newLimit));
        List<StudentUnitWordDto> errorWords = takeUnique(errorPool, usedWordIds, Math.max(0, errorLimit));
        List<StudentUnitWordDto> reviewWords = takeUnique(reviewPool, usedWordIds, Math.max(0, reviewLimit));
        List<StudentUnitWordDto> masteredWords = takeUnique(masteredPool, usedWordIds, 2);

        List<StudentUnitWordDto> focusWords = allWords.stream()
                .filter(dto -> safeInt(dto.getWrongCount()) > 0)
                .sorted(weakWordComparator())
                .limit(3)
                .toList();
        if (focusWords.isEmpty()) {
            focusWords = errorWords;
        }

        int plannedCount = newWords.size() + reviewWords.size() + errorWords.size();
        int studiedCount = (int) allWords.stream()
                .filter(dto -> !"new".equals(dto.getReviewState()))
                .count();
        int completionRate = allWords.isEmpty() ? 0 : (int) Math.round(studiedCount * 100.0 / allWords.size());
        int estimatedMinutes = Math.max(8, Math.min(12, 5 + newWords.size() + Math.max(1, (reviewWords.size() + errorWords.size()) / 3)));
        int tomorrowReviewCount = Math.max(
                (int) allWords.stream().filter(dto -> isDueWithinHours(dto.getNextReviewTime(), 24)).count(),
                Math.min(allWords.size(), newWords.size() + errorWords.size() + Math.max(1, reviewWords.size() / 2))
        );

        Map<String, Object> unitInfo = new LinkedHashMap<>();
        unitInfo.put("unitId", unit.getId());
        unitInfo.put("unitName", unit.getName());
        unitInfo.put("bookId", book != null ? book.getId() : null);
        unitInfo.put("bookName", book != null ? book.getName() : "");
        unitInfo.put("totalWords", allWords.size());
        unitInfo.put("completionRate", completionRate);

        Map<String, Object> todayTask = new LinkedHashMap<>();
        todayTask.put("newCount", newWords.size());
        todayTask.put("reviewCount", reviewWords.size());
        todayTask.put("errorCount", errorWords.size());
        todayTask.put("masteredCheckCount", masteredWords.size());
        todayTask.put("plannedWordCount", plannedCount);
        todayTask.put("estimatedMinutes", estimatedMinutes);
        todayTask.put("completionRate", completionRate);
        todayTask.put("tomorrowReviewCount", tomorrowReviewCount);
        todayTask.put("newPoolCount", newPool.size());
        todayTask.put("reviewPoolCount", reviewPool.size());
        todayTask.put("errorPoolCount", errorPool.size());

        Map<String, Object> buckets = new LinkedHashMap<>();
        buckets.put("newWords", newWords);
        buckets.put("reviewWords", reviewWords);
        buckets.put("errorWords", errorWords);
        buckets.put("masteredWords", masteredWords);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unit", unitInfo);
        result.put("todayTask", todayTask);
        result.put("buckets", buckets);
        result.put("focusWords", focusWords);
        result.put("stagePlan", buildStagePlan(newWords, reviewWords, errorWords, masteredWords));
        result.put("reviewSchedule", List.of(
                Map.of("label", "10分钟后", "tip", "第一次离开短时记忆前回看"),
                Map.of("label", "1天后", "tip", "第二天先复习，再学新词"),
                Map.of("label", "3天后", "tip", "把刚学会的词拉回长期记忆"),
                Map.of("label", "7天后", "tip", "周回看，防止学一个忘一个"),
                Map.of("label", "14天后", "tip", "开始降低频率，但不让它消失"),
                Map.of("label", "30天后", "tip", "月底抽查，确认真的掌握")
        ));
        result.put("allWords", allWords);
        return result;
    }

    private List<Map<String, Object>> buildStagePlan(List<StudentUnitWordDto> newWords,
                                                     List<StudentUnitWordDto> reviewWords,
                                                     List<StudentUnitWordDto> errorWords,
                                                     List<StudentUnitWordDto> masteredWords) {
        int warmupCount = Math.min(3, reviewWords.size() + errorWords.size());
        int learnCount = newWords.size();
        int recognizeCount = Math.min(8, newWords.size() + reviewWords.size() + Math.min(errorWords.size(), 2));
        int recallCount = Math.min(6, newWords.size() + reviewWords.size() + errorWords.size());
        int spellCount = Math.min(5, newWords.size() + errorWords.size());
        int quizCount = Math.min(5, newWords.size() + reviewWords.size() + errorWords.size() + masteredWords.size());
        return List.of(
                stageMeta("warmup", "热身复习", "先拿旧词热手，帮学生快速进入学习状态。", 1, warmupCount),
                stageMeta("learn", "新词学习", "先看图、听发音、读释义，建立完整第一印象。", 2, learnCount),
                stageMeta("recognize", "识别训练", "看图、听音、多选识别，先把词认出来。", 2, recognizeCount),
                stageMeta("recall", "回忆训练", "不给英文，靠中文和图片把答案想出来。", 2, recallCount),
                stageMeta("spell", "拼写训练", "从拖字母到补字母，再到完整拼写。", 2, spellCount),
                stageMeta("quiz", "离开前小测", "混合抽查一轮，决定哪些词进入下次复习。", 1, quizCount)
        );
    }

    private Map<String, Object> stageMeta(String key, String title, String goal, int minutes, int questionCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("title", title);
        item.put("goal", goal);
        item.put("minutes", minutes);
        item.put("questionCount", questionCount);
        return item;
    }

    private List<StudentUnitWordDto> buildUnitWordList(String tenantId, Long userId, Long unitId) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) throw new BusinessException("单元不存在");
        Book book = unit.getBookId() == null ? null : bookService.getById(unit.getBookId());
        return buildUnitWordList(tenantId, userId, unit, book);
    }

    private List<StudentUnitWordDto> buildUnitWordList(String tenantId, Long userId, Unit unit, Book book) {
        List<Word> words = wordService.lambdaQuery()
                .eq(Word::getUnitId, unit.getId())
                .orderByAsc(Word::getSortOrder)
                .list();
        if (words.isEmpty()) return List.of();

        Map<Long, StudentWordProgress> progressMap = studentWordProgressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .in(StudentWordProgress::getWordId, words.stream().map(Word::getId).collect(Collectors.toSet()))
                .list().stream()
                .collect(Collectors.toMap(StudentWordProgress::getWordId, item -> item));

        List<StudentUnitWordDto> list = new ArrayList<>();
        for (Word word : words) {
            list.add(toStudentUnitWordDto(word, progressMap.get(word.getId()), words));
        }
        return list;
    }

    private StudentUnitWordDto toStudentUnitWordDto(Word word, StudentWordProgress progress, List<Word> unitWords) {
        StudentUnitWordDto dto = new StudentUnitWordDto();
        dto.setWordId(word.getId());
        dto.setWord(word.getWord());
        dto.setPhonetic(word.getPhonetic());
        dto.setPos(word.getPos());
        dto.setMeaning(word.getMeaning());
        dto.setExampleEn(word.getExampleSentence());
        dto.setExampleZh(word.getExampleZh());
        dto.setAudioUrl(word.getAudioUrl());
        dto.setImageUrl(word.getImageUrl());
        dto.setExampleAudio(word.getAudioUrl());
        dto.setStatus(resolveWordStatus(progress));
        dto.setReviewState(resolveReviewState(progress));
        dto.setReviewStage(progress != null ? safeInt(progress.getRepetitions()) : 0);
        dto.setNextReviewTime(progress != null && progress.getNextReviewTime() != null ? progress.getNextReviewTime().toString() : null);
        dto.setCorrectCount(progress != null ? safeInt(progress.getCorrectCount()) : 0);
        dto.setWrongCount(progress != null ? safeInt(progress.getWrongCount()) : 0);
        dto.setDifficulty(resolveDifficulty(word, progress));
        dto.setSpellingPattern(resolveSpellingPattern(word.getWord()));
        dto.setConfusionWords(resolveConfusionWords(word, unitWords));
        return dto;
    }

    private List<StudentUnitWordDto> takeUnique(List<StudentUnitWordDto> source, Set<Long> usedWordIds, int limit) {
        if (limit <= 0 || source.isEmpty()) return List.of();
        List<StudentUnitWordDto> result = new ArrayList<>();
        for (StudentUnitWordDto dto : source) {
            if (dto.getWordId() == null || !usedWordIds.add(dto.getWordId())) {
                continue;
            }
            result.add(dto);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private Comparator<StudentUnitWordDto> weakWordComparator() {
        return Comparator
                .comparingInt((StudentUnitWordDto dto) -> safeInt(dto.getWrongCount())).reversed()
                .thenComparing(dto -> parseDateTime(dto.getNextReviewTime()), Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(StudentUnitWordDto::getWordId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<StudentUnitWordDto> reviewWordComparator() {
        return Comparator
                .comparing((StudentUnitWordDto dto) -> parseDateTime(dto.getNextReviewTime()), Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt((StudentUnitWordDto dto) -> safeInt(dto.getWrongCount())).reversed()
                .thenComparing(StudentUnitWordDto::getWordId, Comparator.nullsLast(Long::compareTo));
    }

    private boolean isNewWord(StudentUnitWordDto dto) {
        return "new".equals(dto.getReviewState()) || "unlearned".equals(dto.getStatus());
    }

    private boolean isWeakWord(StudentUnitWordDto dto) {
        return "weak".equals(dto.getReviewState());
    }

    private boolean isReviewWord(StudentUnitWordDto dto) {
        return "due".equals(dto.getReviewState()) || "learning".equals(dto.getReviewState());
    }

    private boolean isDueWithinHours(String nextReviewTime, int hours) {
        LocalDateTime value = parseDateTime(nextReviewTime);
        if (value == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return !value.isBefore(now) && !value.isAfter(now.plusHours(hours));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int resolveDifficulty(Word word, StudentWordProgress progress) {
        String text = word.getWord() == null ? "" : word.getWord().trim();
        int difficulty = 1;
        if (text.length() >= 5) difficulty++;
        if (text.length() >= 8) difficulty++;
        if (text.contains(" ") || text.contains("-")) difficulty++;
        if (progress != null && safeInt(progress.getWrongCount()) >= 2) difficulty++;
        return Math.max(1, Math.min(5, difficulty));
    }

    private String resolveSpellingPattern(String word) {
        if (word == null || word.isBlank()) return "";
        if (word.length() <= 2) return word;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char current = word.charAt(i);
            if (i == 0 || i == word.length() - 1 || !Character.isLetter(current)) {
                builder.append(current);
            } else {
                builder.append('_');
            }
            if (i < word.length() - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private List<String> resolveConfusionWords(Word word, List<Word> unitWords) {
        String current = word.getWord() == null ? "" : word.getWord().toLowerCase();
        return unitWords.stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), word.getId()))
                .sorted(Comparator
                        .comparing((Word candidate) -> !shareSameInitial(current, candidate.getWord()))
                        .thenComparingInt(candidate -> Math.abs(current.length() - length(candidate.getWord())))
                        .thenComparing(Word::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(Word::getWord)
                .filter(Objects::nonNull)
                .limit(3)
                .toList();
    }

    private boolean shareSameInitial(String left, String right) {
        if (left == null || left.isBlank() || right == null || right.isBlank()) return false;
        return Character.toLowerCase(left.charAt(0)) == Character.toLowerCase(right.charAt(0));
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String requiredTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        return tenantId;
    }

    private Long requiredUserId() {
        Long userId = TenantContext.getUserId();
        if (userId == null) throw new BusinessException("未登录");
        return userId;
    }

    private String resolveWordStatus(StudentWordProgress progress) {
        if (progress == null || progress.getStatus() == null) {
            return "unlearned";
        }
        if (progress.getStatus() == StudentWordProgress.STATUS_MASTERED) {
            return "mastered";
        }
        if (progress.getStatus() == StudentWordProgress.STATUS_LEARNING) {
            return "learning";
        }
        return "unlearned";
    }

    private String resolveReviewState(StudentWordProgress progress) {
        if (progress == null || safeInt(progress.getRepetitions()) <= 0) {
            return "new";
        }
        if (progress.getIsWrong() != null && progress.getIsWrong() == StudentWordProgress.IS_WRONG_YES) {
            return "weak";
        }
        if (progress.getInReinforcement() != null && progress.getInReinforcement() == 1) {
            return "weak";
        }
        if (progress.getNextReviewTime() != null && !progress.getNextReviewTime().isAfter(LocalDateTime.now())) {
            return "due";
        }
        if (progress.getStatus() != null && progress.getStatus() == StudentWordProgress.STATUS_MASTERED) {
            return "mastered";
        }
        if (progress.getStrength() != null && progress.getStrength().doubleValue() >= 0.65) {
            return "mastered";
        }
        return "learning";
    }

    private Map<String, Object> buildLastStudyPayload(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        Long bookId = user == null ? null : user.getLastStudyBookId();
        Long unitId = user == null ? null : user.getLastStudyUnitId();
        Book book = bookId == null ? null : bookService.getById(bookId);
        Unit unit = unitId == null ? null : unitService.getById(unitId);

        if (unit != null && (book == null || !Objects.equals(book.getId(), unit.getBookId()))) {
            book = unit.getBookId() == null ? null : bookService.getById(unit.getBookId());
            bookId = unit.getBookId();
        }

        data.put("bookId", book != null ? book.getId() : bookId);
        data.put("bookName", book != null ? book.getName() : "");
        data.put("unitId", unit != null ? unit.getId() : unitId);
        data.put("unitName", unit != null ? unit.getName() : "");
        data.put("updatedAt", user == null ? null : user.getLastStudyAt());
        data.put("hasLastStudy", book != null || unit != null || bookId != null || unitId != null);
        return data;
    }
}

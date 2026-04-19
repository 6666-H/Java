package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.dto.StudyQueueItemDto;
import com.study.english.dto.UnitProgressDetailDto;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.Word;
import com.study.english.mapper.StudentWordProgressMapper;
import com.study.english.mapper.StudyLogMapper;
import com.study.english.service.ProductStudyService;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.UserService;
import com.study.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教材/单元模式 - 学习队列、答题、统计
 * 间隔规则：1=10min, 2=1d, 3=3d, 4=7d, 5=15d, 6=30d, 7+=60d
 */
@Service
@RequiredArgsConstructor
public class ProductStudyServiceImpl implements ProductStudyService {

    private static final int[] INTERVAL_DAYS = {0, 1, 3, 7, 15, 30, 60};
    private static final int FIRST_INTERVAL_MINUTES = 10;
    private static final double STRENGTH_CORRECT = 0.1;
    private static final double STRENGTH_WRONG = -0.15;
    private static final int DAILY_NEW_LIMIT = 20;

    private final WordService wordService;
    private final StudentWordProgressMapper progressMapper;
    private final StudentWordProgressService progressService;
    private final StudyLogMapper studyLogMapper;
    private final StudyLogService studyLogService;
    private final UserService userService;

    @Override
    public List<StudyQueueItemDto> getUnitQueue(String tenantId, Long userId, Long unitId, int limit) {
        if (limit <= 0) limit = 50;
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedToday = new HashSet<>(studyLogMapper.selectWordIdsWithTodayCountGe2(tenantId, userId));

        List<Long> wordIds = new ArrayList<>();

        // 1. 复习词优先
        List<Long> overdue = progressMapper.selectProductOverdueWordIdsInUnit(tenantId, userId, unitId, now, limit);
        if (overdue != null) {
            for (Long id : overdue) {
                if (!excludedToday.contains(id)) wordIds.add(id);
                if (wordIds.size() >= limit) break;
            }
        }
        int remain = limit - wordIds.size();
        if (remain <= 0) return buildQueueItems(tenantId, userId, unitId, wordIds, true);

        // 2. 新词（每日限量 20）
        int newLimit = Math.min(DAILY_NEW_LIMIT, remain);
        List<Word> unitWords = wordService.list(new LambdaQueryWrapper<Word>()
                .eq(Word::getUnitId, unitId)
                .orderByAsc(Word::getSortOrder));
        List<Long> unitWordIds = unitWords.stream().map(Word::getId).toList();
        List<Long> learnedIds = progressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .in(StudentWordProgress::getWordId, unitWordIds)
                .list().stream().map(StudentWordProgress::getWordId).toList();
        Set<Long> learnedSet = new HashSet<>(learnedIds);
        for (Word w : unitWords) {
            if (wordIds.size() >= limit) break;
            if (learnedSet.contains(w.getId())) continue;
            if (excludedToday.contains(w.getId())) continue;
            wordIds.add(w.getId());
            if (wordIds.size() - (limit - remain) >= newLimit) break;
        }

        return buildQueueItems(tenantId, userId, unitId, wordIds, false);
    }

    @Override
    public List<StudyQueueItemDto> getReviewQueue(String tenantId, Long userId, int limit) {
        if (limit <= 0) limit = 50;
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedToday = new HashSet<>(studyLogMapper.selectWordIdsWithTodayCountGe2(tenantId, userId));

        List<Long> overdue = progressMapper.selectProductOverdueWordIdsGlobal(tenantId, userId, now, limit + excludedToday.size());
        List<Long> wordIds = new ArrayList<>();
        if (overdue != null) {
            for (Long id : overdue) {
                if (!excludedToday.contains(id)) wordIds.add(id);
                if (wordIds.size() >= limit) break;
            }
        }
        return buildQueueItemsGlobal(tenantId, userId, wordIds);
    }

    private List<StudyQueueItemDto> buildQueueItems(String tenantId, Long userId, Long unitId, List<Long> wordIds, boolean allReview) {
        if (wordIds == null || wordIds.isEmpty()) return Collections.emptyList();
        List<Word> words = wordService.listByIds(wordIds);
        Map<Long, Word> wordMap = words.stream().collect(Collectors.toMap(Word::getId, w -> w));
        List<Word> unitWords = wordService.list(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, unitId));

        List<StudyQueueItemDto> result = new ArrayList<>();
        Set<Long> reviewIds = new HashSet<>();
        List<Long> overdue = progressMapper.selectProductOverdueWordIdsInUnit(tenantId, userId, unitId, LocalDateTime.now(), 1000);
        if (overdue != null) reviewIds.addAll(overdue);

        for (Long wid : wordIds) {
            Word w = wordMap.get(wid);
            if (w == null) continue;
            List<String> options = buildOptions(w, unitWords);
            int correctIndex = options.indexOf(w.getWord());
            if (correctIndex < 0) correctIndex = 0;
            result.add(new StudyQueueItemDto(w.getId(), w.getWord(), w.getPhonetic(), w.getMeaning(),
                    options, correctIndex, reviewIds.contains(wid)));
        }
        return result;
    }

    private List<StudyQueueItemDto> buildQueueItemsGlobal(String tenantId, Long userId, List<Long> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) return Collections.emptyList();
        List<Word> words = wordService.listByIds(wordIds);
        Map<Long, Word> wordMap = words.stream().collect(Collectors.toMap(Word::getId, w -> w));
        List<Word> allWords = wordService.list();
        List<StudyQueueItemDto> result = new ArrayList<>();
        for (Long wid : wordIds) {
            Word w = wordMap.get(wid);
            if (w == null) continue;
            List<String> options = buildOptions(w, allWords);
            int correctIndex = options.indexOf(w.getWord());
            if (correctIndex < 0) correctIndex = 0;
            result.add(new StudyQueueItemDto(w.getId(), w.getWord(), w.getPhonetic(), w.getMeaning(),
                    options, correctIndex, true));
        }
        return result;
    }

    private List<String> buildOptions(Word correct, List<Word> pool) {
        List<String> options = new ArrayList<>();
        options.add(correct.getWord());
        List<Word> others = pool.stream().filter(x -> x != null && !x.getId().equals(correct.getId())).toList();
        Random r = new Random();
        Set<String> used = new HashSet<>();
        used.add(correct.getWord());
        for (int i = 0; options.size() < 4 && i < 100 && !others.isEmpty(); i++) {
            Word o = others.get(r.nextInt(others.size()));
            if (o.getWord() != null && used.add(o.getWord())) options.add(o.getWord());
        }
        while (options.size() < 4) {
            options.add("option" + (options.size() + 1));
        }
        Collections.shuffle(options);
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitAnswer(String tenantId,
                                            Long userId,
                                            Long wordId,
                                            boolean isCorrect,
                                            String studyMode,
                                            String userInput) {
        LocalDateTime now = LocalDateTime.now();
        StudentWordProgress p = getOrCreateProgress(tenantId, userId, wordId, now);

        int reps = p.getRepetitions() == null ? 0 : p.getRepetitions();
        double strength = p.getStrength() != null ? p.getStrength().doubleValue() : 0;

        if (isCorrect) {
            reps = reps + 1;
            strength = Math.min(1, strength + STRENGTH_CORRECT);
            int intervalDays = reps == 1 ? 0 : (reps <= 6 ? INTERVAL_DAYS[reps] : 60);
            LocalDateTime nextReview = reps == 1 ? now.plusMinutes(FIRST_INTERVAL_MINUTES) : now.plusDays(intervalDays);
            p.setRepetitions(reps);
            p.setNextReviewTime(nextReview);
            p.setConsecutiveCorrectCount(reps);
        } else {
            reps = 1;
            strength = Math.max(0, strength + STRENGTH_WRONG);
            p.setRepetitions(1);
            p.setNextReviewTime(now.plusDays(1));
            p.setConsecutiveCorrectCount(0);
            p.setWrongCount((p.getWrongCount() == null ? 0 : p.getWrongCount()) + 1);
        }
        p.setStrength(BigDecimal.valueOf(strength).setScale(2, RoundingMode.HALF_UP));
        p.setLastStudyAt(now);
        progressService.updateById(p);

        String normalizedMode = normalizeStudyMode(studyMode);
        studyLogService.logStudy(
                tenantId,
                userId,
                wordId,
                normalizedMode,
                isCorrect ? "KNOW" : "DONT_KNOW",
                userInput != null && !userInput.isBlank() ? userInput.trim() : null
        );
        userService.updateLastActiveAt(userId);

        Map<String, Object> uw = new LinkedHashMap<>();
        uw.put("repetitions", p.getRepetitions());
        uw.put("intervalDays", reps == 1 ? 0 : (reps <= 6 ? INTERVAL_DAYS[reps] : 60));
        uw.put("nextReview", p.getNextReviewTime());
        uw.put("strength", p.getStrength());
        uw.put("status", resolveWordStatus(p));
        uw.put("nextReviewLabel", buildNextReviewLabel(p.getNextReviewTime(), now));
        return Map.of("userWord", uw);
    }

    private String normalizeStudyMode(String studyMode) {
        if (studyMode == null || studyMode.isBlank()) {
            return "journey.quiz";
        }
        return studyMode.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveWordStatus(StudentWordProgress progress) {
        if (progress.getStatus() != null && progress.getStatus() == StudentWordProgress.STATUS_MASTERED) {
            return "mastered";
        }
        if (progress.getIsWrong() != null && progress.getIsWrong() == StudentWordProgress.IS_WRONG_YES) {
            return "weak";
        }
        if (progress.getInReinforcement() != null && progress.getInReinforcement() == 1) {
            return "weak";
        }
        return "learning";
    }

    private String buildNextReviewLabel(LocalDateTime nextReviewTime, LocalDateTime now) {
        if (nextReviewTime == null) {
            return "稍后复习";
        }
        long minutes = java.time.Duration.between(now, nextReviewTime).toMinutes();
        if (minutes <= 15) {
            return "10分钟后回看";
        }
        long hours = java.time.Duration.between(now, nextReviewTime).toHours();
        if (hours <= 24) {
            return "明天继续巩固";
        }
        long days = Math.max(1, java.time.Duration.between(now, nextReviewTime).toDays());
        return days + "天后复习";
    }

    private StudentWordProgress getOrCreateProgress(String tenantId, Long userId, Long wordId, LocalDateTime now) {
        StudentWordProgress p = progressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .eq(StudentWordProgress::getWordId, wordId)
                .one();
        if (p == null) {
            p = new StudentWordProgress();
            p.setTenantId(tenantId);
            p.setUserId(userId);
            p.setWordId(wordId);
            p.setRepetitions(0);
            p.setStrength(BigDecimal.ZERO);
            p.setNextReviewTime(now);
            p.setLastStudyAt(now);
            progressService.save(p);
        }
        return p;
    }

    @Override
    public UnitProgressDetailDto getUnitProgress(String tenantId, Long userId, Long unitId) {
        List<Word> unitWords = wordService.list(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, unitId));
        long total = unitWords.size();
        List<Long> unitWordIds = unitWords.stream().map(Word::getId).toList();
        if (unitWordIds.isEmpty()) {
            return new UnitProgressDetailDto("not_started", 0.0, 0, 0, null);
        }
        List<StudentWordProgress> list = progressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .in(StudentWordProgress::getWordId, unitWordIds)
                .list();
        long learned = list.stream().filter(x -> (x.getRepetitions() != null && x.getRepetitions() > 0) || (x.getStrength() != null && x.getStrength().doubleValue() > 0)).count();
        double progress = total > 0 ? (double) learned / total : 0;
        String status = learned == 0 ? "not_started" : (learned >= total ? "completed" : "learning");
        BigDecimal avgStrength = list.stream()
                .map(StudentWordProgress::getStrength)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean completedByStrength = total > 0 && learned > 0 && avgStrength.doubleValue() / learned >= 0.4;
        if (completedByStrength) status = "completed";
        return new UnitProgressDetailDto(status, progress, (int) learned, (int) total,
                status.equals("completed") ? LocalDateTime.now() : null);
    }

    @Override
    public Map<String, Object> getTodayStats(String tenantId, Long userId) {
        Map<String, Object> row = studyLogMapper.selectTodayStats(tenantId, userId);
        long total = row != null && row.get("total") != null ? ((Number) row.get("total")).longValue() : 0;
        Object correctObj = row != null ? row.get("correct") : null;
        long correct = correctObj != null ? ((Number) correctObj).longValue() : 0;
        double rate = total > 0 ? (double) correct / total : 0;
        return Map.of("completedCount", total, "correctCount", correct, "correctRate", rate);
    }

    @Override
    public Map<String, Object> getOverviewStats(String tenantId, Long userId) {
        long totalWords = wordService.count();
        long mastered = progressService.lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .list().stream()
                .filter(p -> p.getStrength() != null && p.getStrength().doubleValue() >= 0.4)
                .count();
        double masteredRatio = totalWords > 0 ? (double) mastered / totalWords : 0;
        return Map.of(
                "streakDays", 0,
                "unitCompletionRate", 0.5,
                "bookCompletionRate", 0.2,
                "masteredRatio", masteredRatio
        );
    }
}

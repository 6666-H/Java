package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.dto.CheckSpellingResult;
import com.study.english.dto.StudyChoiceOptionDto;
import com.study.english.dto.StudyQuestionOptionsDto;
import com.study.english.dto.StudySubmitResult;
import com.study.english.dto.StudyStatsDto;
import com.study.english.entity.ErrorLog;
import com.study.english.entity.StudentWordModeProgress;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.Unit;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.StudentWordProgressMapper;
import com.study.english.service.*;
import com.study.english.service.StudentWordModeProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentWordProgressServiceImpl extends ServiceImpl<StudentWordProgressMapper, StudentWordProgress> implements StudentWordProgressService {

    /** 主记忆进度：首次正确后 10 分钟，再次正确后 1/3/7/15/30 天。 */
    private static final int[] REVIEW_STAGE_HOURS = {0, 24, 72, 168, 360, 720};
    private static final int DEFAULT_NEXT_LIST_LIMIT = 20;

    private final WordService wordService;
    private final UnitService unitService;
    private final ErrorLogService errorLogService;
    private final UserService userService;
    private final StudentUnitModeService unitModeService;
    private final StudentWordModeProgressService modeProgressService;

    @Override
    public Word getNextWord(String tenantId, Long userId, Long unitId) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) throw new BusinessException("单元不存在");
        LocalDateTime now = LocalDateTime.now();
        Long reviewWordId = baseMapper.selectNextReviewWordId(tenantId, userId, unitId, now);
        if (reviewWordId != null) {
            return wordService.getById(reviewWordId);
        }
        List<Word> unitWords = wordService.list(new LambdaQueryWrapper<Word>()
                .eq(Word::getUnitId, unitId)
                .orderByAsc(Word::getSortOrder));
        List<Long> learnedWordIds = list(new LambdaQueryWrapper<StudentWordProgress>()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .in(StudentWordProgress::getWordId, unitWords.stream().map(Word::getId).collect(Collectors.toList())))
                .stream().map(StudentWordProgress::getWordId).collect(Collectors.toList());
        for (Word w : unitWords) {
            if (!learnedWordIds.contains(w.getId())) {
                return w;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(String tenantId, Long userId, Long wordId, boolean known) {
        LocalDateTime now = LocalDateTime.now();
        StudentWordProgress progress = getOne(new LambdaQueryWrapper<StudentWordProgress>()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .eq(StudentWordProgress::getWordId, wordId));
        if (progress == null) {
            progress = new StudentWordProgress();
            progress.setTenantId(tenantId);
            progress.setUserId(userId);
            progress.setWordId(wordId);
            progress.setMasteryLevel(0);
            progress.setWrongCount(0);
            progress.setIsWrong(StudentWordProgress.IS_WRONG_NO);
            progress.setConsecutiveCorrectCount(0);
            progress.setLastReviewTime(now);
            progress.setNextReviewTime(now);
            progress.setLastStudyAt(now);
            save(progress);
        }
        if (known) {
            int streak = (progress.getConsecutiveCorrectCount() == null ? 0 : progress.getConsecutiveCorrectCount()) + 1;
            progress.setConsecutiveCorrectCount(streak);
            progress.setIsWrong(StudentWordProgress.IS_WRONG_NO);
            if (streak >= StudentWordProgress.CONSECUTIVE_CORRECT_TO_MASTER) {
                progress.setNextReviewTime(now.plusHours(24));
                progress.setConsecutiveCorrectCount(0);
            } else {
                int nextLevel = Math.min(StudentWordProgress.MASTERY_MAX, (progress.getMasteryLevel() == null ? 0 : progress.getMasteryLevel()) + 1);
                progress.setMasteryLevel(nextLevel);
                int days = nextLevel <= REVIEW_STAGE_HOURS.length ? Math.max(1, REVIEW_STAGE_HOURS[nextLevel - 1] / 24) : Math.max(1, REVIEW_STAGE_HOURS[REVIEW_STAGE_HOURS.length - 1] / 24);
                progress.setNextReviewTime(now.plusDays(days));
            }
        } else {
            progress.setMasteryLevel(0);
            progress.setNextReviewTime(now.plusHours(24));
            progress.setIsWrong(StudentWordProgress.IS_WRONG_YES);
            progress.setConsecutiveCorrectCount(0);
            if (progress.getWrongCount() == null) progress.setWrongCount(0);
            progress.setWrongCount(progress.getWrongCount() + 1);
            progress.setLastErrorTime(now);
        }
        progress.setLastReviewTime(now);
        progress.setLastStudyAt(now);
        updateById(progress);
        userService.updateLastActiveAt(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudySubmitResult submitStudyFeedback(String tenantId, Long userId, Long wordId, String feedbackType, String errorType, String mode) {
        validateSubmitArgs(tenantId, userId, wordId);
        if (feedbackType == null) feedbackType = "";
        boolean isError = "DONT_KNOW".equalsIgnoreCase(feedbackType) || "SPELLING_ERROR".equalsIgnoreCase(feedbackType);
        LocalDateTime now = LocalDateTime.now();
        StudentWordProgress progress = getOrCreateProgress(tenantId, userId, wordId, now);
        int previousRepetitions = progress.getRepetitions() == null ? 0 : progress.getRepetitions();
        int modeStreak = 0;
        int reviewIntervalHours = 24;

        Word word = wordService.getById(wordId);
        boolean modeTrackable = word != null && word.getUnitId() != null;
        if (modeTrackable) {
            modeStreak = modeProgressService.updateProgress(tenantId, userId, wordId, word.getUnitId(), mode, !isError);
        }

        if (isError) {
            progress.setWrongCount((progress.getWrongCount() == null ? 0 : progress.getWrongCount()) + 1);
            progress.setLastErrorTime(now);
            int nextRepetitions = Math.max(0, previousRepetitions - 1);
            progress.setRepetitions(nextRepetitions);
            progress.setNextReviewTime(nextRepetitions == 0 ? now.plusMinutes(10) : now.plusHours(12));
            progress.setIsWrong(StudentWordProgress.IS_WRONG_YES);
            progress.setConsecutiveCorrectCount(0);
            progress.setConsecutiveWrongCount((progress.getConsecutiveWrongCount() == null ? 0 : progress.getConsecutiveWrongCount()) + 1);
            progress.setStatus(StudentWordProgress.STATUS_LEARNING);
            progress.setMasteryLevel(Math.max(0, progress.getMasteryLevel() == null ? 0 : progress.getMasteryLevel() - 1));
            BigDecimal currentStrength = progress.getStrength() == null ? BigDecimal.ZERO : progress.getStrength();
            progress.setStrength(currentStrength.subtract(BigDecimal.valueOf(0.08)).max(BigDecimal.ZERO));
            progress.setLastReviewTime(now);
            progress.setLastStudyAt(now);
            updateById(progress);
            reviewIntervalHours = nextRepetitions == 0 ? 1 : 12;
            String logErrorType;
            if ("SPELLING_ERROR".equalsIgnoreCase(feedbackType)) {
                logErrorType = ErrorLog.TYPE_SPELLING_ERROR;
            } else {
                String et = (errorType != null && !errorType.isBlank()) ? errorType.trim().toUpperCase() : "";
                logErrorType = ("ENG_TO_CH".equals(et) || "CH_TO_ENG".equals(et) || "FLASHCARD".equals(et))
                        ? et : ErrorLog.TYPE_DONT_KNOW;
            }
            errorLogService.logError(tenantId, userId, wordId, logErrorType);
        } else {
            int streak = (progress.getConsecutiveCorrectCount() == null ? 0 : progress.getConsecutiveCorrectCount()) + 1;
            progress.setConsecutiveCorrectCount(streak);
            progress.setConsecutiveWrongCount(0);
            progress.setStatus(previousRepetitions >= 1 ? StudentWordProgress.STATUS_MASTERED : StudentWordProgress.STATUS_LEARNING);
            int nextLevel = Math.min(StudentWordProgress.MASTERY_MAX, (progress.getMasteryLevel() == null ? 0 : progress.getMasteryLevel()) + 1);
            progress.setMasteryLevel(nextLevel);
            progress.setCorrectCount((progress.getCorrectCount() == null ? 0 : progress.getCorrectCount()) + 1);
            int repetitions = Math.min(7, (progress.getRepetitions() == null ? 0 : progress.getRepetitions()) + 1);
            progress.setRepetitions(repetitions);
            BigDecimal currentStrength = progress.getStrength() == null ? BigDecimal.ZERO : progress.getStrength();
            progress.setStrength(currentStrength.add(BigDecimal.valueOf(0.06)).min(BigDecimal.ONE));
            LocalDateTime nextReviewTime = computeNextReviewTime(now, repetitions);
            progress.setNextReviewTime(nextReviewTime);
            reviewIntervalHours = Math.max(1, (int) java.time.Duration.between(now, nextReviewTime).toHours());
            if (streak >= 2) {
                progress.setIsWrong(StudentWordProgress.IS_WRONG_NO);
            }
            if (repetitions >= 2) progress.setStatus(StudentWordProgress.STATUS_MASTERED);
            progress.setLastReviewTime(now);
            progress.setLastStudyAt(now);
            updateById(progress);
        }
        userService.updateLastActiveAt(userId);
        int updatedRepetitions = progress.getRepetitions() == null ? 0 : progress.getRepetitions();
        boolean wordCompleted = !isError && (
                modeTrackable
                        ? modeStreak >= StudentWordModeProgress.REQUIRED_CORRECT
                        : (previousRepetitions >= 2 || updatedRepetitions >= 2)
        );
        boolean stillWeak = StudentWordProgress.IS_WRONG_YES == (progress.getIsWrong() == null ? 0 : progress.getIsWrong());
        return new StudySubmitResult(wordCompleted, stillWeak, modeStreak, reviewIntervalHours);
    }

    @Override
    public List<Word> getNextList(String tenantId, Long userId, Long unitId, int limit, String mode) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) throw new BusinessException("单元不存在");
        if (limit <= 0) limit = DEFAULT_NEXT_LIST_LIMIT;
        Set<Long> incompleteWordIds = null;
        if ("spell".equalsIgnoreCase(mode) || "SPELL".equalsIgnoreCase(mode)) {
            List<Word> incompleteWords = modeProgressService.getIncompleteWords(tenantId, userId, unitId, mode, Integer.MAX_VALUE);
            if (incompleteWords.isEmpty()) return Collections.emptyList();
            incompleteWordIds = incompleteWords.stream().map(Word::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> wordIds = new ArrayList<>();

        List<Long> wrongIds = baseMapper.selectWrongReviewWordIds(tenantId, userId, unitId, now, limit);
        appendFilteredIds(wordIds, wrongIds, incompleteWordIds, limit);
        int remain = limit - wordIds.size();
        if (remain > 0) {
            List<Long> todayIds = baseMapper.selectTodayReviewWordIds(tenantId, userId, unitId, now, wordIds.isEmpty() ? Collections.emptyList() : wordIds, remain);
            appendFilteredIds(wordIds, todayIds, incompleteWordIds, limit);
        }
        remain = limit - wordIds.size();
        if (remain > 0) {
            List<Word> unitWords = wordService.list(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, unitId).orderByAsc(Word::getSortOrder));
            List<Long> learnedIds = list(new LambdaQueryWrapper<StudentWordProgress>()
                    .eq(StudentWordProgress::getTenantId, tenantId)
                    .eq(StudentWordProgress::getUserId, userId)
                    .in(StudentWordProgress::getWordId, unitWords.stream().map(Word::getId).collect(Collectors.toList())))
                    .stream().map(StudentWordProgress::getWordId).collect(Collectors.toList());
            for (Word w : unitWords) {
                if (incompleteWordIds != null && !incompleteWordIds.contains(w.getId())) continue;
                if (!learnedIds.contains(w.getId())) {
                    wordIds.add(w.getId());
                    if (wordIds.size() >= limit) break;
                }
            }
        }

        if (wordIds.isEmpty()) return Collections.emptyList();
        List<Word> words = wordService.listByIds(wordIds);
        words.sort((a, b) -> Integer.compare(wordIds.indexOf(a.getId()), wordIds.indexOf(b.getId())));
        return words;
    }

    @Override
    public StudyQuestionOptionsDto getQuestionOptions(String tenantId, Long userId, Long wordId, String mode) {
        Word correct = wordService.getById(wordId);
        if (correct == null) throw new BusinessException("单词不存在");
        String normalizedMode = mode == null ? "" : mode.trim().toLowerCase();
        if (!"eng_ch".equals(normalizedMode) && !"ch_eng".equals(normalizedMode)) {
            return new StudyQuestionOptionsDto(wordId, normalizedMode, Collections.emptyList());
        }

        List<Word> candidates = collectDistractorCandidates(tenantId, userId, correct);
        List<Word> distractors = pickDistractors(candidates, correct, normalizedMode, 3);
        List<StudyChoiceOptionDto> options = new ArrayList<>();
        options.add(new StudyChoiceOptionDto(correct.getId(), displayText(correct, normalizedMode), true));
        for (Word distractor : distractors) {
            options.add(new StudyChoiceOptionDto(distractor.getId(), displayText(distractor, normalizedMode), false));
        }
        Collections.shuffle(options, ThreadLocalRandom.current());
        return new StudyQuestionOptionsDto(wordId, normalizedMode, options);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckSpellingResult checkSpelling(String tenantId, Long userId, Long wordId, String userInput) {
        Word word = wordService.getById(wordId);
        if (word == null) return CheckSpellingResult.fail("单词不存在");
        String expected = word.getWord() == null ? "" : word.getWord().trim();
        String actual = userInput == null ? "" : userInput.trim();
        if (expected.equalsIgnoreCase(actual)) return CheckSpellingResult.ok();

        LocalDateTime now = LocalDateTime.now();
        StudentWordProgress progress = getOrCreateProgress(tenantId, userId, wordId, now);
        progress.setWrongCount((progress.getWrongCount() == null ? 0 : progress.getWrongCount()) + 1);
        progress.setLastErrorTime(now);
        progress.setNextReviewTime(now.plusHours(24));
        progress.setIsWrong(StudentWordProgress.IS_WRONG_YES);
        progress.setConsecutiveCorrectCount(0);
        progress.setStatus(StudentWordProgress.STATUS_LEARNING);
        progress.setLastStudyAt(now);
        updateById(progress);
        errorLogService.logError(tenantId, userId, wordId, ErrorLog.TYPE_SPELLING_ERROR);
        modeProgressService.updateProgress(tenantId, userId, wordId, word.getUnitId(), "SPELL", false);
        userService.updateLastActiveAt(userId);
        return CheckSpellingResult.fail("拼写错误，正确为：" + word.getWord());
    }

    private List<Word> collectDistractorCandidates(String tenantId, Long userId, Word correct) {
        LinkedHashMap<Long, Word> candidates = new LinkedHashMap<>();
        Unit currentUnit = correct.getUnitId() != null ? unitService.getById(correct.getUnitId()) : null;

        List<Word> sameUnitWords = wordService.list(new LambdaQueryWrapper<Word>()
                .eq(Word::getUnitId, correct.getUnitId())
                .ne(Word::getId, correct.getId())
                .orderByAsc(Word::getSortOrder));
        addCandidateBatch(candidates, sameUnitWords);

        if (currentUnit != null && currentUnit.getBookId() != null) {
            List<Long> bookUnitIds = unitService.list(new LambdaQueryWrapper<Unit>()
                            .eq(Unit::getBookId, currentUnit.getBookId())
                            .orderByAsc(Unit::getSortOrder))
                    .stream()
                    .map(Unit::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!bookUnitIds.isEmpty()) {
                List<Word> sameBookWords = wordService.list(new LambdaQueryWrapper<Word>()
                        .in(Word::getUnitId, bookUnitIds)
                        .ne(Word::getId, correct.getId())
                        .orderByAsc(Word::getSortOrder));
                addCandidateBatch(candidates, sameBookWords);
            }
        }

        List<Long> weakWordIds = lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .eq(StudentWordProgress::getIsWrong, StudentWordProgress.IS_WRONG_YES)
                .orderByDesc(StudentWordProgress::getLastErrorTime)
                .last("LIMIT 40")
                .list()
                .stream()
                .map(StudentWordProgress::getWordId)
                .filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, correct.getId()))
                .toList();
        if (!weakWordIds.isEmpty()) {
            addCandidateBatch(candidates, wordService.listByIds(weakWordIds));
        }

        addCandidateBatch(candidates, wordService.list(new LambdaQueryWrapper<Word>()
                .ne(Word::getId, correct.getId())
                .last("LIMIT 300")));

        return new ArrayList<>(candidates.values());
    }

    private void addCandidateBatch(Map<Long, Word> candidates, List<Word> words) {
        if (words == null) return;
        for (Word word : words) {
            if (word == null || word.getId() == null) continue;
            candidates.putIfAbsent(word.getId(), word);
        }
    }

    private List<Word> pickDistractors(List<Word> candidates, Word correct, String mode, int limit) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
        Unit currentUnit = correct.getUnitId() != null ? unitService.getById(correct.getUnitId()) : null;
        Long currentBookId = currentUnit != null ? currentUnit.getBookId() : null;
        String correctDisplay = normalizeDisplayText(displayText(correct, mode));

        List<WordScore> scored = new ArrayList<>();
        for (Word candidate : candidates) {
            if (candidate == null || Objects.equals(candidate.getId(), correct.getId())) continue;
            String candidateDisplay = normalizeDisplayText(displayText(candidate, mode));
            if (candidateDisplay.isBlank() || candidateDisplay.equals(correctDisplay)) continue;
            double score = calculateDistractorScore(correct, candidate, mode, currentBookId);
            scored.add(new WordScore(candidate, score));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));

        List<Word> result = new ArrayList<>();
        Set<String> usedDisplays = new HashSet<>();
        usedDisplays.add(correctDisplay);
        for (WordScore item : scored) {
            Word candidate = item.word();
            String display = normalizeDisplayText(displayText(candidate, mode));
            if (!usedDisplays.add(display)) continue;
            result.add(candidate);
            if (result.size() >= limit) break;
        }
        return result;
    }

    private double calculateDistractorScore(Word correct, Word candidate, String mode, Long currentBookId) {
        double score = 0;
        if (Objects.equals(candidate.getUnitId(), correct.getUnitId())) {
            score += 50;
        } else if (currentBookId != null && belongsToBook(candidate.getUnitId(), currentBookId)) {
            score += 28;
        }

        double lexical = lexicalSimilarity(correct.getWord(), candidate.getWord());
        double meaning = meaningSimilarity(correct.getMeaning(), candidate.getMeaning());
        int lengthDiff = Math.abs(safeText(correct.getWord()).length() - safeText(candidate.getWord()).length());

        if ("eng_ch".equals(mode)) {
            score += lexical * 36;
            score += meaning * 14;
        } else {
            score += meaning * 38;
            score += lexical * 12;
        }

        if (startsWithSameLetter(correct.getWord(), candidate.getWord())) score += 8;
        if (sharesSuffix(correct.getWord(), candidate.getWord())) score += 6;
        if (lengthDiff <= 2) score += 7;
        return score;
    }

    private boolean belongsToBook(Long unitId, Long bookId) {
        if (unitId == null || bookId == null) return false;
        Unit unit = unitService.getById(unitId);
        return unit != null && Objects.equals(unit.getBookId(), bookId);
    }

    private boolean startsWithSameLetter(String left, String right) {
        String a = safeText(left).toLowerCase(Locale.ROOT);
        String b = safeText(right).toLowerCase(Locale.ROOT);
        return !a.isBlank() && !b.isBlank() && a.charAt(0) == b.charAt(0);
    }

    private boolean sharesSuffix(String left, String right) {
        String a = safeText(left).toLowerCase(Locale.ROOT);
        String b = safeText(right).toLowerCase(Locale.ROOT);
        if (a.length() < 2 || b.length() < 2) return false;
        return a.substring(a.length() - 2).equals(b.substring(b.length() - 2));
    }

    private double lexicalSimilarity(String left, String right) {
        String a = safeText(left).toLowerCase(Locale.ROOT);
        String b = safeText(right).toLowerCase(Locale.ROOT);
        if (a.isBlank() || b.isBlank()) return 0;
        int matches = 0;
        int limit = Math.min(a.length(), b.length());
        for (int i = 0; i < limit; i++) {
            if (a.charAt(i) == b.charAt(i)) matches++;
        }
        Set<Character> chars = new HashSet<>();
        for (char c : a.toCharArray()) chars.add(c);
        int overlap = 0;
        for (char c : b.toCharArray()) {
            if (chars.contains(c)) overlap++;
        }
        return Math.min(1, ((double) matches / Math.max(a.length(), b.length())) + ((double) overlap / Math.max(a.length(), b.length() * 2.0)));
    }

    private double meaningSimilarity(String left, String right) {
        String a = normalizeDisplayText(left);
        String b = normalizeDisplayText(right);
        if (a.isBlank() || b.isBlank()) return 0;
        Set<String> tokensA = splitMeaningTokens(a);
        Set<String> tokensB = splitMeaningTokens(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0;
        int overlap = 0;
        for (String token : tokensA) {
            if (tokensB.contains(token)) overlap++;
        }
        return (double) overlap / Math.max(tokensA.size(), tokensB.size());
    }

    private Set<String> splitMeaningTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        String normalized = text.replaceAll("[,，;/；、]", " ");
        for (String token : normalized.split("\\s+")) {
            String value = token.trim();
            if (value.length() >= 1) tokens.add(value);
        }
        if (tokens.isEmpty()) {
            for (char c : normalized.toCharArray()) {
                if (!Character.isWhitespace(c)) tokens.add(String.valueOf(c));
            }
        }
        return tokens;
    }

    private String displayText(Word word, String mode) {
        if (word == null) return "";
        return "eng_ch".equals(mode) ? safeText(word.getMeaning()) : safeText(word.getWord());
    }

    private String normalizeDisplayText(String value) {
        return safeText(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[()（）【】\\[\\]“”\"'`·.]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private record WordScore(Word word, double score) {}

    @Override
    public int getReviewCount(String tenantId, Long userId, Long unitId) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) return 0;
        return baseMapper.countWrongReviewWords(tenantId, userId, unitId, LocalDateTime.now());
    }

    @Override
    public int getNewCount(String tenantId, Long userId, Long unitId) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) return 0;
        return baseMapper.countUnlearnedWordsInUnit(tenantId, userId, unitId);
    }

    @Override
    public StudyStatsDto getUnitStats(String tenantId, Long userId, Long unitId) {
        Unit unit = unitService.getById(unitId);
        if (unit == null) return new StudyStatsDto(0, 0, 0, false);
        int reviewCount = getReviewCount(tenantId, userId, unitId);
        int newCount = getNewCount(tenantId, userId, unitId);
        int totalCount = (int) wordService.count(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, unitId));
        int modesDone = unitModeService.countCompletedModes(tenantId, userId, unitId);
        boolean completed = (totalCount > 0 && reviewCount == 0 && newCount == 0 && modesDone >= 4);
        return new StudyStatsDto(reviewCount, newCount, totalCount, completed);
    }

    @Override
    public List<Map<String, Object>> getRecentErrorLogs(String tenantId, Long userId, int days, int limit) {
        if (tenantId == null || userId == null || days <= 0 || limit <= 0) return Collections.emptyList();
        List<String> dates = errorLogService.getErrorDates(tenantId, userId, days);
        if (dates == null || dates.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String date : dates) {
            if (result.size() >= limit) break;
            List<Map<String, Object>> byDate = errorLogService.getErrorsByDate(tenantId, userId, date);
            if (byDate == null) continue;
            for (Map<String, Object> row : byDate) {
                if (result.size() >= limit) break;
                Map<String, Object> withDate = new LinkedHashMap<>(row);
                withDate.put("date", date);
                result.add(withDate);
            }
        }
        return result;
    }

    @Override
    public List<Word> getRecentErrorWords(String tenantId, Long userId, int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Long> wordIds = baseMapper.selectRecentErrorWordIds(tenantId, userId, since, limit);
        if (wordIds == null || wordIds.isEmpty()) return Collections.emptyList();
        List<Word> words = wordService.listByIds(wordIds);
        // Keep the order returned by SQL (latest errors first)
        words.sort((a, b) -> Integer.compare(wordIds.indexOf(a.getId()), wordIds.indexOf(b.getId())));
        return words;
    }

    @Override
    public List<Word> getWeakWords(String tenantId, Long userId, Long unitId, int limit) {
        if (tenantId == null || userId == null || limit <= 0) return Collections.emptyList();
        List<StudentWordProgress> progressList = lambdaQuery()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .eq(StudentWordProgress::getIsWrong, StudentWordProgress.IS_WRONG_YES)
                .orderByDesc(StudentWordProgress::getLastErrorTime)
                .orderByAsc(StudentWordProgress::getRepetitions)
                .last("LIMIT " + limit)
                .list();
        if (progressList == null || progressList.isEmpty()) return Collections.emptyList();
        List<Long> wordIds = progressList.stream().map(StudentWordProgress::getWordId).filter(Objects::nonNull).toList();
        List<Word> words = wordService.listByIds(wordIds);
        Map<Long, Word> wordMap = words.stream().collect(Collectors.toMap(Word::getId, word -> word));
        List<Word> result = new ArrayList<>();
        for (Long wordId : wordIds) {
            Word word = wordMap.get(wordId);
            if (word == null) continue;
            if (unitId != null && !Objects.equals(word.getUnitId(), unitId)) continue;
            result.add(word);
            if (result.size() >= limit) break;
        }
        return result;
    }

    @Override
    public java.util.Map<String, Object> getStudyHeatmapWithStats(String tenantId, Long userId) {
        List<java.util.Map<String, Object>> items = baseMapper.selectStudyHeatmap(tenantId, userId);
        if (items == null) items = java.util.Collections.emptyList();
        int total = 0;
        for (java.util.Map<String, Object> m : items) {
            Object c = m.get("count");
            total += (c != null ? ((Number) c).intValue() : 0);
        }
        int consecutive = 0;
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Set<String> dateSet = new java.util.HashSet<>();
        for (java.util.Map<String, Object> m : items) {
            Object d = m.get("date");
            if (d != null) dateSet.add(String.valueOf(d));
        }
        for (int i = 0; i < 365; i++) {
            String d = today.minusDays(i).toString();
            if (dateSet.contains(d)) consecutive++;
            else break;
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        int masteredCount = baseMapper.countMasteredWords(tenantId, userId);
        result.put("items", items);
        result.put("total", total);
        result.put("consecutiveDays", consecutive);
        result.put("masteredCount", masteredCount);
        return result;
    }

    @Override
    public java.util.Map<String, Object> getMasteredWords(String tenantId, Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        int total = baseMapper.countMasteredWords(tenantId, userId);
        List<Long> wordIds = baseMapper.selectMasteredWordIds(tenantId, userId, offset, pageSize);
        List<Word> list = wordIds.isEmpty() ? java.util.Collections.emptyList() : wordService.listByIds(wordIds);
        list.sort(Comparator.comparingInt(w -> wordIds.indexOf(w.getId())));
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    private StudentWordProgress getOrCreateProgress(String tenantId, Long userId, Long wordId, LocalDateTime now) {
        StudentWordProgress progress = getOne(new LambdaQueryWrapper<StudentWordProgress>()
                .eq(StudentWordProgress::getTenantId, tenantId)
                .eq(StudentWordProgress::getUserId, userId)
                .eq(StudentWordProgress::getWordId, wordId));
        if (progress == null) {
            progress = new StudentWordProgress();
            progress.setTenantId(tenantId);
            progress.setUserId(userId);
            progress.setWordId(wordId);
            progress.setMasteryLevel(0);
            progress.setWrongCount(0);
            progress.setIsWrong(StudentWordProgress.IS_WRONG_NO);
            progress.setConsecutiveCorrectCount(0);
            progress.setStatus(StudentWordProgress.STATUS_UNLEARNED);
            progress.setLastReviewTime(now);
            progress.setNextReviewTime(now);
            progress.setLastStudyAt(now);
            save(progress);
        }
        return progress;
    }

    private static void validateSubmitArgs(String tenantId, Long userId, Long wordId) {
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId 不能为空");
        if (userId == null) throw new IllegalArgumentException("userId 不能为空");
        if (wordId == null) throw new IllegalArgumentException("wordId 不能为空");
    }

    private static void appendFilteredIds(List<Long> target, List<Long> source, Set<Long> allowedWordIds, int limit) {
        if (source == null || source.isEmpty()) return;
        for (Long id : source) {
            if (id == null) continue;
            if (allowedWordIds != null && !allowedWordIds.contains(id)) continue;
            if (!target.contains(id)) target.add(id);
            if (target.size() >= limit) return;
        }
    }

    private static LocalDateTime computeNextReviewTime(LocalDateTime now, int repetitions) {
        if (repetitions <= 1) return now.plusMinutes(10);
        int index = Math.min(repetitions - 2, REVIEW_STAGE_HOURS.length - 1);
        return now.plusHours(REVIEW_STAGE_HOURS[index]);
    }
}

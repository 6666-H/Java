package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.StudentUnitMode;
import com.study.english.entity.StudentWordModeProgress;
import com.study.english.entity.Word;
import com.study.english.mapper.StudentWordModeProgressMapper;
import com.study.english.service.StudentWordModeProgressService;
import com.study.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 单词-模式进度：与当前学习页出队口径一致，每词每类型连续2次正确视为完成 */
@Service
@RequiredArgsConstructor
public class StudentWordModeProgressServiceImpl extends ServiceImpl<StudentWordModeProgressMapper, StudentWordModeProgress>
        implements StudentWordModeProgressService {

    private final WordService wordService;

    @Override
    public int updateProgress(String tenantId, Long userId, Long wordId, Long unitId, String mode, boolean isCorrect) {
        if (tenantId == null || userId == null || wordId == null || unitId == null || mode == null) return 0;
        String normalized = normalizeMode(mode);
        if (normalized == null) return 0;

        StudentWordModeProgress p = getOne(new LambdaQueryWrapper<StudentWordModeProgress>()
                .eq(StudentWordModeProgress::getTenantId, tenantId)
                .eq(StudentWordModeProgress::getUserId, userId)
                .eq(StudentWordModeProgress::getWordId, wordId)
                .eq(StudentWordModeProgress::getMode, normalized));
        if (p == null) {
            p = new StudentWordModeProgress();
            p.setTenantId(tenantId);
            p.setUserId(userId);
            p.setWordId(wordId);
            p.setUnitId(unitId);
            p.setMode(normalized);
            p.setConsecutiveCorrect(0);
        }
        if (isCorrect) {
            int c = (p.getConsecutiveCorrect() == null ? 0 : p.getConsecutiveCorrect()) + 1;
            p.setConsecutiveCorrect(c);
        } else {
            p.setConsecutiveCorrect(0);
        }
        p.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(p);
        return p.getConsecutiveCorrect() == null ? 0 : p.getConsecutiveCorrect();
    }

    @Override
    public Map<String, Map<String, Integer>> getModeStats(String tenantId, Long userId, Long unitId) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        String[] modes = {StudentUnitMode.MODE_FLASHCARD, StudentUnitMode.MODE_ENG_CH, StudentUnitMode.MODE_CH_ENG, StudentUnitMode.MODE_SPELL};
        String[] keys = {"flashcard", "eng_ch", "ch_eng", "spell"};
        long totalCount = wordService.count(new LambdaQueryWrapper<Word>().eq(Word::getUnitId, unitId));
        for (int i = 0; i < modes.length; i++) {
            int incomplete = baseMapper.countIncompleteByUnitAndMode(tenantId, userId, unitId, modes[i]);
            Map<String, Integer> m = new HashMap<>();
            m.put("total", (int) totalCount);
            m.put("incomplete", incomplete);
            result.put(keys[i], m);
        }
        return result;
    }

    @Override
    public List<Word> getIncompleteWords(String tenantId, Long userId, Long unitId, String mode, int limit) {
        String normalized = normalizeMode(mode);
        if (tenantId == null || userId == null || unitId == null || normalized == null || limit <= 0) {
            return Collections.emptyList();
        }
        List<Long> wordIds = baseMapper.selectIncompleteWordIdsByUnitAndMode(tenantId, userId, unitId, normalized, limit);
        if (wordIds == null || wordIds.isEmpty()) return Collections.emptyList();
        List<Word> words = wordService.listByIds(wordIds);
        words.sort(java.util.Comparator.comparingInt(word -> wordIds.indexOf(word.getId())));
        return words;
    }

    @Override
    public int countIncompleteWords(String tenantId, Long userId, Long unitId, String mode) {
        String normalized = normalizeMode(mode);
        if (tenantId == null || userId == null || unitId == null || normalized == null) return 0;
        return baseMapper.countIncompleteByUnitAndMode(tenantId, userId, unitId, normalized);
    }

    @Override
    public boolean hasModeProgressRecords(String tenantId, Long userId, Long unitId, String mode) {
        String normalized = normalizeMode(mode);
        if (tenantId == null || userId == null || unitId == null || normalized == null) return false;
        return lambdaQuery()
                .eq(StudentWordModeProgress::getTenantId, tenantId)
                .eq(StudentWordModeProgress::getUserId, userId)
                .eq(StudentWordModeProgress::getUnitId, unitId)
                .eq(StudentWordModeProgress::getMode, normalized)
                .count() > 0;
    }

    @Override
    public void backfillModeCompleted(String tenantId, Long userId, Long unitId, String mode) {
        String normalized = normalizeMode(mode);
        if (tenantId == null || userId == null || unitId == null || normalized == null) return;

        List<Word> unitWords = wordService.list(new LambdaQueryWrapper<Word>()
                .eq(Word::getUnitId, unitId)
                .orderByAsc(Word::getSortOrder));
        if (unitWords.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<StudentWordModeProgress> rows = new ArrayList<>(unitWords.size());
        for (Word word : unitWords) {
            StudentWordModeProgress row = new StudentWordModeProgress();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setWordId(word.getId());
            row.setUnitId(unitId);
            row.setMode(normalized);
            row.setConsecutiveCorrect(StudentWordModeProgress.REQUIRED_CORRECT);
            row.setUpdatedAt(now);
            rows.add(row);
        }
        saveBatch(rows);
    }

    private static String normalizeMode(String mode) {
        if (mode == null || mode.isEmpty()) return null;
        String u = mode.toUpperCase().replace("-", "_");
        if ("FLASHCARD".equals(u)) return StudentUnitMode.MODE_FLASHCARD;
        if ("ENG_CH".equals(u) || "ENGTOCH".equals(u) || "ENG_TO_CH".equals(u)) return StudentUnitMode.MODE_ENG_CH;
        if ("CH_ENG".equals(u) || "CHTOENG".equals(u) || "CH_TO_ENG".equals(u)) return StudentUnitMode.MODE_CH_ENG;
        if ("SPELL".equals(u) || "SPELLING".equals(u) || "SPELLING_ERROR".equals(u)) return StudentUnitMode.MODE_SPELL;
        return null;
    }
}

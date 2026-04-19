package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.StudentUnitMode;
import com.study.english.mapper.StudentUnitModeMapper;
import com.study.english.service.StudentUnitModeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentUnitModeServiceImpl extends ServiceImpl<StudentUnitModeMapper, StudentUnitMode> implements StudentUnitModeService {

    private static final int REQUIRED_MODES = 4;

    @Override
    public void recordModeComplete(String tenantId, Long userId, Long unitId, String mode) {
        if (tenantId == null || userId == null || unitId == null || mode == null || mode.isEmpty()) return;
        String normalized = normalizeMode(mode);
        long c = count(new LambdaQueryWrapper<StudentUnitMode>()
                .eq(StudentUnitMode::getTenantId, tenantId)
                .eq(StudentUnitMode::getUserId, userId)
                .eq(StudentUnitMode::getUnitId, unitId)
                .eq(StudentUnitMode::getMode, normalized));
        if (c > 0) return;
        StudentUnitMode row = new StudentUnitMode();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setUnitId(unitId);
        row.setMode(normalized);
        row.setCreatedAt(LocalDateTime.now());
        save(row);
    }

    @Override
    public int countCompletedModes(String tenantId, Long userId, Long unitId) {
        if (tenantId == null || userId == null || unitId == null) return 0;
        return (int) count(new LambdaQueryWrapper<StudentUnitMode>()
                .eq(StudentUnitMode::getTenantId, tenantId)
                .eq(StudentUnitMode::getUserId, userId)
                .eq(StudentUnitMode::getUnitId, unitId));
    }

    @Override
    public Map<String, Boolean> getModeCompletion(String tenantId, Long userId, Long unitId) {
        Map<String, Boolean> result = new HashMap<>();
        result.put("flashcard", false);
        result.put("eng_ch", false);
        result.put("ch_eng", false);
        result.put("spell", false);
        if (tenantId == null || userId == null || unitId == null) return result;

        List<StudentUnitMode> list = list(new LambdaQueryWrapper<StudentUnitMode>()
                .eq(StudentUnitMode::getTenantId, tenantId)
                .eq(StudentUnitMode::getUserId, userId)
                .eq(StudentUnitMode::getUnitId, unitId));
        for (StudentUnitMode m : list) {
            String mode = m.getMode();
            if (StudentUnitMode.MODE_FLASHCARD.equals(mode)) result.put("flashcard", true);
            else if (StudentUnitMode.MODE_ENG_CH.equals(mode)) result.put("eng_ch", true);
            else if (StudentUnitMode.MODE_CH_ENG.equals(mode)) result.put("ch_eng", true);
            else if (StudentUnitMode.MODE_SPELL.equals(mode)) result.put("spell", true);
        }
        return result;
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return StudentUnitMode.MODE_FLASHCARD;
        String u = mode.toUpperCase().replace("-", "_");
        if ("FLASHCARD".equals(u) || "看词识义".equals(mode)) return StudentUnitMode.MODE_FLASHCARD;
        if ("ENG_CH".equals(u) || "ENGTOCH".equals(u)) return StudentUnitMode.MODE_ENG_CH;
        if ("CH_ENG".equals(u) || "CHTOENG".equals(u)) return StudentUnitMode.MODE_CH_ENG;
        if ("SPELL".equals(u)) return StudentUnitMode.MODE_SPELL;
        return u;
    }
}

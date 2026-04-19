package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.StudentUnitMode;

import java.util.Map;

/**
 * 本单元各学习类型完成记录：四种类型都完成且错题复习完，本单元才算完成。
 */
public interface StudentUnitModeService extends IService<StudentUnitMode> {

    /** 记录用户在指定单元完成了某类型练习（幂等） */
    void recordModeComplete(String tenantId, Long userId, Long unitId, String mode);

    /** 该用户在本单元已完成的类型数量（0~4） */
    int countCompletedModes(String tenantId, Long userId, Long unitId);

    /** 本单元四种学习类型完成状态：{ flashcard, eng_ch, ch_eng, spell } 均为 true 时表示本单元新词全部学完 */
    Map<String, Boolean> getModeCompletion(String tenantId, Long userId, Long unitId);
}

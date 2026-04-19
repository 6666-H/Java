package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.StudentWordModeProgress;
import com.study.english.entity.Word;

import java.util.List;
import java.util.Map;

/** 单词-模式进度：与当前学习页出队口径一致，每词每类型连续2次正确视为完成 */
public interface StudentWordModeProgressService extends IService<StudentWordModeProgress> {

    /** 更新进度：正确则+1，错误则归零。返回该词该模式连续正确次数。 */
    int updateProgress(String tenantId, Long userId, Long wordId, Long unitId, String mode, boolean isCorrect);

    /** 本单元各类型未完成/全部数量：{ flashcard: {total, incomplete}, ... } */
    Map<String, Map<String, Integer>> getModeStats(String tenantId, Long userId, Long unitId);

    /** 获取本单元某模式下未完成的单词列表。 */
    List<Word> getIncompleteWords(String tenantId, Long userId, Long unitId, String mode, int limit);

    /** 获取本单元某模式下未完成词数。 */
    int countIncompleteWords(String tenantId, Long userId, Long unitId, String mode);

    /** 当前单元当前模式是否已有模式进度记录。 */
    boolean hasModeProgressRecords(String tenantId, Long userId, Long unitId, String mode);

    /** 兼容旧数据：将当前单元当前模式的所有单词回填为已完成。 */
    void backfillModeCompleted(String tenantId, Long userId, Long unitId, String mode);
}

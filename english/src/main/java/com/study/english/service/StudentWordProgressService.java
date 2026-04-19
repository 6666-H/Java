package com.study.english.service;

import com.study.english.dto.BookProgressStatsDto;
import com.study.english.dto.CheckSpellingResult;
import com.study.english.dto.StudyQuestionOptionsDto;
import com.study.english.dto.StudySubmitResult;
import com.study.english.dto.StudyStatsDto;
import com.study.english.entity.StudentWordProgress;
import com.study.english.entity.Word;

import java.util.List;

/**
 * 学习进度与艾宾浩斯、错题收集、智能复习。
 */
public interface StudentWordProgressService extends com.baomidou.mybatisplus.extension.service.IService<StudentWordProgress> {

    /**
     * 获取下一个要学的单词：优先 next_review_time <= now 的旧词，否则该单元内未学过的新词。
     */
    Word getNextWord(String tenantId, Long userId, Long unitId);

    /**
     * 反馈更新：认识则提升 mastery_level 并设置 next_review_time；不认识则重置。
     */
    void submitFeedback(String tenantId, Long userId, Long wordId, boolean known);

    /**
     * 学习反馈提交：KNOW/DONT_KNOW/SPELLING_ERROR。不认识或拼写错误时入错题队，next_review_time=24小时后。
     * @param errorType 可选，用于 eng_ch→ENG_TO_CH、ch_eng→CH_TO_ENG 等，使错题本按类型正确归类
     */
    StudySubmitResult submitStudyFeedback(String tenantId, Long userId, Long wordId, String feedbackType, String errorType, String mode);

    /**
     * 智能复习列表：优先错题(next_review_time&lt;=now且wrong_count&gt;0)，再今日复习，最后新词。
     */
    List<Word> getNextList(String tenantId, Long userId, Long unitId, int limit, String mode);

    /**
     * 当前单词的选择题干扰项，由后端统一生成。
     */
    StudyQuestionOptionsDto getQuestionOptions(String tenantId, Long userId, Long wordId, String mode);

    /**
     * 拼写校验：忽略大小写与前后空格。不一致则增加错误权重并记入 error_log。
     */
    CheckSpellingResult checkSpelling(String tenantId, Long userId, Long wordId, String userInput);

    /**
     * 今日待复习错题数（本单元）
     */
    int getReviewCount(String tenantId, Long userId, Long unitId);

    /**
     * 待学习新词数（本单元）
     */
    int getNewCount(String tenantId, Long userId, Long unitId);

    /**
     * 本单元学习统计：待复习错题数、待学习新词数、总词数、是否全部完成。
     * 全部完成 = 新词已学完且错题也复习完（reviewCount==0 && newCount==0）。
     */
    StudyStatsDto getUnitStats(String tenantId, Long userId, Long unitId);

    /**
     * 获取账号近一周的错题（按时间倒序）
     */
    List<Word> getRecentErrorWords(String tenantId, Long userId, int days, int limit);

    /** 获取当前仍需巩固的弱词列表，可按单元筛选。 */
    List<Word> getWeakWords(String tenantId, Long userId, Long unitId, int limit);

    /**
     * 获取账号近期的错题明细（包含日期、错误类型和单词信息）
     */
    java.util.List<java.util.Map<String, Object>> getRecentErrorLogs(String tenantId, Long userId, int days, int limit);


    /**
     * 获取学习热力图数据
     */
    /** 学习热力图。返回 Map: items=[{date,count}], total=累计词数, consecutiveDays=连续打卡天数, masteredCount=已掌握词数 */
    java.util.Map<String, Object> getStudyHeatmapWithStats(String tenantId, Long userId);

    /** 已掌握单词列表（correct_count>=1），分页。返回 { list, total } */
    java.util.Map<String, Object> getMasteredWords(String tenantId, Long userId, int page, int pageSize);
}

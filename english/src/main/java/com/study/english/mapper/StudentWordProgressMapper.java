package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.StudentWordProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 艾宾浩斯：按 (tenant_id, user_id, next_review_time) 查待复习
 */
@Mapper
public interface StudentWordProgressMapper extends BaseMapper<StudentWordProgress> {

    Long selectNextReviewWordId(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now);

    List<Long> selectWrongReviewWordIds(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                                  @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now,
                                                  @Param("limit") int limit);

    List<Long> selectTodayReviewWordIds(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                                  @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now,
                                                  @Param("excludeWordIds") java.util.List<Long> excludeWordIds,
                                                  @Param("limit") int limit);

    List<Long> selectDueReviewWordIds(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                      @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now,
                                      @Param("limit") int limit);

    List<Long> selectReinforcementWordIds(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                         @Param("unitId") Long unitId,
                                         @Param("excludeWordIds") java.util.List<Long> excludeWordIds,
                                         @Param("limit") int limit);

    int countTodayNewWordsInUnit(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                @Param("unitId") Long unitId);

    int countWrongReviewWords(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                             @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now);

    int countDueReviewWords(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                           @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now);

    int countUnlearnedWordsInUnit(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                  @Param("unitId") Long unitId);

    int countMasteredInUnit(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                           @Param("unitId") Long unitId);

    int countErrorInUnit(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                        @Param("unitId") Long unitId);

    /** 书本内正确/错误总数 */
    Map<String, Object> sumCorrectWrongInBook(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                              @Param("bookId") Long bookId);

    /** 近期错题（用于首页左侧模块展示） */
    java.util.List<Long> selectRecentErrorWordIds(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                                  @Param("since") java.time.LocalDateTime since, @Param("limit") int limit);

    int countWeakWordsForDate(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                              @Param("date") String date, @Param("errorType") String errorType);

    /** 教材/单元模式：本单元已到期复习词 */
    List<Long> selectProductOverdueWordIdsInUnit(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                                 @Param("unitId") Long unitId, @Param("now") java.time.LocalDateTime now,
                                                 @Param("limit") int limit);

    /** 教材/单元模式：全局已到期复习词 */
    List<Long> selectProductOverdueWordIdsGlobal(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                                   @Param("now") java.time.LocalDateTime now, @Param("limit") int limit);

    /** 已掌握单词ID（分页） */
    List<Long> selectMasteredWordIds(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                     @Param("offset") int offset, @Param("limit") int limit);
    int countMasteredWords(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    /** 学习热力图：过去30天每天的复习/学习单词数 */
    @Select("SELECT DATE(last_study_at) as date, COUNT(*) as count " +
            "FROM student_word_progress " +
            "WHERE tenant_id = #{tenantId} AND user_id = #{userId} " +
            "AND last_study_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(last_study_at) ORDER BY date ASC")
    List<Map<String, Object>> selectStudyHeatmap(@Param("tenantId") String tenantId, @Param("userId") Long userId);
}

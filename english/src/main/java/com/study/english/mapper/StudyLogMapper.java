package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.StudyLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudyLogMapper extends BaseMapper<StudyLog> {

    /** 教材/单元模式：今日学习次数已达 2 次的 word_id 列表（同日限制） */
    @Select("SELECT word_id FROM study_log WHERE tenant_id=#{tenantId} AND user_id=#{userId} " +
            "AND DATE(created_at)=CURDATE() GROUP BY word_id HAVING COUNT(*) >= 2")
    List<Long> selectWordIdsWithTodayCountGe2(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    /** 教材/单元模式：今日完成数、正确数 */
    @Select("SELECT COUNT(*) as total, COALESCE(SUM(CASE WHEN feedback_type='KNOW' THEN 1 ELSE 0 END), 0) as correct " +
            "FROM study_log WHERE tenant_id=#{tenantId} AND user_id=#{userId} AND DATE(created_at)=CURDATE()")
    Map<String, Object> selectTodayStats(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT user_id) FROM study_log " +
            "WHERE tenant_id=#{tenantId} AND DATE(created_at)=CURDATE()")
    int countActiveStudentsToday(@Param("tenantId") String tenantId);

    @Select("SELECT DATE(created_at) as day, COUNT(DISTINCT user_id) as activeCount " +
            "FROM study_log " +
            "WHERE tenant_id=#{tenantId} AND created_at >= #{start} AND created_at <= #{end} " +
            "GROUP BY DATE(created_at) ORDER BY DATE(created_at) ASC")
    List<Map<String, Object>> selectActiveTrend(@Param("tenantId") String tenantId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}

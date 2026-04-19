package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.ErrorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ErrorLogMapper extends BaseMapper<ErrorLog> {

    /** 最近 days 天内有错题的日期列表，格式 yyyy-MM-dd，倒序（GROUP BY 与 SELECT 一致以兼容 only_full_group_by） */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS d FROM error_log " +
            "WHERE tenant_id = #{tenantId} AND user_id = #{userId} " +
            "AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d') ORDER BY d DESC")
    List<String> selectErrorDates(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("days") int days);

    /** 某日错题明细：含 word_id, word, meaning, error_type */
    @Select("SELECT e.word_id AS wordId, w.word AS word, w.meaning AS meaning, e.error_type AS errorType " +
            "FROM error_log e " +
            "INNER JOIN word w ON w.id = e.word_id " +
            "INNER JOIN student_word_progress p ON p.tenant_id = e.tenant_id AND p.user_id = e.user_id AND p.word_id = e.word_id " +
            "WHERE e.tenant_id = #{tenantId} AND e.user_id = #{userId} " +
            "AND DATE(e.created_at) = #{date} AND p.is_wrong = 1 " +
            "GROUP BY e.word_id, w.word, w.meaning, e.error_type " +
            "ORDER BY MAX(e.created_at) DESC")
    List<Map<String, Object>> selectErrorsByDate(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("date") String date);

    /** 某日错题涉及的 word_id 列表（去重，按最近错误时间倒序），用于重复训练拉取完整单词 */
    @Select("SELECT e.word_id FROM error_log e " +
            "INNER JOIN student_word_progress p ON p.tenant_id = e.tenant_id AND p.user_id = e.user_id AND p.word_id = e.word_id " +
            "WHERE e.tenant_id = #{tenantId} AND e.user_id = #{userId} AND DATE(e.created_at) = #{date} AND p.is_wrong = 1 " +
            "GROUP BY e.word_id ORDER BY MAX(e.created_at) DESC")
    List<Long> selectWordIdsByDate(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("date") String date);

    /** 按日期+类型统计错题数量，用于错题列表展示。返回 (date, error_type, cnt) */
    @Select("SELECT DATE_FORMAT(e.created_at, '%Y-%m-%d') AS date, e.error_type AS errorType, COUNT(DISTINCT e.word_id) AS cnt " +
            "FROM error_log e " +
            "INNER JOIN student_word_progress p ON p.tenant_id = e.tenant_id AND p.user_id = e.user_id AND p.word_id = e.word_id " +
            "WHERE e.tenant_id = #{tenantId} AND e.user_id = #{userId} " +
            "AND e.created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) AND p.is_wrong = 1 " +
            "GROUP BY DATE_FORMAT(e.created_at, '%Y-%m-%d'), e.error_type " +
            "ORDER BY date DESC, e.error_type")
    List<Map<String, Object>> selectErrorStatsByDate(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("days") int days);

    /** 按日期+类型统计错题数量（支持书本、单元、时间筛选），返回 (date, errorType, cnt) */
    @Select("<script>SELECT DATE_FORMAT(e.created_at, '%Y-%m-%d') AS date, e.error_type AS errorType, COUNT(DISTINCT e.word_id) AS cnt " +
            "FROM error_log e INNER JOIN word w ON w.id = e.word_id INNER JOIN unit u ON u.id = w.unit_id " +
            "WHERE e.tenant_id = #{tenantId} AND e.user_id = #{userId} " +
            "<choose><when test='startDate != null and startDate != \"\" and endDate != null and endDate != \"\"'>" +
            "AND DATE(e.created_at) &gt;= #{startDate} AND DATE(e.created_at) &lt;= #{endDate}" +
            "</when><otherwise>AND e.created_at &gt;= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)</otherwise></choose>" +
            "<if test='bookId != null'> AND u.book_id = #{bookId} </if>" +
            "<if test='unitId != null'> AND w.unit_id = #{unitId} </if>" +
            "GROUP BY DATE_FORMAT(e.created_at, '%Y-%m-%d'), e.error_type ORDER BY date DESC, e.error_type</script>")
    List<Map<String, Object>> selectErrorStatsByDateFiltered(@Param("tenantId") String tenantId, @Param("userId") Long userId,
            @Param("days") int days, @Param("startDate") String startDate, @Param("endDate") String endDate,
            @Param("bookId") Long bookId, @Param("unitId") Long unitId);
}

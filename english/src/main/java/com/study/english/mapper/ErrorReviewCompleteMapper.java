package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.ErrorReviewComplete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ErrorReviewCompleteMapper extends BaseMapper<ErrorReviewComplete> {

    @Select("SELECT DATE_FORMAT(review_date, '%Y-%m-%d') AS reviewDate, error_type AS errorType " +
            "FROM error_review_complete WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    List<Map<String, Object>> selectCompletedTypesByDate(@Param("tenantId") String tenantId, @Param("userId") Long userId);
}

package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 统计租户下用户数；查询连续 N 天未打卡学生
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    int countByTenantId(@Param("tenantId") String tenantId);

    /** 仅统计学生账号数（校长不计入名额） */
    int countStudentsByTenantId(@Param("tenantId") String tenantId);

   List<User> selectInactiveStudents(@Param("tenantId") String tenantId, @Param("role") String role, @Param("beforeTime") java.time.LocalDateTime beforeTime);
}

package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.StudentWordModeProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 单词-模式进度：与当前学习页出队口径一致，每词每类型连续2次正确视为完成。
 */
@Mapper
public interface StudentWordModeProgressMapper extends BaseMapper<StudentWordModeProgress> {

    /**
     * 统计本单元某模式下未完成词数（consecutive_correct < 2 或不存在记录）
     */
    int countIncompleteByUnitAndMode(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                     @Param("unitId") Long unitId, @Param("mode") String mode);

    /**
     * 获取本单元某模式下未完成单词ID。
     */
    java.util.List<Long> selectIncompleteWordIdsByUnitAndMode(@Param("tenantId") String tenantId, @Param("userId") Long userId,
                                                              @Param("unitId") Long unitId, @Param("mode") String mode,
                                                              @Param("limit") int limit);
}

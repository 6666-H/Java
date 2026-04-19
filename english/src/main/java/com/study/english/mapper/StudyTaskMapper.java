package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.StudyTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudyTaskMapper extends BaseMapper<StudyTask> {
}

package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.dto.CreateStudyTaskRequest;
import com.study.english.dto.StudentHomeTaskDto;
import com.study.english.entity.StudyTask;

import java.util.List;
import java.util.Map;

public interface StudyTaskService extends IService<StudyTask> {
    List<Map<String, Object>> listTasksForTenant(String tenantId);

    List<StudyTask> createTasks(String tenantId, Long operatorUserId, CreateStudyTaskRequest req);

    void cancelTask(String tenantId, Long taskId);

    List<StudentHomeTaskDto> listStudentTasks(String tenantId, Long studentId);
}

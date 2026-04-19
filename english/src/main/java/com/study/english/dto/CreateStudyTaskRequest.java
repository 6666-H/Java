package com.study.english.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateStudyTaskRequest {
    @NotEmpty(message = "学生不能为空")
    private List<Long> studentIds;

    @NotEmpty(message = "单元不能为空")
    private List<Long> unitIds;
}

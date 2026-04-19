package com.study.english.dto;

import lombok.Data;

@Data
public class StudentHomeTaskDto {
    private Long taskId;
    private Long unitId;
    private String unitName;
    private String bookName;
    private String status;
    private Integer completedModes;
    private Integer totalModes;
    private String assignedAt;
}

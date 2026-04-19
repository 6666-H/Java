package com.study.english.dto;

import lombok.Data;

@Data
public class TenantReportRowDto {
    private Long studentId;
    private String realName;
    private String username;
    private String studentNo;
    private String gradeClass;
    private int learningDays;
    private int streakDays;
    private int masteredCount;
    private int totalErrors;
    private double flashcardRate;
    private double engChRate;
    private double chEngRate;
    private double spellRate;
    private String lastStudyAt;
}

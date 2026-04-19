package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentStatsDto {
    private int consecutiveDays;
    private int learningDays;
    private int masteredCount;
}

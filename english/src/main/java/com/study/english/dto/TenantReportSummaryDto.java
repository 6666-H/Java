package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantReportSummaryDto {
    private int totalStudents;
    private double studentGrowthRate;
    private double averageProgress;
    private String averageProgressLabel;
    private long averageMastered;
    private double averageMasteredDelta;
    private double averageAccuracy;
    private double averageAccuracyDelta;
    private double averageDurationMinutes;
    private String averageDurationLabel;
    private int activeStudentCount;
    private double activeStudentDelta;
    private List<String> chartLabels;
    private List<Integer> chartMasteredValues;
    private List<Integer> chartDurationValues;
    private String insightSummary;
    private String focusTopic;
    private String suggestion;
}

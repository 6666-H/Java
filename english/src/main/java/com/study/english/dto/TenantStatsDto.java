package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatsDto {

    private int totalStudents;
    private int activeToday;
    private int totalWordsLearned;
    private List<Integer> activeTrend;
    private int inactiveStudents;
    private double studentGrowthRate;
    private double activeStudentDelta;
    private double taskCompletionRate;
    private double taskCompletionRateDelta;
    private long totalLearningMinutes;
    private List<String> trendLabels;
    private List<Integer> trendValues;
    private List<ActivityItemDto> recentActivities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItemDto {
        private String key;
        private String primary;
        private String time;
        private String avatar;
        private String color;
        private LocalDateTime sortTime;
    }
}

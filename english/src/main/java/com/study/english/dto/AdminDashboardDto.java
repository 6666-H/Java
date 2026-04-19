package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private int totalTenants;
    private int activeTenants;
    private int todayActiveStudents;
    private int totalMasteredWords;
    private List<String> trendLabels;
    private List<Integer> trendValues;
    private List<TenantActiveRankDto> tenantRanks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantActiveRankDto {
        private String tenantId;
        private String tenantName;
        private int activeStudents;
    }
}

package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTenantOverviewDto {
    private int totalCount;
    private int activeStudentCount;
    private int expiringCount;
    private BigDecimal estimatedRevenue;
    private double totalCountGrowthRate;
    private double activeStudentGrowthRate;
    private List<TenantRow> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantRow {
        private String id;
        private String name;
        private String contactName;
        private String contactPhone;
        private Integer accountQuota;
        private Integer usedCount;
        private Integer activeStudents;
        private Integer authorizedBookCount;
        private Integer status;
        private String orgAdminUsername;
        private String orgAdminPhone;
        private String planName;
        private String expireStatus;
        private Integer usagePercent;
        private Long daysRemaining;
        private java.time.LocalDateTime expireTime;
    }
}

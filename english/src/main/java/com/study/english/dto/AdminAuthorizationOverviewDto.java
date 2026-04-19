package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthorizationOverviewDto {
    private int totalCount;
    private int activeCount;
    private int expiringCount;
    private double growthRate;
    private long filteredCount;
    private long page;
    private long pageSize;
    private List<Record> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Record {
        private String id;
        private String tenantId;
        private String tenantName;
        private Long bookId;
        private String bookName;
        private String bookLabel;
        private String authType;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime expireTime;
        private long daysRemaining;
    }
}

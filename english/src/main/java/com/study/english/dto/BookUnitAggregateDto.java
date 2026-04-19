package com.study.english.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookUnitAggregateDto {
    private Long bookId;
    private Integer unitCount;
    private LocalDateTime latestUnitUpdatedAt;
}

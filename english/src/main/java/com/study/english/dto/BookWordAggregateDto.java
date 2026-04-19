package com.study.english.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookWordAggregateDto {
    private Long bookId;
    private Integer wordCount;
    private LocalDateTime latestWordUpdatedAt;
}

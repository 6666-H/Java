package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookOverviewDto {
    private int totalBookCount;
    private int totalWordCount;
    private int totalUnitCount;
    private int capacityRate;
    private int filteredCount;
    private int page;
    private int pageSize;
    private List<BookCard> books;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookCard {
        private Long id;
        private String name;
        private String grade;
        private String versionName;
        private String description;
        private String coverUrl;
        private Integer unitCount;
        private Integer wordCount;
        private String status;
        private LocalDateTime updatedAt;
    }
}

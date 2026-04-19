package com.study.english.dto;

import lombok.Data;

@Data
public class SaveStageResultRequest {
    private Long unitId;
    private String stage;
    private String bookName;
    private String unitName;
    private Integer totalQuestions;
    private Integer correctAttempts;
    private Integer wrongAttempts;
    private Integer firstRoundCorrect;
    private Integer stabilizedCount;
    private Integer durationSeconds;
    private Boolean starReward;
}

package com.study.english.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentUnitWordDto {
    private Long wordId;
    private String word;
    private String phonetic;
    private String pos;
    private String meaning;
    private String exampleEn;
    private String exampleZh;
    private String audioUrl;
    private String imageUrl;
    private String exampleAudio;
    private String status;
    private String reviewState;
    private Integer reviewStage;
    private String nextReviewTime;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer difficulty;
    private String spellingPattern;
    private List<String> confusionWords;
}

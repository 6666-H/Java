package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyQuestionOptionsDto {

    private Long wordId;
    private String mode;
    private List<StudyChoiceOptionDto> options;
}

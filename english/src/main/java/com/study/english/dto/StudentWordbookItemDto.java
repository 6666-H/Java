package com.study.english.dto;

import lombok.Data;

@Data
public class StudentWordbookItemDto {
    private Long wordId;
    private String word;
    private String phonetic;
    private String pos;
    private String meaning;
    private String exampleEn;
    private String exampleZh;
    private String status;
    private Long unitId;
    private String unitName;
    private Long bookId;
    private String bookName;
}

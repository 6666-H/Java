package com.study.english.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 校长端：按学生、日期展示的学习进度/错误记录（含学习类型、记录类型、错误类型，支持筛选）。
 */
@Data
public class LearningProgressRecordDto {

    private Long userId;
    private String username;
    private String realName;
    private LocalDate studyDate;
    private String bookName;
    private String unitName;
    private String word;
    private Long wordId;
    private LocalDateTime lastStudyAt;

    /** 学习类型：FLASHCARD/ENG_CH/CH_ENG/SPELL（看词识义/看英选中/看中选英/拼写），无则 null */
    private String studyType;
    /** 记录类型：progress=学习记录, error=错误记录 */
    private String recordType;
    /** 错误类型：DONT_KNOW=不认识, SPELLING_ERROR=拼写错误，仅当 recordType=error 时有值 */
    private String errorType;
    /** 学生具体操作：选题选了什么/拼写输入了什么，错时有值 */
    private String userInput;
}

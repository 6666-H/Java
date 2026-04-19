package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录：每次学习反馈一条，含学习类型（看词识义/看英选中/看中选英/拼写）与反馈类型，供校长端进度与筛选。
 */
@Data
@TableName("study_log")
public class StudyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("word_id")
    private Long wordId;

    /** 学习类型：FLASHCARD/ENG_CH/CH_ENG/SPELL，对应 看词识义/看英选中/看中选英/拼写 */
    @TableField("mode")
    private String mode;

    /** KNOW / DONT_KNOW / SPELLING_ERROR */
    @TableField("feedback_type")
    private String feedbackType;

    /** 学生具体操作： eng_ch/ch_eng=选错的选项内容, spell=拼写的输入, 错时有值 */
    @TableField("user_input")
    private String userInput;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public static final String MODE_FLASHCARD = "FLASHCARD";
    public static final String MODE_ENG_CH = "ENG_CH";
    public static final String MODE_CH_ENG = "CH_ENG";
    public static final String MODE_SPELL = "SPELL";
    public static final String FEEDBACK_KNOW = "KNOW";
    public static final String FEEDBACK_DONT_KNOW = "DONT_KNOW";
    public static final String FEEDBACK_SPELLING_ERROR = "SPELLING_ERROR";
}

package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_session")
public class StudySession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("unit_id")
    private Long unitId;

    @TableField("stage")
    private String stage;

    @TableField("words_json")
    private String wordsJson;

    @TableField("queue_json")
    private String queueJson;

    @TableField("error_ids_json")
    private String errorIdsJson;

    @TableField("user_input_json")
    private String userInputJson;

    @TableField("first_attempt_seen_json")
    private String firstAttemptSeenJson;

    @TableField("first_attempt_correct_json")
    private String firstAttemptCorrectJson;

    @TableField("initial_count")
    private Integer initialCount;

    @TableField("answered_count")
    private Integer answeredCount;

    @TableField("first_round_correct")
    private Integer firstRoundCorrect;

    @TableField("correct_attempts")
    private Integer correctAttempts;

    @TableField("wrong_attempts")
    private Integer wrongAttempts;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_stage_result")
public class StudyStageResult {

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

    @TableField("book_name")
    private String bookName;

    @TableField("unit_name")
    private String unitName;

    @TableField("total_questions")
    private Integer totalQuestions;

    @TableField("correct_attempts")
    private Integer correctAttempts;

    @TableField("wrong_attempts")
    private Integer wrongAttempts;

    @TableField("first_round_correct")
    private Integer firstRoundCorrect;

    @TableField("stabilized_count")
    private Integer stabilizedCount;

    @TableField("duration_seconds")
    private Integer durationSeconds;

    @TableField("star_reward")
    private Integer starReward;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

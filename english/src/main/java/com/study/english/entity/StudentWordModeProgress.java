package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 单词-模式进度：与当前学习页出队口径一致，每词每类型连续2次正确视为完成 */
@Data
@TableName("student_word_mode_progress")
public class StudentWordModeProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("word_id")
    private Long wordId;

    @TableField("unit_id")
    private Long unitId;

    @TableField("mode")
    private String mode;

    @TableField("consecutive_correct")
    private Integer consecutiveCorrect;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public static final int REQUIRED_CORRECT = 2;
}

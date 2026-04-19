package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 错题复习完成记录：用户完成某日错题重复训练后写入，用于智能归纳/历史错题划分。
 */
@Data
@TableName("error_review_complete")
public class ErrorReviewComplete {

    public static final String TYPE_ALL = "ALL";

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("review_date")
    private LocalDate reviewDate;

    @TableField("error_type")
    private String errorType;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

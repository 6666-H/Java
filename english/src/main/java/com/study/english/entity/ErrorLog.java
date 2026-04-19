package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错误记录：用户点击「不认识」或「拼写错误」时写入
 */
@Data
@TableName("error_log")
public class ErrorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("word_id")
    private Long wordId;

    /** DONT_KNOW / SPELLING_ERROR */
    @TableField("error_type")
    private String errorType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public static final String TYPE_DONT_KNOW = "DONT_KNOW";
    public static final String TYPE_SPELLING_ERROR = "SPELLING_ERROR";
}

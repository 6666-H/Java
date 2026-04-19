package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户书本授权：记录哪些租户可以使用哪些书
 */
@Data
@TableName("tenant_book_auth")
public class TenantBookAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("book_id")
    private Long bookId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

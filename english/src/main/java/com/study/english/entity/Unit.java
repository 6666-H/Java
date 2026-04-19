package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局单元（无 tenant_id）
 */
@Data
@TableName("unit")
public class Unit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书本ID */
    @TableField("book_id")
    private Long bookId;

    /** 单元名 */
    @TableField("name")
    private String name;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

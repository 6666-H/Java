package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局书本（无 tenant_id）
 */
@Data
@TableName("book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 版本（如人教版、外研版） */
    @TableField("version_name")
    private String versionName;

    /** 年级（如三年级、四年级） */
    @TableField("grade")
    private String grade;

    /** 册别/书名 */
    @TableField("name")
    private String name;

    /** 封面图 URL */
    @TableField("cover_url")
    private String coverUrl;

    /** 简介 */
    @TableField("description")
    private String description;

    /** 版本号，更新词库后递增以同步所有租户 */
    @TableField("version")
    private Integer version;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

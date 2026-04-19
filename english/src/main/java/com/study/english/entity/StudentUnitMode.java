package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本单元各学习类型完成记录：只有四种类型都完成且错题复习完，本单元才算完成。
 * 表结构：student_unit_mode (id, tenant_id, user_id, unit_id, mode, created_at)
 * 建议唯一约束：UNIQUE(tenant_id, user_id, unit_id, mode)
 */
@Data
@TableName("student_unit_mode")
public class StudentUnitMode {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("unit_id")
    private Long unitId;

    /** 学习类型：FLASHCARD / ENG_CH / CH_ENG / SPELL */
    @TableField("mode")
    private String mode;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public static final String MODE_FLASHCARD = "FLASHCARD";
    public static final String MODE_ENG_CH = "ENG_CH";
    public static final String MODE_CH_ENG = "CH_ENG";
    public static final String MODE_SPELL = "SPELL";
}

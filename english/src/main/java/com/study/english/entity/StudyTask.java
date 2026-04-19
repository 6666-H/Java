package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_task")
public class StudyTask {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CANCELLED = "cancelled";

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("student_id")
    private Long studentId;

    @TableField("unit_id")
    private Long unitId;

    @TableField("assigned_by")
    private Long assignedBy;

    @TableField("status")
    private String status;

    @TableField("assigned_at")
    private LocalDateTime assignedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

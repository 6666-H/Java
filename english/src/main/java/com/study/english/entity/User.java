package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（含 tenant_id；超级管理员 tenant_id 为 null）
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID=手机号，超级管理员为 null */
    @TableField("tenant_id")
    private String tenantId;

    /** 登录名 */
    @TableField("username")
    private String username;

    /** 密码哈希 */
    @TableField("password_hash")
    private String passwordHash;

    /** 密码明文（与哈希同时存储） */
    @TableField("password_plain")
    private String passwordPlain;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 学号/编号 */
    @TableField("student_no")
    private String studentNo;

    /** 年级/班级 */
    @TableField("grade_class")
    private String gradeClass;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像 */
    @TableField("avatar")
    private String avatar;

    /** 最近学习课本 */
    @TableField("last_study_book_id")
    private Long lastStudyBookId;

    /** 最近学习单元 */
    @TableField("last_study_unit_id")
    private Long lastStudyUnitId;

    /** 最近学习时间 */
    @TableField("last_study_at")
    private LocalDateTime lastStudyAt;

    /** 是否首次登录强制改密 */
    @TableField("must_change_pwd")
    private Integer mustChangePwd;

    /** 角色：SUPER_ADMIN / ORG_ADMIN / STUDENT */
    @TableField("role")
    private String role;

    /** 最后活跃时间(学生打卡/学习) */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    /** 登录令牌版本，每次登录递增，实现单设备登录 */
    @TableField("token_version")
    private Integer tokenVersion;

    /** 状态：0-禁用 1-启用 */
    @TableField("status")
    private Integer status;

    /** 软删除：0-正常 1-已删除 */
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;

    /** 软删除时间 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 角色常量 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ORG_ADMIN = "ORG_ADMIN";
    /** @deprecated 使用 ORG_ADMIN */
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";

    /** 状态常量 */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
}

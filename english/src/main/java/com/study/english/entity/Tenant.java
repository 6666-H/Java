package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户表
 */
@Data
@TableName("tenant")
public class Tenant {

    /** 租户ID = 手机号 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 租户名称/机构名 */
    @TableField("name")
    private String name;

    /** 联系人 */
    @TableField("contact_name")
    private String contactName;

    /** 联系电话 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 账号额度（可创建学生数） */
    @TableField("account_quota")
    private Integer accountQuota;

    /** 到期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

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

    /** 状态常量 */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
}

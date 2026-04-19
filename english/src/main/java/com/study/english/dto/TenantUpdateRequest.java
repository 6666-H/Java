package com.study.english.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 超级管理员：更新租户的账号上限与到期日期。
 * account_quota / max_student_count 二选一；expire_time / service_end_date 二选一。
 */
@Data
public class TenantUpdateRequest {

    /** 账号上限（对应 account_quota，与 maxStudentCount 二选一） */
    private Integer accountQuota;

    /** 校长名额上限（同 accountQuota，前端可传此名） */
    private Integer maxStudentCount;

    /** 到期日期（对应 expire_time，与 serviceEndDate 二选一） */
    private LocalDateTime expireTime;

    /** 服务到期日（同 expireTime，前端可传此名） */
    private LocalDateTime serviceEndDate;

    /** 租户名称 */
    private String name;
    /** 联系人 */
    private String contactName;
    /** 联系电话 */
    private String contactPhone;
    /** 状态：0 禁用 1 启用 */
    private Integer status;
}

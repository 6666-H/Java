package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租户管理员：当前租户信息，含到期与“剩余 7 天”提示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoDto {

    /** 租户 ID（手机号） */
    private String id;
    /** 租户名称 */
    private String name;
    /** 到期时间 */
    private LocalDateTime expireTime;
    /** 剩余天数，已过期为负数 */
    private long daysRemaining;
    /** 剩余 ≤7 天时为 true，用于后台显著提示“服务即将到期” */
    private boolean expiringSoon;
    private Integer status;
}

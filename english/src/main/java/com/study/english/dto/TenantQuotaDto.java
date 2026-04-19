package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 租户名额：已用/总名额。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantQuotaDto {

    /** 已用名额（已创建学生数） */
    private int usedCount;
    /** 总名额（账号额度） */
    private int totalQuota;

    public boolean isFull() {
        return totalQuota <= 0 || usedCount >= totalQuota;
    }
}

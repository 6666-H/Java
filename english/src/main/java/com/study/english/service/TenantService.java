package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.Tenant;

public interface TenantService extends IService<Tenant> {

    /** 软删除租户（设置 deleted=1, deleted_at=NOW()） */
    void softDeleteTenant(String tenantId);
}

package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.Tenant;
import com.study.english.mapper.TenantMapper;
import com.study.english.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDeleteTenant(String tenantId) {
        if (tenantId == null) return;
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<Tenant>()
                .eq(Tenant::getId, tenantId)
                .set(Tenant::getDeleted, 1)
                .set(Tenant::getDeletedAt, now));
    }
}

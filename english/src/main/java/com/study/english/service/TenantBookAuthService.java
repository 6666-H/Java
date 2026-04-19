package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.TenantBookAuth;

import java.util.List;

public interface TenantBookAuthService extends IService<TenantBookAuth> {

    void grantBookToTenant(String tenantId, Long bookId);

    void revokeBookFromTenant(String tenantId, Long bookId);

    List<Long> listBookIdsByTenantId(String tenantId);

    void replaceBooksForTenant(String tenantId, List<Long> bookIds);
}

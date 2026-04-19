package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.TenantBookAuth;
import com.study.english.mapper.TenantBookAuthMapper;
import com.study.english.service.TenantBookAuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TenantBookAuthServiceImpl extends ServiceImpl<TenantBookAuthMapper, TenantBookAuth> implements TenantBookAuthService {

    @Override
    public void grantBookToTenant(String tenantId, Long bookId) {
        long c = count(new LambdaQueryWrapper<TenantBookAuth>()
                .eq(TenantBookAuth::getTenantId, tenantId)
                .eq(TenantBookAuth::getBookId, bookId));
        if (c > 0) return;
        TenantBookAuth tb = new TenantBookAuth();
        tb.setTenantId(tenantId);
        tb.setBookId(bookId);
        save(tb);
    }

    @Override
    public void revokeBookFromTenant(String tenantId, Long bookId) {
        remove(new LambdaQueryWrapper<TenantBookAuth>()
                .eq(TenantBookAuth::getTenantId, tenantId)
                .eq(TenantBookAuth::getBookId, bookId));
    }

    @Override
    public List<Long> listBookIdsByTenantId(String tenantId) {
        return list(new LambdaQueryWrapper<TenantBookAuth>().eq(TenantBookAuth::getTenantId, tenantId))
                .stream().map(TenantBookAuth::getBookId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceBooksForTenant(String tenantId, List<Long> bookIds) {
        remove(new LambdaQueryWrapper<TenantBookAuth>().eq(TenantBookAuth::getTenantId, tenantId));
        if (bookIds == null || bookIds.isEmpty()) return;
        Set<Long> distinctIds = bookIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.isEmpty()) return;
        List<TenantBookAuth> authList = distinctIds.stream().map(bookId -> {
            TenantBookAuth auth = new TenantBookAuth();
            auth.setTenantId(tenantId);
            auth.setBookId(bookId);
            return auth;
        }).toList();
        saveBatch(authList);
    }
}

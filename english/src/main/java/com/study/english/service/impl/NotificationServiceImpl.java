package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.Notification;
import com.study.english.mapper.NotificationMapper;
import com.study.english.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public Notification createTaskAssignedNotification(String tenantId, Long userId, String content) {
        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setUserId(userId);
        notification.setType(Notification.TYPE_TASK_ASSIGNED);
        notification.setContent(content);
        notification.setIsRead(0);
        save(notification);
        return notification;
    }

    @Override
    public List<Notification> listByUser(String tenantId, Long userId) {
        return list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTenantId, tenantId)
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    @Override
    public int countUnread(String tenantId, Long userId) {
        return lambdaQuery()
                .eq(Notification::getTenantId, tenantId)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .count().intValue();
    }

    @Override
    public void markAllRead(String tenantId, Long userId) {
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getTenantId, tenantId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }
}

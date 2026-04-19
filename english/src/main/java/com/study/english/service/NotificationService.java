package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {
    Notification createTaskAssignedNotification(String tenantId, Long userId, String content);

    List<Notification> listByUser(String tenantId, Long userId);

    int countUnread(String tenantId, Long userId);

    void markAllRead(String tenantId, Long userId);
}

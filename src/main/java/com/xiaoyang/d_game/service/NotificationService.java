package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.entity.Notification;

public interface NotificationService extends IService<Notification> {

    void sendNotification(Long receiverId, Long senderId, Integer type, String title, String content,
                          Integer targetType, Long targetId);

    PageResult<Notification> pageNotifications(Long page, Long size);

    long unreadCount();

    void markRead(Long notificationId);

    void markAllRead();
}

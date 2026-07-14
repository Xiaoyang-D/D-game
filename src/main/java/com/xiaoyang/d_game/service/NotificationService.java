package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.entity.Notification;

/**
 * 站内通知服务接口。
 *
 * <p>业务模块通过 {@link #sendNotification} 创建通知；用户侧通过分页、未读数和标记已读接口消费通知。</p>
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 创建一条站内通知。
     *
     * @param receiverId 接收者用户 ID
     * @param senderId 触发通知的用户 ID，系统通知可为空
     * @param type 通知类型编码
     * @param title 通知标题
     * @param content 通知正文
     * @param targetType 关联目标类型，可为空
     * @param targetId 关联目标 ID，可为空
     */
    void sendNotification(Long receiverId, Long senderId, Integer type, String title, String content,
                          Integer targetType, Long targetId);

    /**
     * 分页查询当前用户通知。
     */
    PageResult<Notification> pageNotifications(Long page, Long size);

    /**
     * 查询当前用户未读通知数量。
     */
    long unreadCount();

    /**
     * 标记当前用户的一条通知为已读。
     */
    void markRead(Long notificationId);

    /**
     * 标记当前用户所有通知为已读。
     */
    void markAllRead();
}

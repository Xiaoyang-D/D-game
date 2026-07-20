package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.entity.Notification;
import com.xiaoyang.d_game.mapper.NotificationMapper;
import com.xiaoyang.d_game.messaging.notification.NotificationEvent;
import com.xiaoyang.d_game.messaging.notification.NotificationEventPublisher;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 站内通知业务实现。
 *
 * <p>创建通知已经改为 RabbitMQ 异步模式：业务线程发布通知事件，消费者并发落库。
 * 查询、未读数和标记已读仍由该服务同步处理。</p>
 */
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    /** 未读数量缓存 key 前缀。 */
    private static final String UNREAD_KEY_PREFIX = "notify:unread:";

    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    /**
     * 发布通知事件。
     *
     * <p>如果当前方法处在数据库事务中，则注册 afterCommit 回调，等主业务提交成功后再投递 RabbitMQ。
     * 这样可以避免主业务回滚但通知已经发出的不一致。</p>
     */
    public void sendNotification(Long receiverId, Long senderId, Integer type, String title, String content,
                                 Integer targetType, Long targetId) {
        if (receiverId == null || Objects.equals(receiverId, senderId)) {
            // 没有接收者或自己触发给自己的通知不入库，减少无意义消息。
            return;
        }
        NotificationEvent event = NotificationEvent.of(receiverId, senderId, type, title, content, targetType, targetId);
        publishAfterCommit(event);
    }

    @Override
    /**
     * 分页查询当前用户通知。
     */
    public PageResult<Notification> pageNotifications(Long page, Long size) {
        Long userId = currentUserId();
        Page<Notification> result = page(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .orderByDesc(Notification::getGmtCreate));
        return PageResult.of(result);
    }

    @Override
    /**
     * 查询当前用户未读数。
     *
     * <p>优先读 Redis，缓存没有或异常时回源数据库，并把结果写回 Redis。</p>
     */
    public long unreadCount() {
        Long userId = currentUserId();
        try {
            String cached = stringRedisTemplate.opsForValue().get(unreadKey(userId));
            if (cached != null) {
                return Long.parseLong(cached);
            }
        } catch (Exception e) {
            log.warn("读取未读计数缓存失败, userId={}", userId, e);
        }
        long count = count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0));
        try {
            stringRedisTemplate.opsForValue().set(unreadKey(userId), String.valueOf(count));
        } catch (Exception e) {
            log.warn("写入未读计数缓存失败, userId={}", userId, e);
        }
        return count;
    }

    @Override
    /**
     * 标记单条通知为已读。
     */
    public void markRead(Long notificationId) {
        Long userId = currentUserId();
        Notification notification = getById(notificationId);
        if (notification == null || !Objects.equals(notification.getReceiverId(), userId)) {
            // 不存在或不属于当前用户都返回 NOT_FOUND，避免泄露其他用户通知 ID 是否存在。
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            updateById(notification);
            decrUnread(userId);
        }
    }

    @Override
    /**
     * 标记当前用户全部通知已读。
     */
    public void markAllRead() {
        Long userId = currentUserId();
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        try {
            stringRedisTemplate.opsForValue().set(unreadKey(userId), "0");
        } catch (Exception e) {
            log.warn("重置未读计数失败, userId={}", userId, e);
        }
    }

    /**
     * 递减未读数缓存。
     *
     * <p>并发标记或缓存脏数据可能导致负数，这里主动校正为 0。</p>
     */
    private void decrUnread(Long userId) {
        try {
            Long current = stringRedisTemplate.opsForValue().increment(unreadKey(userId), -1);
            if (current != null && current < 0) {
                stringRedisTemplate.opsForValue().set(unreadKey(userId), "0");
            }
        } catch (Exception e) {
            log.warn("递减未读计数失败, userId={}", userId, e);
        }
    }

    /**
     * 在事务提交后发布消息；没有事务时立即发布。
     */
    private void publishAfterCommit(NotificationEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationEventPublisher.publish(event);
                }
            });
            return;
        }
        notificationEventPublisher.publish(event);
    }

    /**
     * 获取当前登录用户。
     */
    private Long currentUserId() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 构造当前用户未读数缓存 key。
     */
    private String unreadKey(Long userId) {
        return UNREAD_KEY_PREFIX + userId;
    }
}

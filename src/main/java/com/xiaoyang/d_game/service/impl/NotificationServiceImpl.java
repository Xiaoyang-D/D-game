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
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private static final String UNREAD_KEY_PREFIX = "notify:unread:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendNotification(Long receiverId, Long senderId, Integer type, String title, String content,
                                 Integer targetType, Long targetId) {
        if (receiverId == null || Objects.equals(receiverId, senderId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content == null ? "" : content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setIsRead(0);
        save(notification);
        try {
            stringRedisTemplate.opsForValue().increment(unreadKey(receiverId));
        } catch (Exception e) {
            log.warn("更新未读计数失败, receiverId={}", receiverId, e);
        }
    }

    @Override
    public PageResult<Notification> pageNotifications(Long page, Long size) {
        Long userId = currentUserId();
        Page<Notification> result = page(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .orderByDesc(Notification::getGmtCreate));
        return PageResult.of(result);
    }

    @Override
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
    public void markRead(Long notificationId) {
        Long userId = currentUserId();
        Notification notification = getById(notificationId);
        if (notification == null || !Objects.equals(notification.getReceiverId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            updateById(notification);
            decrUnread(userId);
        }
    }

    @Override
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

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String unreadKey(Long userId) {
        return UNREAD_KEY_PREFIX + userId;
    }
}

package com.xiaoyang.d_game.messaging.notification;

import com.xiaoyang.d_game.entity.Notification;
import com.xiaoyang.d_game.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;

/**
 * 通知事件处理器。
 *
 * <p>消费者拿到 RabbitMQ 消息后，由这里完成真正的业务副作用：写入通知表、更新未读数缓存。
 * 处理失败会向上抛出异常，使消息进入死信队列。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    /** 未读数量缓存 key 前缀。 */
    private static final String UNREAD_KEY_PREFIX = "notify:unread:";

    /** 通知事件消费幂等 key 前缀。 */
    private static final String PROCESSED_EVENT_KEY_PREFIX = "notify:event:processed:";

    /** 幂等记录保留时间。 */
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);

    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 处理通知事件。
     *
     * <p>使用 eventId 做幂等：同一消息被重复投递时只会处理一次。
     * 如果数据库写入失败，会清理幂等标记，让 RabbitMQ 重试或死信重放时仍有机会重新处理。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(NotificationEvent event) {
        if (event.getReceiverId() == null || Objects.equals(event.getReceiverId(), event.getSenderId())) {
            log.debug("忽略无效或自发自收的通知事件, eventId={}", event.getEventId());
            return;
        }
        if (!markProcessing(event)) {
            log.debug("通知事件已处理过, eventId={}", event.getEventId());
            return;
        }
        try {
            Notification notification = new Notification();
            notification.setReceiverId(event.getReceiverId());
            notification.setSenderId(event.getSenderId());
            notification.setType(event.getType());
            notification.setTitle(event.getTitle());
            notification.setContent(event.getContent() == null ? "" : event.getContent());
            notification.setTargetType(event.getTargetType());
            notification.setTargetId(event.getTargetId());
            notification.setIsRead(0);
            notificationMapper.insert(notification);
            incrementUnread(event.getReceiverId());
        } catch (RuntimeException e) {
            clearProcessingMark(event);
            throw e;
        }
    }

    /**
     * 标记事件正在处理。
     *
     * @return true 表示本次应继续处理，false 表示同一事件已经处理过
     */
    private boolean markProcessing(NotificationEvent event) {
        try {
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(processedKey(event.getEventId()), "1", PROCESSED_EVENT_TTL);
            return !Boolean.FALSE.equals(first);
        } catch (RuntimeException e) {
            log.warn("通知事件幂等标记写入 Redis 失败，将继续处理, eventId={}", event.getEventId(), e);
            return true;
        }
    }

    /**
     * 数据库写入失败时清理幂等标记，避免下次重放被误判为已处理。
     */
    private void clearProcessingMark(NotificationEvent event) {
        try {
            stringRedisTemplate.delete(processedKey(event.getEventId()));
        } catch (RuntimeException ex) {
            log.warn("通知事件幂等标记清理失败, eventId={}", event.getEventId(), ex);
        }
    }

    /**
     * 增加用户未读通知数。
     *
     * <p>未读数缓存失败不影响通知落库，因为数据库才是通知列表的事实来源。</p>
     */
    private void incrementUnread(Long receiverId) {
        try {
            stringRedisTemplate.opsForValue().increment(unreadKey(receiverId));
        } catch (RuntimeException e) {
            log.warn("更新未读计数失败, receiverId={}", receiverId, e);
        }
    }

    /**
     * 构造未读数缓存 key。
     */
    private String unreadKey(Long userId) {
        return UNREAD_KEY_PREFIX + userId;
    }

    /**
     * 构造消息幂等 key。
     */
    private String processedKey(String eventId) {
        return PROCESSED_EVENT_KEY_PREFIX + eventId;
    }
}

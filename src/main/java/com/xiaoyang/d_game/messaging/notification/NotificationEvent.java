package com.xiaoyang.d_game.messaging.notification;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 站内通知事件。
 *
 * <p>这是 RabbitMQ 中传递的消息体。它只保存创建通知所需的最小信息，
 * 消费者拿到事件后再落库为 Notification 记录。</p>
 */
@Data
public class NotificationEvent implements Serializable {

    /** 事件唯一 ID，用于消息追踪和消费幂等。 */
    private String eventId = UUID.randomUUID().toString();

    /** 事件版本，方便后续消息结构升级时做兼容。 */
    private Integer version = 1;

    /** 事件产生时间。 */
    private LocalDateTime occurredAt = LocalDateTime.now();

    /** 通知接收者用户 ID。 */
    private Long receiverId;

    /** 触发通知的用户 ID；系统通知可为空。 */
    private Long senderId;

    /** 通知类型编码。 */
    private Integer type;

    /** 通知标题。 */
    private String title;

    /** 通知正文。 */
    private String content = "";

    /** 关联目标类型，可为空。 */
    private Integer targetType;

    /** 关联目标 ID，可为空。 */
    private Long targetId;

    /**
     * 构造通知事件。
     */
    public static NotificationEvent of(Long receiverId, Long senderId, Integer type, String title, String content,
                                       Integer targetType, Long targetId) {
        NotificationEvent event = new NotificationEvent();
        event.setReceiverId(receiverId);
        event.setSenderId(senderId);
        event.setType(type);
        event.setTitle(title);
        event.setContent(content == null ? "" : content);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        return event;
    }
}

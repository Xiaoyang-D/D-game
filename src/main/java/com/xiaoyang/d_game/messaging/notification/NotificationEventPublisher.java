package com.xiaoyang.d_game.messaging.notification;

import com.xiaoyang.d_game.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知事件发布器。
 *
 * <p>业务服务只依赖这个发布器投递事件，不关心交换机、路由键等 RabbitMQ 细节。
 * RabbitMQ 不可用时只记录错误，不回滚主业务，避免通知系统拖垮点赞、评论、审核等核心操作。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布通知事件。
     */
    public void publish(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EVENT_EXCHANGE,
                    RabbitMqConfig.NOTIFICATION_ROUTING_KEY,
                    event,
                    message -> {
                        message.getMessageProperties().setMessageId(event.getEventId());
                        message.getMessageProperties().setHeader("eventType", "NotificationEvent");
                        return message;
                    });
            log.debug("通知事件已投递到 RabbitMQ, eventId={}, receiverId={}, type={}",
                    event.getEventId(), event.getReceiverId(), event.getType());
        } catch (AmqpException e) {
            log.error("通知事件投递 RabbitMQ 失败, eventId={}, receiverId={}, type={}",
                    event.getEventId(), event.getReceiverId(), event.getType(), e);
        }
    }
}

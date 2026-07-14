package com.xiaoyang.d_game.messaging.notification;

import com.xiaoyang.d_game.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 通知事件消费者。
 *
 * <p>监听容器使用 {@code notificationRabbitListenerContainerFactory}，
 * 其中配置了 Java 线程池、并发消费者数量和失败不重新入队策略。</p>
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationEventHandler notificationEventHandler;

    /**
     * 消费通知事件。
     *
     * <p>这里不吞掉异常：处理失败时让异常抛给 Rabbit 监听容器，
     * 容器会拒绝消息且不重新入队，最终由 RabbitMQ 路由到死信队列。</p>
     */
    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE,
            containerFactory = "notificationRabbitListenerContainerFactory")
    public void consume(NotificationEvent event) {
        log.debug("开始消费通知事件, eventId={}, receiverId={}, type={}",
                event.getEventId(), event.getReceiverId(), event.getType());
        notificationEventHandler.handle(event);
        log.debug("通知事件消费完成, eventId={}", event.getEventId());
    }
}

package com.xiaoyang.d_game.messaging.notification;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.config.RabbitMqConfig;
import com.xiaoyang.d_game.entity.*;
import com.xiaoyang.d_game.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/** 短事务认领事件后投递；直到通知落库才完成，确认丢失及消费者失败均可安全重试。 */
@Component @RequiredArgsConstructor @Slf4j
@Profile("!test")
public class NotificationOutboxDispatcher {
    private final NotificationOutboxMapper outbox;
    private final NotificationMapper notifications;
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;
    @Scheduled(fixedDelayString = "${app.notification.outbox-interval-ms:1000}", scheduler = "notificationOutboxScheduler")
    public void dispatch() {
        try {
            dispatchPending();
        } catch (RuntimeException exception) {
            log.error("通知发件箱扫描或状态更新失败，等待下一轮调度", exception);
            throw exception;
        }
    }

    private void dispatchPending() {
        for (var event : outbox.selectList(new LambdaQueryWrapper<NotificationOutbox>()
                .ne(NotificationOutbox::getDelivered, 2).le(NotificationOutbox::getNextAttemptAt, LocalDateTime.now())
                .orderByAsc(NotificationOutbox::getNextAttemptAt).last("LIMIT 100"))) {
            // 使用到期条件原子认领，给本轮预留三十秒窗口，防止同一事件被同时投递。
            if (outbox.update(null, new LambdaUpdateWrapper<NotificationOutbox>().eq(NotificationOutbox::getId, event.getId())
                    .ne(NotificationOutbox::getDelivered, 2).le(NotificationOutbox::getNextAttemptAt, LocalDateTime.now())
                    .set(NotificationOutbox::getNextAttemptAt, LocalDateTime.now().plusSeconds(30))
                    .set(NotificationOutbox::getAttempts, event.getAttempts() + 1)) != 1) { continue; }
            try {
                if (notifications.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getEventId, event.getEventId())) > 0) {
                    outbox.update(null, new LambdaUpdateWrapper<NotificationOutbox>().eq(NotificationOutbox::getId, event.getId())
                            .set(NotificationOutbox::getDelivered, 2));
                    log.debug("通知消费已完成，发件箱事件结束，eventId={}", event.getEventId());
                    continue;
                }
                NotificationEvent payload = json.readValue(event.getPayload(), NotificationEvent.class);
                CorrelationData correlation = new CorrelationData(event.getEventId());
                rabbit.convertAndSend(RabbitMqConfig.EVENT_EXCHANGE, RabbitMqConfig.NOTIFICATION_ROUTING_KEY, payload,
                        message -> { message.getMessageProperties().setMessageId(event.getEventId()); return message; }, correlation);
                if (!correlation.getFuture().get(5, TimeUnit.SECONDS).isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("通知投递未获确认");
                }
                outbox.update(null, new LambdaUpdateWrapper<NotificationOutbox>().eq(NotificationOutbox::getId, event.getId())
                        .set(NotificationOutbox::getDelivered, 1));
                // MQ 确认只代表进入队列，仍需下一轮确认消费者入库，才能置为最终完成。
                log.debug("通知投递已确认，等待消费入库，eventId={}，attempt={}", event.getEventId(), event.getAttempts() + 1);
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    log.warn("通知发件箱投递线程被中断，保留待重试事件，eventId={}", event.getEventId());
                    return;
                }
                int delay = Math.min(60, 1 << Math.min(event.getAttempts() + 1, 6));
                // 先保留原始投递异常，避免重试时间更新失败时丢失最初的 MQ 故障原因。
                log.warn("通知发件箱投递失败，将重试，eventId={}，attempt={}，retryDelaySeconds={}",
                        event.getEventId(), event.getAttempts() + 1, delay, exception);
                outbox.update(null, new LambdaUpdateWrapper<NotificationOutbox>().eq(NotificationOutbox::getId, event.getId())
                        .set(NotificationOutbox::getNextAttemptAt, LocalDateTime.now().plusSeconds(delay)));
            }
        }
    }
}

package com.xiaoyang.d_game.messaging.notification;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyang.d_game.entity.Notification;
import com.xiaoyang.d_game.mapper.NotificationMapper;
import com.xiaoyang.d_game.messaging.RealtimeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
/** 数据库唯一事件 ID 保证消费幂等，Redis 故障不会重复生成通知。 */
@Slf4j
@Component @RequiredArgsConstructor
public class NotificationEventHandler {
    private final NotificationMapper notifications;
    private final RealtimeGateway realtime;
    @Transactional(rollbackFor = Exception.class)
    public void handle(NotificationEvent event) {
        if (event.getReceiverId() == null || Objects.equals(event.getReceiverId(), event.getSenderId())) { return; }
        Notification notification = new Notification(); notification.setEventId(event.getEventId());
        notification.setReceiverId(event.getReceiverId()); notification.setSenderId(event.getSenderId());
        notification.setType(event.getType()); notification.setTitle(event.getTitle());
        notification.setContent(event.getContent() == null ? "" : event.getContent());
        notification.setTargetType(event.getTargetType()); notification.setTargetId(event.getTargetId());
        notification.setIsSilent(event.getIsSilent() == null ? 0 : event.getIsSilent());
        try { notifications.insert(notification); }
        catch (DuplicateKeyException exception) {
            // 只吞掉同一事件的重复消费；其他唯一约束冲突继续抛出，不能误报成功。
            if (notifications.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getEventId, event.getEventId())) > 0) {
                log.debug("通知事件已处理，忽略重复消费，eventId={}，receiverId={}", event.getEventId(), event.getReceiverId());
                return;
            }
            throw exception;
        }
        realtime.changed(event.getReceiverId(), "NOTIFICATION", null, event.getSenderId());
        RealtimeGateway.afterCommit(() -> log.debug("通知入库成功，eventId={}，receiverId={}，type={}，静默={}",
                event.getEventId(), event.getReceiverId(), event.getType(), notification.getIsSilent()));
    }
}

package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.common.*;
import com.xiaoyang.d_game.common.enums.TargetTypeEnum;
import com.xiaoyang.d_game.dto.*;
import com.xiaoyang.d_game.entity.*;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.messaging.RealtimeGateway;
import com.xiaoyang.d_game.messaging.notification.NotificationEvent;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** 通知生产、偏好和已读状态服务；事件与业务共同提交，未读数量以数据库为准。 */
@Slf4j
@Service @RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    private final NotificationSettingMapper preferences;
    private final NotificationOutboxMapper outbox;
    private final ObjectMapper json;
    private final UserMapper users;
    private final PostMapper posts;
    private final CommentMapper comments;
    private final RealtimeGateway realtime;
    @Override @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long receiverId, Long senderId, Integer type, String title, String content, Integer targetType, Long targetId) {
        if (receiverId == null || Objects.equals(receiverId, senderId)) { return; }
        NotificationEvent event = NotificationEvent.of(receiverId, senderId, type, title, content, targetType, targetId);
        var setting = preference(receiverId);
        boolean enabled = switch (type) {
            case 1 -> setting.likes(); case 2 -> setting.replies(); case 6 -> setting.mentions();
            case 3, 7 -> setting.social(); default -> true;
        };
        event.setIsSilent(enabled ? 0 : 1);
        // 静默状态在事件生成时冻结：恢复开关不补发关闭期间的提醒，MQ 消费时不再读取设置。
        NotificationOutbox saved = new NotificationOutbox(); saved.setEventId(event.getEventId()); saved.setNextAttemptAt(LocalDateTime.now());
        try { saved.setPayload(json.writeValueAsString(event)); }
        catch (Exception exception) { throw new IllegalStateException("无法保存通知事件", exception); }
        outbox.insert(saved);
        RealtimeGateway.afterCommit(() -> log.debug("通知事件已保存至发件箱，eventId={}，receiverId={}，type={}，静默={}",
                event.getEventId(), receiverId, type, event.getIsSilent()));
    }
    private MessageDtos.Settings preference(Long userId) {
        var setting = preferences.selectOne(new LambdaQueryWrapper<NotificationSetting>().eq(NotificationSetting::getUserId, userId));
        return setting == null ? new MessageDtos.Settings(true, true, true, true)
                : new MessageDtos.Settings(setting.getReplies() == 1, setting.getMentions() == 1, setting.getLikes() == 1, setting.getSocial() == 1);
    }
    @Override public MessageDtos.Settings settings() { return preference(ChatServiceImpl.currentUser()); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void saveSettings(MessageDtos.Settings request) {
        Long me = ChatServiceImpl.currentUser();
        users.selectOne(new LambdaQueryWrapper<User>().select(User::getId).eq(User::getId, me).last("FOR UPDATE"));
        var setting = preferences.selectOne(new LambdaQueryWrapper<NotificationSetting>().eq(NotificationSetting::getUserId, me).last("FOR UPDATE"));
        boolean create = setting == null;
        if (create) { setting = new NotificationSetting(); setting.setUserId(me); }
        setting.setReplies(request.replies() ? 1 : 0); setting.setMentions(request.mentions() ? 1 : 0);
        setting.setLikes(request.likes() ? 1 : 0); setting.setSocial(request.social() ? 1 : 0);
        if (create) { preferences.insert(setting); } else { preferences.updateById(setting); }
        realtime.changed(me, "SETTINGS", null, null);
        RealtimeGateway.afterCommit(() -> log.info("通知偏好保存成功，userId={}，回复={}，提及={}，点赞={}，关注好友={}",
                me, request.replies(), request.mentions(), request.likes(), request.social()));
    }
    private List<Integer> category(String name) {
        if (name == null || name.isBlank()) { return List.of(1, 2, 3, 4, 5, 6, 7); }
        return switch (name) {
            case "reply" -> List.of(2); case "mention" -> List.of(6); case "like" -> List.of(1);
            case "system" -> List.of(4, 5); case "social" -> List.of(3, 7);
            default -> throw new BizException(ResultCode.BAD_REQUEST, "未知通知分类");
        };
    }
    @Override public PageResult<NotificationResp> pageNotifications(Long page, Long size) { return pageNotifications(page, size, null); }
    @Override public PageResult<NotificationResp> pageNotifications(Long page, Long size, String category) {
        Page<Notification> result = page(new Page<>(Math.max(1, page), Math.min(50, Math.max(1, size))),
                new LambdaQueryWrapper<Notification>().eq(Notification::getReceiverId, ChatServiceImpl.currentUser())
                        .in(Notification::getType, category(category)).orderByDesc(Notification::getGmtCreate).orderByDesc(Notification::getId));
        PageResult<NotificationResp> response = new PageResult<>(); response.setPage(result.getCurrent()); response.setSize(result.getSize()); response.setTotal(result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::project).toList()); return response;
    }
    private NotificationResp project(Notification notification) {
        // 目标可见性在查询时重新校验；目标删除或审核不通过时保留通知，但不提供跳转链接。
        User sender = notification.getSenderId() == null ? null : users.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getNickname, User::getAvatarUrl).eq(User::getId, notification.getSenderId()));
        String url = null;
        if (notification.getTargetId() != null) {
            if (Objects.equals(notification.getTargetType(), TargetTypeEnum.POST.getCode())) {
                Post post = posts.selectById(notification.getTargetId());
                if (post != null && post.getStatus() == 2) { url = "/posts/" + post.getId(); }
                else if (post != null && post.getUserId().equals(ChatServiceImpl.currentUser()) && notification.getType() == 5) {
                    url = "/posts/new?postId=" + post.getId();
                }
            } else if (Objects.equals(notification.getTargetType(), TargetTypeEnum.COMMENT.getCode())) {
                Comment comment = comments.selectById(notification.getTargetId());
                Post post = comment == null ? null : posts.selectById(comment.getPostId());
                if (comment != null && comment.getStatus() == 2 && post != null && post.getStatus() == 2) {
                    url = "/posts/" + post.getId() + "?commentId=" + comment.getId();
                }
            }
        } else if ((notification.getType() == 3 || notification.getType() == 7) && sender != null) { url = "/users/" + sender.getId(); }
        return new NotificationResp(notification.getId(), notification.getReceiverId(), notification.getSenderId(), notification.getType(),
                notification.getTitle(), notification.getContent(), notification.getTargetType(), notification.getTargetId(), notification.getIsRead(), notification.getIsSilent(), notification.getGmtCreate(),
                sender == null ? "系统" : sender.getNickname(), sender == null ? null : sender.getAvatarUrl(), url,
                url != null || notification.getTargetId() == null);
    }
    @Override public long unreadCount() { return categoryUnread(null); }
    @Override public long categoryUnread(String category) {
        // 静默通知仍保存，但不计未读；不维护容易被并发已读操作扣错的 Redis 增减计数。
        return count(new LambdaQueryWrapper<Notification>().eq(Notification::getReceiverId, ChatServiceImpl.currentUser())
                .eq(Notification::getIsRead, 0).eq(Notification::getIsSilent, 0).in(Notification::getType, category(category)));
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        Long me = ChatServiceImpl.currentUser();
        if (baseMapper.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getId, id).eq(Notification::getReceiverId, me)) == 0) { throw new BizException(ResultCode.NOT_FOUND); }
        int changed = baseMapper.update(null, new LambdaUpdateWrapper<Notification>().eq(Notification::getId, id).eq(Notification::getReceiverId, me)
                .eq(Notification::getIsRead, 0).set(Notification::getIsRead, 1));
        if (changed > 0) {
            realtime.changed(me, "UNREAD", null, null);
            RealtimeGateway.afterCommit(() -> log.debug("通知标记已读成功，userId={}，notificationId={}", me, id));
        }
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void markAllRead() { markAllRead(null); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void markAllRead(String category) {
        Long me = ChatServiceImpl.currentUser();
        // 只更新当前用户仍未读的记录，重复调用不会产生额外计数变化。
        int changed = baseMapper.update(null, new LambdaUpdateWrapper<Notification>().eq(Notification::getReceiverId, me).eq(Notification::getIsRead, 0)
                .in(Notification::getType, category(category)).set(Notification::getIsRead, 1));
        realtime.changed(me, "UNREAD", null, null);
        RealtimeGateway.afterCommit(() -> log.debug("分类通知已标记已读，userId={}，category={}，changedCount={}",
                me, category == null ? "全部" : category, changed));
    }
}

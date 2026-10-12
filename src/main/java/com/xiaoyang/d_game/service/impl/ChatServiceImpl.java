package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.*;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.entity.*;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.messaging.RealtimeGateway;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** 好友私信服务：以数据库成员状态为准，只允许参与者访问，管理员无额外私信权限。 */
@Slf4j
@Service @RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatConversationMapper conversations;
    private final ChatMemberMapper members;
    private final ChatMessageMapper messages;
    private final UserMapper users;
    private final UserFollowMapper follows;
    private final RealtimeGateway realtime;
    private static final int MAX_PAGE_SIZE = 50;
    public static Long currentUser() {
        Long id = CurrentUser.getUserId();
        if (id == null) { throw new BizException(ResultCode.UNAUTHORIZED); }
        return id;
    }
    @Override public PageResult<MessageDtos.UserBrief> friends(long page, long size, String q) {
        return PageResult.of(conversations.friends(new Page<>(Math.max(1, page), boundedSize(size)), currentUser(), q));
    }
    private long boundedSize(long size) { return Math.min(MAX_PAGE_SIZE, Math.max(1, size)); }
    private void lockUsers(Long first, Long second) {
        // 与关注、取消关注共用按 ID 升序的用户锁，再锁会话，防止死锁和解除好友期间越权发送。
        if (Objects.equals(first, second)) { throw new BizException(ResultCode.BAD_REQUEST); }
        for (Long id : List.of(Math.min(first, second), Math.max(first, second))) {
            User user = users.selectOne(new LambdaQueryWrapper<User>().select(User::getId, User::getStatus)
                    .eq(User::getId, id).last("FOR UPDATE"));
            if (user == null) { throw new BizException(ResultCode.USER_NOT_FOUND); }
            if (!Objects.equals(user.getStatus(), 1)) { throw new BizException(ResultCode.USER_BANNED); }
        }
    }
    private boolean isFriend(Long first, Long second, boolean lock) {
        // 好友由两条有效关注记录计算，不维护第二份容易失去同步的好友关系。
        var query = new LambdaQueryWrapper<UserFollow>().select(UserFollow::getId)
                .in(UserFollow::getFollowerId, List.of(first, second))
                .in(UserFollow::getFolloweeId, List.of(first, second));
        if (lock) { query.last("FOR UPDATE"); }
        return follows.selectList(query).size() == 2;
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public MessageDtos.Conversation open(Long friendId) {
        Long me = currentUser();
        lockUsers(me, friendId);
        if (!isFriend(me, friendId, true)) { throw new BizException(ResultCode.FORBIDDEN, "只有好友可以发送消息"); }
        ChatConversation conversation = conversations.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUserLow, Math.min(me, friendId))
                .eq(ChatConversation::getUserHigh, Math.max(me, friendId)).last("FOR UPDATE"));
        boolean created = conversation == null;
        if (created) {
            conversation = new ChatConversation();
            conversation.setUserLow(Math.min(me, friendId));
            conversation.setUserHigh(Math.max(me, friendId));
            conversations.insert(conversation);
            for (Long user : List.of(me, friendId)) {
                var member = new ChatMember(); member.setConversationId(conversation.getId()); member.setUserId(user);
                // 未发送消息时仅发起者的列表出现会话。
                member.setHidden(user.equals(me) ? 0 : 1); members.insert(member);
            }
        } else {
            members.update(null, new LambdaUpdateWrapper<ChatMember>().eq(ChatMember::getConversationId, conversation.getId())
                    .eq(ChatMember::getUserId, me).set(ChatMember::getHidden, 0));
        }
        Long conversationId = conversation.getId();
        RealtimeGateway.afterCommit(() -> log.debug("会话打开成功，userId={}，friendId={}，conversationId={}，新建={}",
                me, friendId, conversationId, created));
        return project(conversation, member(conversationId, me));
    }
    @Override public PageResult<MessageDtos.Conversation> conversations(long page, long size) {
        Long me = currentUser();
        Page<ChatMember> result = members.selectPage(new Page<>(Math.max(1, page), boundedSize(size)),
                new LambdaQueryWrapper<ChatMember>().eq(ChatMember::getUserId, me).eq(ChatMember::getHidden, 0)
                        .orderByDesc(ChatMember::getGmtModified).orderByDesc(ChatMember::getId));
        PageResult<MessageDtos.Conversation> response = new PageResult<>();
        response.setPage(result.getCurrent()); response.setSize(result.getSize()); response.setTotal(result.getTotal());
        response.setRecords(result.getRecords().stream().map(m -> project(conversations.selectById(m.getConversationId()), m)).toList());
        return response;
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public MessageDtos.Conversation detail(Long id) {
        Long me = currentUser();
        return project(lockedConversation(id, me), member(id, me));
    }
    private MessageDtos.Conversation project(ChatConversation conversation, ChatMember member) {
        Long friend = conversation.getUserLow().equals(member.getUserId()) ? conversation.getUserHigh() : conversation.getUserLow();
        User user = users.selectOne(new LambdaQueryWrapper<User>().select(User::getId, User::getNickname, User::getAvatarUrl, User::getStatus)
                .eq(User::getId, friend));
        ChatMessage last = messages.selectOne(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, conversation.getId())
                .gt(ChatMessage::getSeq, member.getClearSeq()).orderByDesc(ChatMessage::getSeq).last("LIMIT 1"));
        long unread = messages.selectCount(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, conversation.getId())
                .ne(ChatMessage::getSenderId, member.getUserId()).eq(ChatMessage::getRecalled, 0)
                .gt(ChatMessage::getSeq, Math.max(member.getReadSeq(), member.getClearSeq())));
        return new MessageDtos.Conversation(conversation.getId(), friend, user == null ? "已注销用户" : user.getNickname(),
                user == null ? null : user.getAvatarUrl(), last == null ? "" : last.getRecalled() == 1 ? "消息已撤回" : last.getContent(),
                last == null ? conversation.getGmtCreate() : last.getGmtCreate(), unread,
                user != null && user.getStatus() == 1 && isFriend(member.getUserId(), friend, false));
    }
    private ChatMember member(Long conversationId, Long me) {
        ChatMember member = members.selectOne(new LambdaQueryWrapper<ChatMember>()
                .eq(ChatMember::getConversationId, conversationId).eq(ChatMember::getUserId, me));
        if (member == null) { throw new BizException(ResultCode.NOT_FOUND); }
        return member;
    }
    private ChatConversation lockedConversation(Long id, Long me) {
        // 所有私信入口统一验证参与者；对其他用户返回不存在，避免泄露会话是否存在。
        ChatConversation conversation = conversations.selectOne(new LambdaQueryWrapper<ChatConversation>().eq(ChatConversation::getId, id).last("FOR UPDATE"));
        if (conversation == null || (!me.equals(conversation.getUserLow()) && !me.equals(conversation.getUserHigh()))) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return conversation;
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public MessageDtos.History history(Long id, Long before, Long after, int size) {
        // clearSeq 只属于当前成员。游标查询永远排除本人已清除的历史，不影响对方。
        Long me = currentUser(); lockedConversation(id, me);
        if ((before != null && before <= 0) || (after != null && after < 0) || (before != null && after != null)) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        ChatMember member = member(id, me);
        int limit = (int) boundedSize(size);
        var query = new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, id).gt(ChatMessage::getSeq, member.getClearSeq());
        if (before != null) { query.lt(ChatMessage::getSeq, before); }
        if (after != null) { query.gt(ChatMessage::getSeq, after).orderByAsc(ChatMessage::getSeq); }
        else { query.orderByDesc(ChatMessage::getSeq); }
        List<ChatMessage> found = messages.selectList(query.last("LIMIT " + (limit + 1)));
        boolean more = found.size() > limit;
        var visible = new ArrayList<>(found.subList(0, Math.min(limit, found.size())));
        if (after == null) { Collections.reverse(visible); }
        return new MessageDtos.History(visible.stream().map(this::message).toList(), more);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public MessageDtos.Message send(Long id, MessageDtos.Send request) {
        Long me = currentUser();
        ChatConversation located = conversations.selectById(id);
        if (located == null || (!me.equals(located.getUserLow()) && !me.equals(located.getUserHigh()))) { throw new BizException(ResultCode.NOT_FOUND); }
        Long other = me.equals(located.getUserLow()) ? located.getUserHigh() : located.getUserLow();
        lockUsers(me, other);
        ChatConversation conversation = lockedConversation(id, me);
        ChatMessage existing = messages.selectOne(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, id)
                .eq(ChatMessage::getSenderId, me).eq(ChatMessage::getClientMessageId, request.clientMessageId()).last("FOR UPDATE"));
        // 同一客户端重试键直接返回持久化结果；撤回后的重试也不会恢复原文。
        if (existing != null) {
            log.debug("私信发送重试命中已有消息，userId={}，conversationId={}，messageId={}", me, id, existing.getId());
            return message(existing);
        }
        if (!isFriend(me, other, true)) { throw new BizException(ResultCode.FORBIDDEN, "好友关系已解除，无法发送新消息"); }
        if (request.content() == null || request.content().isBlank() || request.content().length() > 2000
                || request.clientMessageId() == null || !request.clientMessageId().matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")) { throw new BizException(ResultCode.BAD_REQUEST); }
        ChatMessage saved = new ChatMessage(); saved.setConversationId(id); saved.setSenderId(me);
        saved.setSeq(conversation.getLastSeq() + 1); saved.setContent(request.content()); saved.setClientMessageId(request.clientMessageId());
        saved.setGmtCreate(LocalDateTime.now()); messages.insert(saved);
        conversation.setLastSeq(saved.getSeq()); conversations.updateById(conversation);
        members.update(null, new LambdaUpdateWrapper<ChatMember>().eq(ChatMember::getConversationId, id)
                .set(ChatMember::getHidden, 0).set(ChatMember::getGmtModified, LocalDateTime.now()));
        // 提交成功后才记录发送结果；只记录标识和序号，不记录私信正文或请求对象。
        RealtimeGateway.afterCommit(() -> log.info("私信发送成功，senderId={}，receiverId={}，conversationId={}，messageId={}，seq={}",
                me, other, id, saved.getId(), saved.getSeq()));
        realtime.changed(me, "MESSAGE", id, me); realtime.changed(other, "MESSAGE", id, me);
        return message(saved);
    }
    private MessageDtos.Message message(ChatMessage message) {
        // 撤回状态优先于正文；历史、补取和发送重试共用此投影，防止原文再次暴露。
        return new MessageDtos.Message(message.getId(), message.getConversationId(), message.getSenderId(), message.getSeq(),
                message.getRecalled() == 1 ? "" : message.getContent(), message.getRecalled() == 1, message.getGmtCreate(), message.getGmtCreate().plusMinutes(2));
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void recall(Long id) {
        // 撤回只校验本人身份与服务端发送时间，解除好友后仍允许在两分钟内撤回。
        Long me = currentUser(); ChatMessage located = messages.selectById(id);
        if (located == null || !me.equals(located.getSenderId())) { throw new BizException(ResultCode.NOT_FOUND); }
        ChatConversation conversation = lockedConversation(located.getConversationId(), me);
        ChatMessage latest = messages.selectOne(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getId, id).last("FOR UPDATE"));
        if (latest.getRecalled() == 1) {
            log.debug("消息已撤回，忽略重复操作，userId={}，messageId={}", me, id);
            return;
        }
        if (!LocalDateTime.now().isBefore(latest.getGmtCreate().plusMinutes(2))) { throw new BizException(ResultCode.FORBIDDEN, "消息已超过两分钟撤回时限"); }
        messages.update(null, new LambdaUpdateWrapper<ChatMessage>().eq(ChatMessage::getId, id).eq(ChatMessage::getRecalled, 0)
                .set(ChatMessage::getRecalled, 1).set(ChatMessage::getContent, "").set(ChatMessage::getRecalledAt, LocalDateTime.now()));
        RealtimeGateway.afterCommit(() -> log.info("私信撤回成功，userId={}，conversationId={}，messageId={}", me, conversation.getId(), id));
        realtime.changed(conversation.getUserLow(), "RECALL", conversation.getId(), me);
        realtime.changed(conversation.getUserHigh(), "RECALL", conversation.getId(), me);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void clear(Long id) {
        // 与发送共用会话锁：锁内 lastSeq 是本次清除边界，之后的新消息会重新显示。
        Long me = currentUser(); ChatConversation conversation = lockedConversation(id, me);
        members.update(null, new LambdaUpdateWrapper<ChatMember>().eq(ChatMember::getConversationId, id).eq(ChatMember::getUserId, me)
                .set(ChatMember::getClearSeq, conversation.getLastSeq()).set(ChatMember::getReadSeq, conversation.getLastSeq()).set(ChatMember::getHidden, 1));
        RealtimeGateway.afterCommit(() -> log.info("本人会话历史清除成功，userId={}，conversationId={}，clearSeq={}", me, id, conversation.getLastSeq()));
        realtime.changed(me, "CLEAR", id, null);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void read(Long id, Long seq) {
        Long me = currentUser(); ChatConversation conversation = lockedConversation(id, me);
        if (seq == null || seq < 0 || seq > conversation.getLastSeq()) { throw new BizException(ResultCode.BAD_REQUEST); }
        ChatMember member = member(id, me);
        if (seq > member.getReadSeq()) {
            // 只推进本人实际展示位置，不返回或推送对方的已读回执。
            members.update(null, new LambdaUpdateWrapper<ChatMember>().eq(ChatMember::getId, member.getId()).set(ChatMember::getReadSeq, seq));
            realtime.changed(me, "UNREAD", id, null);
            RealtimeGateway.afterCommit(() -> log.debug("本人聊天阅读位置已推进，userId={}，conversationId={}，readSeq={}", me, id, seq));
        }
    }
    @Override public long unread() { return conversations.unread(currentUser()); }
}

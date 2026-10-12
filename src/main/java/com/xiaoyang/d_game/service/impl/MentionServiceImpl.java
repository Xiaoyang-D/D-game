package com.xiaoyang.d_game.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.common.*;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.entity.*;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xiaoyang.d_game.messaging.RealtimeGateway;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** 按用户 ID 保存提及，重建可信显示标签；编辑只通知此前未收到此内容通知的用户。 */
@Slf4j
@Service @RequiredArgsConstructor
public class MentionServiceImpl implements MentionService {
    private final UserMapper users;
    private final ContentMentionMapper mentions;
    private final CommentMapper comments;
    private final PostMapper posts;
    private final NotificationService notifications;
    private final HtmlSanitizer sanitizer;
    private final ObjectMapper json;
    @Override public List<MessageDtos.UserBrief> candidates(String q) {
        // 搜索所有正常用户，仅投影 ID、昵称和头像，禁止返回邮箱或手机号。
        ChatServiceImpl.currentUser();
        if (q == null || q.length() > 50) { throw new BizException(ResultCode.BAD_REQUEST); }
        if (q.isBlank()) { return List.of(); }
        return users.selectList(new LambdaQueryWrapper<User>().select(User::getId, User::getNickname, User::getAvatarUrl)
                .eq(User::getStatus, 1).like(User::getNickname, q.trim()).orderByAsc(User::getNickname).orderByAsc(User::getId).last("LIMIT 20"))
                .stream().map(user -> new MessageDtos.UserBrief(user.getId(), user.getNickname(), user.getAvatarUrl())).toList();
    }
    private User user(Long id) {
        User user = users.selectOne(new LambdaQueryWrapper<User>().select(User::getId, User::getNickname)
                .eq(User::getId, id).eq(User::getStatus, 1));
        if (user == null) { throw new BizException(ResultCode.BAD_REQUEST, "被提及用户不存在或不可用"); }
        return user;
    }
    @Override public String normalizePost(String html) {
        // 忽略客户端提供的昵称和样式，按已选择用户 ID 重建经过清理的提及标签。
        var document = Jsoup.parseBodyFragment(sanitizer.sanitizePostContent(html));
        Set<Long> selected = new HashSet<>();
        for (var element : document.select("span[data-type=mention]")) {
            Long id;
            try { id = Long.valueOf(element.attr("data-id")); } catch (Exception exception) { throw new BizException(ResultCode.BAD_REQUEST, "无效的用户提及"); }
            var user = user(id); selected.add(id);
            element.attr("data-id", id.toString()).attr("data-label", user.getNickname()).attr("class", "user-mention").text("@" + user.getNickname());
        }
        if (selected.size() > 10) { throw new BizException(ResultCode.BAD_REQUEST, "每条内容最多提及10位用户"); }
        return document.body().html();
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void syncPost(Post post) {
        Map<Long, List<MessageDtos.Mention>> selected = new LinkedHashMap<>();
        for (var element : Jsoup.parseBodyFragment(post.getContent()).select("span[data-type=mention]")) {
            try { selected.put(Long.valueOf(element.attr("data-id")), List.of()); }
            catch (NumberFormatException exception) { throw new BizException(ResultCode.BAD_REQUEST); }
        }
        synchronize(1, post.getId(), post.getUserId(), selected, Objects.equals(post.getStatus(), 2));
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void syncComment(Long id, Long authorId, String content, List<MessageDtos.Mention> selected) {
        // 浏览器与 Java String 都使用 UTF-16 下标；校验范围、顺序、重叠和当前昵称。
        Map<Long, List<MessageDtos.Mention>> grouped = new LinkedHashMap<>();
        int previousEnd = 0;
        for (var mention : selected == null ? List.<MessageDtos.Mention>of() : selected.stream().sorted(Comparator.comparingInt(MessageDtos.Mention::start)).toList()) {
            if (mention.userId() == null || mention.start() < previousEnd || mention.end() <= mention.start() || mention.end() > content.length()
                    || content.charAt(mention.start()) != '@') { throw new BizException(ResultCode.BAD_REQUEST, "提及位置与正文不一致"); }
            User user = user(mention.userId());
            if (!content.substring(mention.start(), mention.end()).equals("@" + user.getNickname())) { throw new BizException(ResultCode.BAD_REQUEST, "用户昵称已变化，请重新选择提及用户"); }
            previousEnd = mention.end();
            grouped.computeIfAbsent(user.getId(), ignored -> new ArrayList<>()).add(mention);
        }
        if (grouped.size() > 10) { throw new BizException(ResultCode.BAD_REQUEST, "每条内容最多提及10位用户"); }
        synchronize(2, id, authorId, grouped, true);
    }
    private void synchronize(int type, Long sourceId, Long authorId, Map<Long, List<MessageDtos.Mention>> selected, boolean published) {
        // 删除显示范围时保留 notifiedAt，移除后再次提及同一用户不会重复打扰。
        List<ContentMention> previous = mentions.selectList(new LambdaQueryWrapper<ContentMention>().eq(ContentMention::getSourceType, type)
                .eq(ContentMention::getSourceId, sourceId).last("FOR UPDATE"));
        for (var old : previous) {
            if (!selected.containsKey(old.getUserId())) { old.setRangesJson("[]"); mentions.updateById(old); }
        }
        int notificationsQueued = 0;
        for (var entry : selected.entrySet()) {
            ContentMention mention = previous.stream().filter(old -> old.getUserId().equals(entry.getKey())).findFirst().orElse(null);
            boolean create = mention == null;
            if (create) { mention = new ContentMention(); mention.setSourceType(type); mention.setSourceId(sourceId); mention.setUserId(entry.getKey()); }
            try { mention.setRangesJson(json.writeValueAsString(entry.getValue())); }
            catch (Exception exception) { throw new IllegalStateException("无法保存提及关系", exception); }
            if (published && mention.getNotifiedAt() == null) {
                // 草稿及尚未公开的定时帖子只保存关系，实际公开后才写入通知发件箱。
                notifications.sendNotification(entry.getKey(), authorId, 6, "有人@了你", "点击查看提及你的内容", type, sourceId);
                mention.setNotifiedAt(LocalDateTime.now());
                if (!entry.getKey().equals(authorId)) { notificationsQueued++; }
            }
            if (create) { mentions.insert(mention); } else { mentions.updateById(mention); }
        }
        int queuedCount = notificationsQueued;
        RealtimeGateway.afterCommit(() -> log.debug("内容提及同步完成，sourceType={}，sourceId={}，authorId={}，userCount={}，公开={}，notificationCount={}",
                type, sourceId, authorId, selected.size(), published, queuedCount));
    }
    @Override public List<MessageDtos.Mention> commentMentions(Long id) {
        List<MessageDtos.Mention> result = new ArrayList<>();
        for (var mention : mentions.selectList(new LambdaQueryWrapper<ContentMention>().eq(ContentMention::getSourceType, 2).eq(ContentMention::getSourceId, id))) {
            try { result.addAll(json.readValue(mention.getRangesJson(), new TypeReference<List<MessageDtos.Mention>>() { })); }
            catch (Exception exception) { throw new IllegalStateException("无法读取提及关系", exception); }
        }
        result.sort(Comparator.comparingInt(MessageDtos.Mention::start)); return result;
    }
    @Override public MessageDtos.Location location(Long id, String sort, int size) {
        ChatServiceImpl.currentUser();
        Comment comment = comments.selectById(id); Post post = comment == null ? null : posts.selectById(comment.getPostId());
        if (comment == null || comment.getStatus() != 2 || post == null || post.getStatus() != 2) { throw new BizException(ResultCode.NOT_FOUND, "内容已删除或暂不可见"); }
        // 与评论分页使用相同的排序和主键兜底，直接计数定位，不读取整个帖子评论。
        return new MessageDtos.Location(post.getId(), id, comments.countBefore(comment, sort) / Math.min(50, Math.max(1, size)) + 1);
    }
}

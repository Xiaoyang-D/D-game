package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xiaoyang.d_game.common.LongNumberSerializer;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/** 消息中心的请求与投影，不暴露数据库实体及对方阅读位置。 */
public final class MessageDtos {
    private MessageDtos() { }
    /** 仅包含可公开的展示信息，不包含联系方式。 */
    public record UserBrief(@JsonSerialize(using = ToStringSerializer.class) Long id,
                            String nickname, String avatarUrl) { }
    /** 未读数量为数字，实体 ID 为字符串，不暴露对方阅读位置。 */
    public record Conversation(@JsonSerialize(using = ToStringSerializer.class) Long id,
                               @JsonSerialize(using = ToStringSerializer.class) Long friendId,
                               String nickname, String avatarUrl, String lastContent,
                               LocalDateTime lastTime, @JsonSerialize(using = LongNumberSerializer.class) long unreadCount, boolean isFriend) { }
    /** 序号用字符串避免 JS 精度丢失，撤回后的 content 必须为空。 */
    public record Message(@JsonSerialize(using = ToStringSerializer.class) Long id,
                          @JsonSerialize(using = ToStringSerializer.class) Long conversationId,
                          @JsonSerialize(using = ToStringSerializer.class) Long senderId,
                          @JsonSerialize(using = ToStringSerializer.class) Long seq,
                          String content, boolean recalled, LocalDateTime gmtCreate,
                          LocalDateTime recallDeadline) { }
    public record History(List<Message> records, boolean hasMore) { }
    public record Open(@NotNull @Positive Long friendId) { }
    /** 长度按 UTF-16 计数，与浏览器一致；客户端 UUID 用于失败重试去重。 */
    public record Send(@NotBlank @Size(max = 2000) String content,
                       @NotBlank @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String clientMessageId) { }
    public record Read(@NotNull @PositiveOrZero Long lastVisibleSeq) { }
    public record Settings(boolean replies, boolean mentions, boolean likes, boolean social) { }
    /** 分类计数均为 JSON 数字，不受全局 Long 转字符串影响。 */
    public record Unread(@JsonSerialize(using = LongNumberSerializer.class) long total,
                         @JsonSerialize(using = LongNumberSerializer.class) long privateMessages,
                         @JsonSerialize(using = LongNumberSerializer.class) long replies,
                         @JsonSerialize(using = LongNumberSerializer.class) long mentions,
                         @JsonSerialize(using = LongNumberSerializer.class) long likes,
                         @JsonSerialize(using = LongNumberSerializer.class) long system,
                         @JsonSerialize(using = LongNumberSerializer.class) long social) { }
    public record Mention(@JsonSerialize(using = ToStringSerializer.class) @NotNull @Positive Long userId, @Min(0) int start, @Positive int end) { }
    public record Location(@JsonSerialize(using = ToStringSerializer.class) Long postId,
                           @JsonSerialize(using = ToStringSerializer.class) Long commentId,
                           @JsonSerialize(using = LongNumberSerializer.class) long page) { }
}

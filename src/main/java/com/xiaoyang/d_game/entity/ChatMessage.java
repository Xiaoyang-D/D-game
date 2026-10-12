package com.xiaoyang.d_game.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/** 双方共享私信，日志仅记录标识，禁止直接记录包含正文的实体。 */
@Data @EqualsAndHashCode(callSuper = true) @TableName("chat_message")
public class ChatMessage extends BaseEntity {
    private Long conversationId;
    private Long senderId;
    private Long seq;
    /** 与会话和发送者组成唯一约束，保证发送重试幂等。 */
    private String clientMessageId;
    private String content;
    /** 撤回时同时清空 content，不能只隐藏显示或保留可再次获取的原文。 */
    private Integer recalled = 0;
    private LocalDateTime recalledAt;
}

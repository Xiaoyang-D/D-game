package com.xiaoyang.d_game.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** 双方共享会话，用户 ID 规范化后唯一；单方清除历史不能删除共享记录。 */
@Data @EqualsAndHashCode(callSuper = true) @TableName("chat_conversation")
public class ChatConversation extends BaseEntity {
    private Long userLow;
    private Long userHigh;
    /** 最新消息序号，仅在持有会话锁的事务中推进。 */
    private Long lastSeq = 0L;
}

package com.xiaoyang.d_game.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** 成员本人的展示状态，阅读、清除和隐藏不修改另一成员的数据。 */
@Data @EqualsAndHashCode(callSuper = true) @TableName("chat_member")
public class ChatMember extends BaseEntity {
    private Long conversationId;
    private Long userId;
    /** 本人最后实际展示的序号，单调递增，不作为对方已读回执。 */
    private Long readSeq = 0L;
    /** 本人清除边界，此序号及之前的历史不再返回给本人。 */
    private Long clearSeq = 0L;
    /** 1 隐藏本人入口，新消息重新显示入口但不恢复历史。 */
    private Integer hidden = 0;
}

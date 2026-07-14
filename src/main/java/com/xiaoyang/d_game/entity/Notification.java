package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
/**
 * 站内通知实体。
 *
 * <p>用于记录点赞、评论、关注、系统消息和审核结果等用户可见通知。</p>
 */
public class Notification extends BaseEntity {

    /** 通知接收者用户 ID。 */
    private Long receiverId;

    /** 触发通知的用户 ID；系统通知可为空。 */
    private Long senderId;

    /** 通知类型，取值见 NotificationTypeEnum。 */
    private Integer type;

    /** 通知标题。 */
    private String title;

    /** 通知正文。 */
    private String content = "";

    /** 关联目标类型，例如帖子或评论，可为空。 */
    private Integer targetType;

    /** 关联目标 ID，可为空。 */
    private Long targetId;

    /** 是否已读：0 未读，1 已读。 */
    private Integer isRead = 0;
}

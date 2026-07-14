package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_report")
/**
 * 内容举报实体。
 *
 * <p>用户对帖子、评论、游戏等目标提交举报后会写入该表，后台管理员处理后更新状态和处理说明。</p>
 */
public class ContentReport extends BaseEntity {

    /** 举报人用户 ID。 */
    private Long reporterId;

    /** 被举报目标类型，取值见 TargetTypeEnum。 */
    private Integer targetType;

    /** 被举报目标 ID。 */
    private Long targetId;

    /** 举报原因，来自用户提交。 */
    private String reason;

    /** 举报处理状态：0 待处理，1 已处理，2 已驳回。 */
    private Integer status;

    /** 管理员处理备注。 */
    private String handleNote;

    /** 处理人管理员用户 ID。 */
    private Long handlerId;
}

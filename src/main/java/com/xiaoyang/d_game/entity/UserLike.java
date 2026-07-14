package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_like")
/**
 * 用户点赞实体。
 */
public class UserLike extends BaseEntity {

    /** 点赞用户 ID。 */
    private Long userId;

    /** 点赞目标类型，例如帖子、评论或游戏。 */
    private Integer targetType;

    /** 点赞目标 ID。 */
    private Long targetId;
}

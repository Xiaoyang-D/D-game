package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_follow")
/**
 * 用户关注实体。
 */
public class UserFollow extends BaseEntity {

    /** 关注者用户 ID。 */
    private Long followerId;

    /** 被关注者用户 ID。 */
    private Long followeeId;
}

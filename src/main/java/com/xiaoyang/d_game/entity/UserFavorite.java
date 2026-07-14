package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_favorite")
/**
 * 用户收藏实体。
 */
public class UserFavorite extends BaseEntity {

    /** 收藏人用户 ID。 */
    private Long userId;

    /** 收藏目标类型，例如帖子或游戏。 */
    private Integer targetType;

    /** 收藏目标 ID。 */
    private Long targetId;
}

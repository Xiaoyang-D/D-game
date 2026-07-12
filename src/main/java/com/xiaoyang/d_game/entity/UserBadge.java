package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_badge")
public class UserBadge extends BaseEntity {

    private Long userId;

    private String badgeCode;

    private String badgeName;

    private String description = "";
}

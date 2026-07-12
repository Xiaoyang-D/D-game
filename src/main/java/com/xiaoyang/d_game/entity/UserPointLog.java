package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_point_log")
public class UserPointLog extends BaseEntity {

    private Long userId;

    private String sourceType;

    private String sourceKey;

    private Integer pointsChange;

    private String targetType;

    private Long targetId;

    private String remark = "";
}

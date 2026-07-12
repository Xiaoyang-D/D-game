package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    private Long receiverId;

    private Long senderId;

    private Integer type;

    private String title;

    private String content = "";

    private Integer targetType;

    private Long targetId;

    private Integer isRead = 0;
}

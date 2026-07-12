package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_report")
public class ContentReport extends BaseEntity {
    private Long reporterId;
    private Integer targetType;
    private Long targetId;
    private String reason;
    /** 0 pending, 1 handled, 2 dismissed */
    private Integer status;
    private String handleNote;
    private Long handlerId;
}

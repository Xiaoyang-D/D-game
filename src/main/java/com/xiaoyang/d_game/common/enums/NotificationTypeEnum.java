package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum NotificationTypeEnum {

    LIKE(1, "点赞"),
    COMMENT(2, "评论"),
    FOLLOW(3, "关注"),
    SYSTEM(4, "系统通知"),
    AUDIT(5, "审核通知");

    private final int code;
    private final String desc;

    NotificationTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

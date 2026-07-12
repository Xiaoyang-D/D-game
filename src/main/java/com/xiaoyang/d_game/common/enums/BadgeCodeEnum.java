package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum BadgeCodeEnum {

    FIRST_CHECK_IN("FIRST_CHECK_IN", "初来乍到", "完成首次签到"),
    STREAK_7("STREAK_7", "七日坚持", "连续签到 7 天"),
    FIRST_POST("FIRST_POST", "首发作者", "发布第一篇帖子"),
    FIRST_RATING("FIRST_RATING", "评分先锋", "完成首次游戏评分");

    private final String code;
    private final String name;
    private final String description;

    BadgeCodeEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}

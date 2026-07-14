package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 用户徽章编码枚举。
 *
 * <p>徽章编码会存入 user_badge 表，用于前端展示用户成长成就。</p>
 */
public enum BadgeCodeEnum {

    /** 完成首次签到时获得。 */
    FIRST_CHECK_IN("FIRST_CHECK_IN", "初来乍到", "完成首次签到"),

    /** 连续签到 7 天时获得。 */
    STREAK_7("STREAK_7", "七日坚持", "连续签到 7 天"),

    /** 发布第一篇帖子时预留使用。 */
    FIRST_POST("FIRST_POST", "首发作者", "发布第一篇帖子"),

    /** 完成首次游戏评分时预留使用。 */
    FIRST_RATING("FIRST_RATING", "评分先锋", "完成首次游戏评分");

    /** 稳定编码，入库和接口返回都使用它。 */
    private final String code;

    /** 徽章展示名称。 */
    private final String name;

    /** 徽章达成条件说明。 */
    private final String description;

    /**
     * 枚举构造器。
     */
    BadgeCodeEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}

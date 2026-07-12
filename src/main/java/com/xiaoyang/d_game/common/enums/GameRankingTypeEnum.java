package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum GameRankingTypeEnum {

    RATING("rating", "评分榜"),
    POPULAR("popular", "人气榜");

    private final String code;
    private final String desc;

    GameRankingTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static GameRankingTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RATING;
        }
        for (GameRankingTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return RATING;
    }
}

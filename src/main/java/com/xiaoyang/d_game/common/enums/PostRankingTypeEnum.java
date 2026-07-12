package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum PostRankingTypeEnum {

    HOT("hot", "热度榜"),
    LATEST("latest", "最新榜");

    private final String code;
    private final String desc;

    PostRankingTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PostRankingTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return HOT;
        }
        for (PostRankingTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return HOT;
    }
}

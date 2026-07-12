package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum SearchTypeEnum {

    GAME("GAME", "游戏"),
    POST("POST", "帖子"),
    ALL("ALL", "全部");

    private final String code;
    private final String desc;

    SearchTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SearchTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ALL;
        }
        for (SearchTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return ALL;
    }
}

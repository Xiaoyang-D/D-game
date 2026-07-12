package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum TargetTypeEnum {

    POST(1, "帖子"),
    COMMENT(2, "评论"),
    GAME(3, "游戏");

    private final int code;
    private final String desc;

    TargetTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TargetTypeEnum of(int code) {
        for (TargetTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("无效的目标类型: " + code);
    }
}

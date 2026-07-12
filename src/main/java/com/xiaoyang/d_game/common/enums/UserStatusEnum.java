package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {

    NORMAL(1, "正常"),
    BANNED(0, "封禁");

    private final int code;
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum ContentStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝");

    private final int code;
    private final String desc;

    ContentStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

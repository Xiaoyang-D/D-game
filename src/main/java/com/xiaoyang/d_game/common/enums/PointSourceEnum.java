package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
public enum PointSourceEnum {

    CHECK_IN("CHECK_IN", "每日签到", 10);

    private final String code;
    private final String desc;
    private final int defaultPoints;

    PointSourceEnum(String code, String desc, int defaultPoints) {
        this.code = code;
        this.desc = desc;
        this.defaultPoints = defaultPoints;
    }
}

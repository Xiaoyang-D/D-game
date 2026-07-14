package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 积分来源枚举。
 *
 * <p>积分流水会保存来源编码和幂等键，避免同一业务重复发放奖励。</p>
 */
public enum PointSourceEnum {

    /** 每日签到积分来源。 */
    CHECK_IN("CHECK_IN", "每日签到", 10);

    /** 来源编码。 */
    private final String code;

    /** 来源中文说明。 */
    private final String desc;

    /** 该来源默认发放积分。 */
    private final int defaultPoints;

    PointSourceEnum(String code, String desc, int defaultPoints) {
        this.code = code;
        this.desc = desc;
        this.defaultPoints = defaultPoints;
    }
}

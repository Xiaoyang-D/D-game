package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 互动/举报/通知关联目标类型枚举。
 */
public enum TargetTypeEnum {

    /** 帖子。 */
    POST(1, "帖子"),

    /** 评论。 */
    COMMENT(2, "评论"),

    /** 游戏。 */
    GAME(3, "游戏");

    /** 数据库存储的目标类型码。 */
    private final int code;

    /** 中文说明。 */
    private final String desc;

    TargetTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据数字编码解析目标类型。
     *
     * @throws IllegalArgumentException 编码无效时抛出，调用方通常会转换成参数错误或业务错误
     */
    public static TargetTypeEnum of(int code) {
        for (TargetTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("无效的目标类型: " + code);
    }
}

package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 站内通知类型枚举。
 */
public enum NotificationTypeEnum {

    /** 点赞通知。 */
    LIKE(1, "点赞"),

    /** 评论通知。 */
    COMMENT(2, "评论"),

    /** 关注通知。 */
    FOLLOW(3, "关注"),

    /** 系统通知。 */
    SYSTEM(4, "系统通知"),

    /** 审核结果通知。 */
    AUDIT(5, "审核通知");

    /** 数据库存储的类型码。 */
    private final int code;

    /** 中文说明。 */
    private final String desc;

    NotificationTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 用户状态枚举。
 */
public enum UserStatusEnum {

    /** 正常用户，可以登录并访问需要登录的接口。 */
    NORMAL(1, "正常"),

    /** 被封禁用户，鉴权时会被拦截。 */
    BANNED(0, "封禁");

    /** 数据库存储的状态码。 */
    private final int code;

    /** 中文说明。 */
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

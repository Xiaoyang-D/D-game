package com.xiaoyang.d_game.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "系统异常"),

    USER_EXISTS(1001, "用户已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_BANNED(1004, "账号已被封禁"),
    TOKEN_INVALID(1005, "Token无效"),
    TOKEN_EXPIRED(1006, "Token已过期"),

    GAME_NOT_FOUND(2001, "游戏不存在"),
    POST_NOT_FOUND(3001, "帖子不存在"),
    BOARD_NOT_FOUND(3002, "版块不存在"),
    COMMENT_NOT_FOUND(4001, "评论不存在"),
    ALREADY_LIKED(4002, "已点赞"),
    NOT_LIKED(4003, "未点赞"),
    ALREADY_FAVORITED(4004, "已收藏"),
    NOT_FAVORITED(4005, "未收藏"),
    ALREADY_FOLLOWED(4006, "已关注"),
    NOT_FOLLOWED(4007, "未关注"),
    CANNOT_FOLLOW_SELF(4008, "不能关注自己"),
    ALREADY_CHECKED_IN(4009, "今日已签到"),

    FILE_UPLOAD_ERROR(5001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(5002, "文件类型不允许");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

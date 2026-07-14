package com.xiaoyang.d_game.common;

import lombok.Getter;

@Getter
/**
 * 统一业务状态码。
 *
 * <p>错误码按业务域分段：通用错误使用 HTTP 风格数字，用户/鉴权为 1000 段，
 * 游戏为 2000 段，社区内容和互动为 3000/4000 段，文件上传为 5000 段。
 * Controller 和 Service 应优先抛出这里定义的错误码，避免前端需要处理过多临时文案。</p>
 */
public enum ResultCode {

    /** 请求处理成功。 */
    SUCCESS(0, "success"),

    /** 通用请求参数错误。 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未登录、登录态缺失或登录态过期。 */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /** 已登录但缺少访问目标资源或操作的权限。 */
    FORBIDDEN(403, "无权限访问"),

    /** 请求的业务资源不存在。 */
    NOT_FOUND(404, "资源不存在"),

    /** 唯一键冲突、重复提交等资源状态冲突。 */
    CONFLICT(409, "资源冲突"),

    /** 未被业务异常捕获的系统内部错误。 */
    INTERNAL_ERROR(500, "系统异常"),

    /** 用户名、邮箱或手机号等唯一信息已存在。 */
    USER_EXISTS(1001, "用户已存在"),

    /** 用户记录不存在。 */
    USER_NOT_FOUND(1002, "用户不存在"),

    /** 登录密码不匹配；为安全起见不区分用户名不存在和密码错误。 */
    PASSWORD_ERROR(1003, "用户名或密码错误"),

    /** 用户账号已被管理员封禁。 */
    USER_BANNED(1004, "账号已被封禁"),

    /** JWT 结构、签名或类型不合法。 */
    TOKEN_INVALID(1005, "Token无效"),

    /** JWT 已超过有效期。 */
    TOKEN_EXPIRED(1006, "Token已过期"),

    /** 游戏不存在。 */
    GAME_NOT_FOUND(2001, "游戏不存在"),

    /** 帖子不存在。 */
    POST_NOT_FOUND(3001, "帖子不存在"),

    /** 社区版块不存在。 */
    BOARD_NOT_FOUND(3002, "版块不存在"),

    /** 评论不存在。 */
    COMMENT_NOT_FOUND(4001, "评论不存在"),

    /** 当前用户已经对目标点过赞。 */
    ALREADY_LIKED(4002, "已点赞"),

    /** 当前用户尚未对目标点赞，无法取消。 */
    NOT_LIKED(4003, "未点赞"),

    /** 当前用户已经收藏过目标。 */
    ALREADY_FAVORITED(4004, "已收藏"),

    /** 当前用户尚未收藏目标，无法取消。 */
    NOT_FAVORITED(4005, "未收藏"),

    /** 当前用户已经关注该用户。 */
    ALREADY_FOLLOWED(4006, "已关注"),

    /** 当前用户尚未关注该用户，无法取消。 */
    NOT_FOLLOWED(4007, "未关注"),

    /** 用户不能关注自己。 */
    CANNOT_FOLLOW_SELF(4008, "不能关注自己"),

    /** 当前自然日已完成签到。 */
    ALREADY_CHECKED_IN(4009, "今日已签到"),

    /** 文件上传过程失败。 */
    FILE_UPLOAD_ERROR(5001, "文件上传失败"),

    /** 文件 MIME 类型或扩展名不在允许范围。 */
    FILE_TYPE_NOT_ALLOWED(5002, "文件类型不允许");

    /** 对外返回的业务错误码。 */
    private final int code;

    /** 对外返回的默认提示文案。 */
    private final String message;

    /**
     * 枚举构造器，仅在枚举常量中使用。
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

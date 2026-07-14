package com.xiaoyang.d_game.common;

import lombok.Getter;

@Getter
/**
 * 业务异常。
 *
 * <p>用于表达“请求本身可预期地失败”，例如未登录、资源不存在、重复点赞等。
 * 全局异常处理器会把它转换成统一的 {@link Result}，HTTP 状态保持 200，业务状态通过 {@code code} 区分。</p>
 */
public class BizException extends RuntimeException {

    /**
     * 业务错误码，和 {@link ResultCode} 保持一致。
     */
    private final int code;

    /**
     * 使用标准错误码和标准提示构造业务异常。
     */
    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用标准错误码但覆盖错误提示，适合补充具体资源 ID、字段名等上下文。
     */
    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 直接指定错误码和提示，适合少量不在枚举中的兼容场景。
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}

package com.xiaoyang.d_game.common;

import lombok.Data;

import java.io.Serializable;

@Data
/**
 * 后端统一响应体。
 *
 * <p>所有 REST 接口都通过 {@code Result} 包一层业务状态，前端可以稳定读取 {@code code/message/data}。
 * HTTP 状态主要表达协议层错误，业务成功或失败由 {@code code} 区分。</p>
 *
 * @param <T> data 字段的数据类型
 */
public class Result<T> implements Serializable {

    /** 业务状态码，0 表示成功，非 0 表示失败。 */
    private int code;

    /** 给前端或用户展示的提示文案。 */
    private String message;

    /** 实际业务数据，列表/详情/令牌等都放在这里。 */
    private T data;

    /** 请求链路 ID，预留给日志追踪或网关注入。 */
    private String requestId;

    /**
     * 构造成功响应并携带业务数据。
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 构造无数据成功响应，适合删除、更新、退出登录等只需要表达成功的接口。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 使用标准错误码构造失败响应。
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    /**
     * 使用自定义错误码和提示构造失败响应。
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}

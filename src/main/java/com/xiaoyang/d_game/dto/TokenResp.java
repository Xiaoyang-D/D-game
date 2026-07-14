package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 登录令牌响应。
 */
public class TokenResp {

    /** 访问令牌，用于调用普通受保护接口。 */
    private String accessToken;

    /** 刷新令牌，用于换取新的访问令牌。 */
    private String refreshToken;

    /** access token 剩余有效期，单位秒。 */
    private Long expiresIn;

    /** 当前登录用户信息。 */
    private UserResp user;
}

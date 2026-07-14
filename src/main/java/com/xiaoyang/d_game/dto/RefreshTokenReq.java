package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * 刷新 token 请求。
 *
 * <p>浏览器 Cookie 模式下请求体可以为空，由 Controller 从 refresh_token Cookie 中补齐。</p>
 */
public class RefreshTokenReq {

    /** 刷新令牌。 */
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}

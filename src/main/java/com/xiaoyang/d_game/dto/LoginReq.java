package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * 登录请求。
 */
public class LoginReq {

    /** 用户名。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 明文密码，仅用于本次登录校验，后端不会保存。 */
    @NotBlank(message = "密码不能为空")
    private String password;
}

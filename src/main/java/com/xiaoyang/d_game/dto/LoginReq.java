package com.xiaoyang.d_game.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.util.Locale;
/** 邮箱密码登录请求。 */
@Data
public class LoginReq {
    @NotBlank @Email @Size(max = 128)
    private String email;
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
    @NotBlank @ToString.Exclude
    private String password;
}

package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 用户注册请求。
 */
public class RegisterReq {

    /** 用户名，长度 3-64，系统内唯一。 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度需在3-64之间")
    private String username;

    /** 明文密码，后端会使用 BCrypt 哈希后保存。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    private String password;

    /** 昵称，可为空；为空时默认使用用户名。 */
    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;

    /** 邮箱，可为空；填写时必须符合邮箱格式且唯一。 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号，可为空；填写时要求唯一。 */
    private String mobile;
}

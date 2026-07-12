package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileReq {

    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String mobile;

    private String avatarUrl;

    @Size(max = 500, message = "简介长度不能超过500")
    private String bio;
}

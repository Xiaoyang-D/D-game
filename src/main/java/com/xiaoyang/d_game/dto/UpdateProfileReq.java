package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 更新个人资料请求。
 *
 * <p>字段为空时表示不更新该字段；头像和简介允许传空字符串清空。</p>
 */
public class UpdateProfileReq {

    /** 昵称，最多 64 字。 */
    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;

    /** 邮箱，填写时校验格式和唯一性。 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号，填写时校验唯一性。 */
    private String mobile;

    /** 头像 URL。 */
    private String avatarUrl;

    /** 个人简介，最多 500 字。 */
    @Size(max = 500, message = "简介长度不能超过500")
    private String bio;
}

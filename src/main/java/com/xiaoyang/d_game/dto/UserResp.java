package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
/**
 * 用户资料响应。
 *
 * <p>不包含密码哈希等敏感字段。</p>
 */
public class UserResp {

    /** 用户 ID。 */
    private Long id;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 邮箱。 */
    private String email;

    private LocalDateTime emailVerifiedAt;

    /** 手机号。 */
    private String mobile;

    /** 头像 URL。 */
    private String avatarUrl;

    /** 个人简介。 */
    private String bio;

    /** 用户状态：0 封禁，1 正常。 */
    private Integer status;

    /** 用户拥有的角色编码列表。 */
    private List<String> roles;

    /** 注册时间。 */
    private LocalDateTime gmtCreate;
}

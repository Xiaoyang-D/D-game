package com.xiaoyang.d_game.entity;

import com.xiaoyang.d_game.common.BaseEntity;
import com.xiaoyang.d_game.common.enums.UserStatusEnum;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`user`")
/**
 * 用户实体。
 *
 * <p>对应数据库中的 user 表。由于 user 是 MySQL 关键字，表名使用反引号声明。</p>
 */
public class User extends BaseEntity {

    /** 登录用户名，唯一。 */
    private String username;

    /** BCrypt 密码哈希，不对外返回。 */
    private String passwordHash;

    /** 用户昵称，用于前端展示。 */
    private String nickname = "";

    /** 邮箱，可为空但填写后唯一。 */
    private String email;

    /** 邮箱归属验证时间；历史邮箱默认未验证。 */
    private java.time.LocalDateTime emailVerifiedAt;

    /** 认证版本，密码重置和迁移后递增。 */
    private Integer authVersion;

    /** 手机号，可为空但填写后唯一。 */
    private String mobile;

    /** 头像 URL。 */
    private String avatarUrl;

    /** 个人简介。 */
    private String bio = "";

    /** 用户状态，取值见 {@link UserStatusEnum}。 */
    private Integer status = UserStatusEnum.NORMAL.getCode();
}


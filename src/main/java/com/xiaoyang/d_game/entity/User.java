package com.xiaoyang.d_game.entity;

import com.xiaoyang.d_game.common.BaseEntity;
import com.xiaoyang.d_game.common.enums.UserStatusEnum;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`user`")
public class User extends BaseEntity {

    private String username;

    private String passwordHash;

    private String nickname = "";

    private String email;

    private String mobile;

    private String avatarUrl;

    private String bio = "";

    private Integer status = UserStatusEnum.NORMAL.getCode();
}


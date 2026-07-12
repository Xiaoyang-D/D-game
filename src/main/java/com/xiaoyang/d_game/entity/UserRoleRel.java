package com.xiaoyang.d_game.entity;

import com.xiaoyang.d_game.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_role_rel")
public class UserRoleRel extends BaseEntity {

    private Long userId;

    private Long roleId;
}


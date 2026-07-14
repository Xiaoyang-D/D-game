package com.xiaoyang.d_game.entity;

import com.xiaoyang.d_game.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_role_rel")
/**
 * 用户与角色关联实体。
 */
public class UserRoleRel extends BaseEntity {

    /** 用户 ID。 */
    private Long userId;

    /** 角色 ID。 */
    private Long roleId;
}


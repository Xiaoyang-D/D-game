package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role_permission_rel")
/**
 * 角色与权限关联实体。
 */
public class RolePermissionRel extends BaseEntity {

    /** 角色 ID。 */
    private Long roleId;

    /** 权限 ID。 */
    private Long permissionId;
}

package com.xiaoyang.d_game.entity;

import com.xiaoyang.d_game.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
/**
 * 系统角色实体。
 *
 * <p>角色编码会写入 JWT 并用于 {@code @RequireRole} 鉴权。</p>
 */
public class SysRole extends BaseEntity {

    /** 角色编码，例如 ADMIN、USER。 */
    private String roleCode;

    /** 角色名称，例如管理员、普通用户。 */
    private String roleName;

    /** 角色描述。 */
    private String description;
}


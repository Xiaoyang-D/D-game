package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
/**
 * 系统权限实体。
 *
 * <p>当前角色鉴权主要使用角色编码，权限表为后续更细粒度 RBAC 扩展预留。</p>
 */
public class SysPermission extends BaseEntity {

    /** 权限编码，例如 admin:user:manage。 */
    private String permCode;

    /** 权限名称。 */
    private String permName;

    /** 权限描述。 */
    private String description = "";
}

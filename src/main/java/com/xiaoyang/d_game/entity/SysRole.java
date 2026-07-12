package com.xiaoyang.d_game.entity;

import com.xiaoyang.d_game.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String description;
}


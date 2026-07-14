package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
/**
 * 游戏标签实体。
 */
public class Tag extends BaseEntity {

    /** 标签名称，例如多人联机、开放世界。 */
    private String name;
}

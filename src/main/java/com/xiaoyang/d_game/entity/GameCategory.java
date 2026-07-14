package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_category")
/**
 * 游戏分类实体。
 */
public class GameCategory extends BaseEntity {

    /** 分类名称，例如动作、角色扮演、策略。 */
    private String name;

    /** 展示排序，越小越靠前。 */
    private Integer sortOrder = 0;
}

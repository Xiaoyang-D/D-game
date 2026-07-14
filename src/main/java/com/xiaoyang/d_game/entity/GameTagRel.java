package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_tag_rel")
/**
 * 游戏与标签关联实体。
 *
 * <p>一个游戏可以绑定多个标签，一个标签也可关联多个游戏。</p>
 */
public class GameTagRel extends BaseEntity {

    /** 游戏 ID。 */
    private Long gameId;

    /** 标签 ID。 */
    private Long tagId;
}

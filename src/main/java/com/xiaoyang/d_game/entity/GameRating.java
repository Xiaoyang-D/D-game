package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_rating")
/**
 * 游戏评分/评测实体。
 *
 * <p>一名用户对一个游戏最多一条评分记录，重复评分会更新该记录。</p>
 */
public class GameRating extends BaseEntity {

    /** 评分用户 ID。 */
    private Long userId;

    /** 被评分游戏 ID。 */
    private Long gameId;

    /** 分数，范围通常为 1-10。 */
    private Integer score;

    /** 一句话短评。 */
    private String summary;

    /** 游戏优点描述。 */
    private String pros;

    /** 游戏缺点描述。 */
    private String cons;

    /** 游玩时长，单位小时。 */
    private Integer playtimeHours;
}

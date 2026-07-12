package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_rating")
public class GameRating extends BaseEntity {

    private Long userId;

    private Long gameId;

    private Integer score;

    private String summary;

    private String pros;

    private String cons;

    private Integer playtimeHours;
}

package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 游戏排行榜查询请求。
 */
public class GameRankingQueryReq {

    /** 排行榜类型：rating 表示评分榜，popular 表示人气榜。 */
    private String type;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}

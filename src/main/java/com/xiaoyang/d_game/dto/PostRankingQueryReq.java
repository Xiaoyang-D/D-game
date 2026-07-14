package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 帖子排行榜查询请求。
 */
public class PostRankingQueryReq {

    /** 排行榜类型：hot 热度榜，latest 最新榜。 */
    private String type;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}

package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 帖子分页查询请求。
 */
public class PostQueryReq {

    /** 版块 ID 筛选。 */
    private Long boardId;

    /** 关联游戏 ID 筛选。 */
    private Long gameId;

    /** 标题关键字。 */
    private String keyword;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}

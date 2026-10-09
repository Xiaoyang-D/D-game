package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 统一搜索请求。
 */
public class SearchQueryReq {

    /** 可选游戏版区；指定时仅搜索该版区的公开帖子。 */
    private Long gameId;
    /** 分区筛选，例如攻略。 */
    private Long boardId;
    /** 最热排序按点赞量降序，默认最新。 */
    private Boolean recommended = false;

    /** 搜索关键字。 */
    private String keyword;

    /** 搜索类型：GAME 游戏，POST 帖子，ALL 全部。 */
    private String type;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}

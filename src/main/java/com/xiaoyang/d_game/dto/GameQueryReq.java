package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 游戏分页查询请求。
 */
public class GameQueryReq {

    /** 分类 ID 筛选。 */
    private Long categoryId;

    /** 标签 ID 筛选。 */
    private Long tagId;

    /** 游戏名称关键字。 */
    private String keyword;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}

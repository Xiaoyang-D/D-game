package com.xiaoyang.d_game.dto;

import com.xiaoyang.d_game.common.PageResult;
import lombok.Data;

@Data
/**
 * 统一搜索响应。
 *
 * <p>根据搜索类型，games 或 posts 可能为空。</p>
 */
public class SearchResp {

    /** 游戏搜索结果分页。 */
    private PageResult<GameResp> games;

    /** 帖子搜索结果分页。 */
    private PageResult<PostResp> posts;
}

package com.xiaoyang.d_game.dto;

import com.xiaoyang.d_game.common.PageResult;
import lombok.Data;

@Data
public class SearchResp {

    private PageResult<GameResp> games;

    private PageResult<PostResp> posts;
}

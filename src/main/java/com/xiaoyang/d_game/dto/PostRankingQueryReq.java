package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class PostRankingQueryReq {

    /** hot / latest */
    private String type;

    private Long page = 1L;

    private Long size = 10L;
}

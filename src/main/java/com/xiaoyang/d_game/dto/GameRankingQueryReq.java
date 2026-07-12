package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class GameRankingQueryReq {

    /** rating / popular */
    private String type;

    private Long page = 1L;

    private Long size = 10L;
}

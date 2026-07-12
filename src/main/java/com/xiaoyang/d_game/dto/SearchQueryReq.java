package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class SearchQueryReq {

    private String keyword;

    /** GAME / POST / ALL */
    private String type;

    private Long page = 1L;

    private Long size = 10L;
}

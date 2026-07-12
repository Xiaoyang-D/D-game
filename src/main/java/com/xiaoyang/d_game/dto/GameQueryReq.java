package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class GameQueryReq {

    private Long categoryId;

    private Long tagId;

    private String keyword;

    private Long page = 1L;

    private Long size = 10L;
}

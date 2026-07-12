package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class PostQueryReq {

    private Long boardId;

    private Long gameId;

    private String keyword;

    private Long page = 1L;

    private Long size = 10L;
}

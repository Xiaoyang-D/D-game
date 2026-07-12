package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class BannedAuthorPostQueryReq {

    private Long authorId;

    private String keyword;

    private Long page = 1L;

    private Long size = 10L;
}

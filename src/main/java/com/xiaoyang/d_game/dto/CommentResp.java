package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResp {

    private Long id;

    private Long postId;

    private Long parentId;

    private Long userId;

    private String userNickname;

    private String content;

    private Integer likeCount;

    private LocalDateTime gmtCreate;
}

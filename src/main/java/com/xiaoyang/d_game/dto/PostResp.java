package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long boardId;

    private String boardName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long gameId;

    private String gameName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String authorNickname;

    private String authorUsername;

    private String authorAvatarUrl;

    /** 作者状态：0 封禁 / 1 正常 */
    private Integer authorStatus;

    private String title;

    private String content;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer favoriteCount;

    private LocalDateTime gmtCreate;
}

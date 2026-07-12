package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameReviewResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userNickname;

    private Integer score;

    private String summary;

    private String pros;

    private String cons;

    private Integer playtimeHours;

    private LocalDateTime gmtCreate;
}

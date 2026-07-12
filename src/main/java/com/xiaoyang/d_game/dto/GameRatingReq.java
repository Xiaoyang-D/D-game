package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GameRatingReq {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 10, message = "评分最高为10")
    private Integer score;

    @Size(max = 300, message = "短评不能超过300字")
    private String summary;

    @Size(max = 500, message = "优点不能超过500字")
    private String pros;

    @Size(max = 500, message = "缺点不能超过500字")
    private String cons;

    @Min(value = 0, message = "游玩时长不能为负数")
    @Max(value = 100000, message = "游玩时长过大")
    private Integer playtimeHours;
}

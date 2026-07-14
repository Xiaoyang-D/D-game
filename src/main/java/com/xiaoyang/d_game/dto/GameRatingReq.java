package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 游戏评分/评测请求。
 *
 * <p>同一用户对同一游戏重复提交会覆盖原评分。</p>
 */
public class GameRatingReq {

    /** 评分，范围 1-10。 */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 10, message = "评分最高为10")
    private Integer score;

    /** 一句话短评，最多 300 字。 */
    @Size(max = 300, message = "短评不能超过300字")
    private String summary;

    /** 优点说明，最多 500 字。 */
    @Size(max = 500, message = "优点不能超过500字")
    private String pros;

    /** 缺点说明，最多 500 字。 */
    @Size(max = 500, message = "缺点不能超过500字")
    private String cons;

    /** 游玩时长，单位小时。 */
    @Min(value = 0, message = "游玩时长不能为负数")
    @Max(value = 100000, message = "游玩时长过大")
    private Integer playtimeHours;
}

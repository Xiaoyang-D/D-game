package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 游戏评测响应。
 */
public class GameReviewResp {

    /** 评测记录 ID，序列化为字符串避免前端精度丢失。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 评测用户 ID，序列化为字符串避免前端精度丢失。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 评测用户昵称。 */
    private String userNickname;

    /** 评分，范围 1-10。 */
    private Integer score;

    /** 短评。 */
    private String summary;

    /** 优点。 */
    private String pros;

    /** 缺点。 */
    private String cons;

    /** 游玩时长，单位小时。 */
    private Integer playtimeHours;

    /** 评测创建时间。 */
    private LocalDateTime gmtCreate;
}

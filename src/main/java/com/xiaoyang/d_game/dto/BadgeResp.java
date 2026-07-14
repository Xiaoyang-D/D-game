package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 用户徽章响应。
 */
public class BadgeResp {

    /** 徽章编码。 */
    private String badgeCode;

    /** 徽章名称。 */
    private String badgeName;

    /** 徽章说明。 */
    private String description;

    /** 获得徽章的时间。 */
    private LocalDateTime obtainedAt;
}

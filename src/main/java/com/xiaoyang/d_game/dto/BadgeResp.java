package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BadgeResp {

    private String badgeCode;

    private String badgeName;

    private String description;

    private LocalDateTime obtainedAt;
}

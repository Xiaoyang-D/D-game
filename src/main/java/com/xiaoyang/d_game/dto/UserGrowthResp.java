package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserGrowthResp {

    private int totalPoints;

    private int streakDays;

    private boolean checkedInToday;

    private List<BadgeResp> badges = new ArrayList<>();
}

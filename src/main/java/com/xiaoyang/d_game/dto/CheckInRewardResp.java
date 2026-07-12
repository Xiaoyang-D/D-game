package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CheckInRewardResp {

    private int pointsEarned;

    private List<String> newBadges = new ArrayList<>();
}
